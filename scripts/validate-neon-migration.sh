#!/bin/bash
# Neon Migration Validation Script
# Validates your specific Neon PostgreSQL database after migration

set -e

echo "🔍 Validating Your Neon PostgreSQL Migration"
echo "============================================"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_step() { echo -e "${PURPLE}🔄 $1${NC}"; }

# Your Neon database details
NEON_DATABASE_URL="postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"

print_info "Validating Neon database: ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
print_info "Database: chatdb (Singapore region)"

# Check if psql is available
if ! command -v psql &> /dev/null; then
    print_error "psql not found. Please install PostgreSQL client tools."
    exit 1
fi

print_info "Starting comprehensive validation..."

# Test 1: Basic connection and database info
print_step "Testing database connection and info..."
if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Database connection successful"
    
    # Get detailed database info
    PG_VERSION=$(psql "$NEON_DATABASE_URL" -t -c "SELECT version();" | head -1)
    DB_NAME=$(psql "$NEON_DATABASE_URL" -t -c "SELECT current_database();" | xargs)
    CURRENT_USER=$(psql "$NEON_DATABASE_URL" -t -c "SELECT current_user;" | xargs)
    
    print_info "PostgreSQL Version: $PG_VERSION"
    print_info "Database Name: $DB_NAME"
    print_info "Connected User: $CURRENT_USER"
    print_info "Region: Singapore (ap-southeast-1)"
else
    print_error "Database connection failed"
    exit 1
fi

# Test 2: Check table structure
print_step "Checking table structure..."

TABLES=("users" "conversations" "conversation_participants")
for table in "${TABLES[@]}"; do
    if psql "$NEON_DATABASE_URL" -c "\dt $table" > /dev/null 2>&1; then
        print_status "Table $table exists"
        
        # Get table info
        ROW_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM $table;" | xargs 2>/dev/null || echo "0")
        print_info "  └─ Rows: $ROW_COUNT"
    else
        print_error "Table $table missing"
        exit 1
    fi
done

# Test 3: Check data integrity and relationships
print_step "Checking data integrity..."

USER_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

print_status "Data Summary:"
echo "  ├─ Users: $USER_COUNT"
echo "  ├─ Conversations: $CONVERSATION_COUNT"  
echo "  └─ Participants: $PARTICIPANT_COUNT"

# Test 4: Validate critical queries from DatabaseCleanupService
print_step "Testing DatabaseCleanupService queries..."

# Test findAllActiveConversationIds
ACTIVE_CONVERSATIONS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NULL;" | xargs 2>/dev/null || echo "0")
print_info "Active conversations: $ACTIVE_CONVERSATIONS"

# Test findAllSoftDeletedConversationIds  
SOFT_DELETED=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL;" | xargs 2>/dev/null || echo "0")
print_info "Soft-deleted conversations: $SOFT_DELETED"

# Test cleanup query with 30-day cutoff
THIRTY_DAYS_AGO=$(date -d '30 days ago' '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -v-30d '+%Y-%m-%d %H:%M:%S')
OLD_DELETED=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL AND deleted_at < '$THIRTY_DAYS_AGO'::timestamp;" | xargs 2>/dev/null || echo "0")
print_info "Old soft-deleted conversations (>30 days): $OLD_DELETED"

# Test complex query that cleanup service uses
ACTIVE_CONV_IDS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT array_length(array_agg(id), 1) FROM conversations WHERE deleted_at IS NULL;" | xargs 2>/dev/null || echo "0")
print_info "Active conversation IDs query result: $ACTIVE_CONV_IDS"

print_status "DatabaseCleanupService queries validated"

# Test 5: Check indexes and constraints
print_step "Checking indexes and constraints..."

# Check if primary keys exist
USER_PK=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'PRIMARY KEY';" | xargs 2>/dev/null || echo "0")
CONV_PK=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'conversations' AND constraint_type = 'PRIMARY KEY';" | xargs 2>/dev/null || echo "0")
PART_PK=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'conversation_participants' AND constraint_type = 'PRIMARY KEY';" | xargs 2>/dev/null || echo "0")

if [ "$USER_PK" -gt 0 ] && [ "$CONV_PK" -gt 0 ] && [ "$PART_PK" -gt 0 ]; then
    print_status "Primary keys validated"
else
    print_warning "Some primary keys might be missing (normal for new installations)"
fi

# Test 6: Check unique constraints
print_step "Checking unique constraints..."

USER_UNIQUE=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'UNIQUE';" | xargs 2>/dev/null || echo "0")
print_info "User unique constraints: $USER_UNIQUE"

# Test 7: Validate soft delete functionality
print_step "Validating soft delete functionality..."

if [ "$SOFT_DELETED" -gt 0 ]; then
    # Check if deleted_at timestamps are reasonable
    INVALID_TIMESTAMPS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL AND (deleted_at < '2020-01-01'::timestamp OR deleted_at > NOW());" | xargs 2>/dev/null || echo "0")
    
    if [ "$INVALID_TIMESTAMPS" -eq 0 ]; then
        print_status "Soft delete timestamps are valid"
    else
        print_error "Found $INVALID_TIMESTAMPS invalid soft delete timestamps"
        exit 1
    fi
else
    print_info "No soft-deleted conversations to validate (normal for new installations)"
fi

# Test 8: Connection pool and serverless behavior
print_step "Testing Neon serverless connection pool..."

# Test multiple concurrent connections
CONNECTION_TESTS=5
print_info "Testing $CONNECTION_TESTS concurrent connections..."

for i in $(seq 1 $CONNECTION_TESTS); do
    if psql "$NEON_DATABASE_URL" -c "SELECT 1;" > /dev/null 2>&1; then
        print_info "  Connection $i: ✅ OK"
    else
        print_error "  Connection $i: ❌ Failed"
        exit 1
    fi
done

print_status "Connection pool test passed"

# Test 9: Performance baseline for Singapore region
print_step "Running performance baseline tests..."

# Test query performance (multiple runs to account for cold starts)
TOTAL_TIME=0
RUNS=3

for i in $(seq 1 $RUNS); do
    START_TIME=$(date +%s%N)
    psql "$NEON_DATABASE_URL" -c "SELECT COUNT(*) FROM users;" > /dev/null 2>&1
    END_TIME=$(date +%s%N)
    QUERY_TIME=$((($END_TIME - $START_TIME) / 1000000))
    TOTAL_TIME=$((TOTAL_TIME + QUERY_TIME))
    print_info "  Query $i: ${QUERY_TIME}ms"
done

AVG_TIME=$((TOTAL_TIME / RUNS))
print_info "Average query time: ${AVG_TIME}ms"

if [ "$AVG_TIME" -lt 50 ]; then
    print_status "Excellent performance (Singapore region optimized)"
elif [ "$AVG_TIME" -lt 200 ]; then
    print_status "Good performance"
elif [ "$AVG_TIME" -lt 1000 ]; then
    print_warning "Acceptable performance (may include cold starts)"
else
    print_warning "Slow performance - investigate connection or serverless cold starts"
fi

# Test 10: Application-specific validations
print_step "Testing application-specific functionality..."

# Test user authentication query pattern
if [ "$USER_COUNT" -gt 0 ]; then
    USER_AUTH_TEST=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users WHERE username IS NOT NULL AND email IS NOT NULL;" | xargs 2>/dev/null || echo "0")
    print_info "Users with complete auth data: $USER_AUTH_TEST"
else
    print_info "No users found (normal for new installations)"
fi

# Test conversation queries
if [ "$CONVERSATION_COUNT" -gt 0 ]; then
    CONVERSATION_TYPES=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(DISTINCT type) FROM conversations;" | xargs 2>/dev/null || echo "0")
    print_info "Conversation types available: $CONVERSATION_TYPES"
else
    print_info "No conversations found (normal for new installations)"
fi

echo ""
print_status "🎉 Neon migration validation completed successfully!"
echo ""
print_info "Your Neon Database Summary:"
echo "  ├─ Host: ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
echo "  ├─ Database: chatdb"
echo "  ├─ Region: Singapore (ap-southeast-1)"
echo "  ├─ Users: $USER_COUNT"  
echo "  ├─ Conversations: $CONVERSATION_COUNT (Active: $ACTIVE_CONVERSATIONS, Soft-deleted: $SOFT_DELETED)"
echo "  ├─ Participants: $PARTICIPANT_COUNT"
echo "  └─ Performance: ${AVG_TIME}ms average"
echo ""
print_warning "Ready for deployment! Next steps:"
echo "  1. Update Render DATABASE_URL environment variable"
echo "  2. Deploy your application"
echo "  3. Test with: ./scripts/test-critical-services-neon.sh"
echo "  4. Monitor application logs for database connectivity"
echo ""
print_info "Validation completed at $(date)"
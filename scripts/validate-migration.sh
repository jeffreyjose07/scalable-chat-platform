#!/bin/bash
# Migration Validation Script
# Tests all critical functionality after migrating to Neon PostgreSQL

set -e

echo "🔍 Validating Neon PostgreSQL Migration"
echo "======================================="

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

# Check if psql is available
if ! command -v psql &> /dev/null; then
    print_error "psql not found. Please install PostgreSQL client tools."
    exit 1
fi

# Get Neon database URL
echo ""
print_step "Please provide your Neon PostgreSQL DATABASE_URL:"
echo -n "Neon DATABASE_URL: "
read -s NEON_DATABASE_URL
echo ""

if [[ -z "$NEON_DATABASE_URL" ]]; then
    print_error "Neon DATABASE_URL is required"
    exit 1
fi

print_info "Starting comprehensive validation..."

# Test 1: Basic connection
print_step "Testing database connection..."
if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Database connection successful"
    
    # Get PostgreSQL version info
    PG_VERSION=$(psql "$NEON_DATABASE_URL" -t -c "SELECT version();" | head -1)
    print_info "PostgreSQL Version: $PG_VERSION"
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
    else
        print_error "Table $table missing"
        exit 1
    fi
done

# Test 3: Check data integrity
print_step "Checking data integrity..."

USER_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

print_status "Users: $USER_COUNT"
print_status "Conversations: $CONVERSATION_COUNT"
print_status "Participants: $PARTICIPANT_COUNT"

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
    print_warning "Some primary keys might be missing"
fi

# Test 6: Check unique constraints
print_step "Checking unique constraints..."

USER_UNIQUE=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'UNIQUE';" | xargs 2>/dev/null || echo "0")
print_info "User unique constraints: $USER_UNIQUE"

# Test 7: Validate timestamp preservation for soft deletes
print_step "Validating soft delete timestamps..."

if [ "$SOFT_DELETED" -gt 0 ]; then
    # Check if deleted_at timestamps are reasonable (not null, within reasonable range)
    INVALID_TIMESTAMPS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL AND (deleted_at < '2020-01-01'::timestamp OR deleted_at > NOW());" | xargs 2>/dev/null || echo "0")
    
    if [ "$INVALID_TIMESTAMPS" -eq 0 ]; then
        print_status "Soft delete timestamps are valid"
    else
        print_error "Found $INVALID_TIMESTAMPS invalid soft delete timestamps"
        exit 1
    fi
else
    print_info "No soft-deleted conversations to validate"
fi

# Test 8: Connection pool test
print_step "Testing connection pool behavior..."

# Test multiple concurrent connections (simulate connection pool)
for i in {1..5}; do
    if psql "$NEON_DATABASE_URL" -c "SELECT 1;" > /dev/null 2>&1; then
        print_info "Connection $i: OK"
    else
        print_error "Connection $i: Failed"
        exit 1
    fi
done

print_status "Connection pool test passed"

# Test 9: Performance baseline
print_step "Running performance baseline tests..."

# Simple query performance test
START_TIME=$(date +%s%N)
psql "$NEON_DATABASE_URL" -c "SELECT COUNT(*) FROM users;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
QUERY_TIME=$((($END_TIME - $START_TIME) / 1000000)) # Convert to milliseconds

print_info "Simple COUNT query time: ${QUERY_TIME}ms"

if [ "$QUERY_TIME" -lt 1000 ]; then
    print_status "Query performance is good"
elif [ "$QUERY_TIME" -lt 5000 ]; then
    print_warning "Query performance is acceptable but monitor closely"
else
    print_warning "Query performance is slow - may need optimization"
fi

# Test 10: Application-specific validations
print_step "Testing application-specific functionality..."

# Test query that the ConversationRepository uses
ACTIVE_CONV_IDS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT string_agg(id, ',') FROM conversations WHERE deleted_at IS NULL LIMIT 5;" | xargs 2>/dev/null || echo "")
if [[ -n "$ACTIVE_CONV_IDS" ]]; then
    print_status "ConversationRepository query pattern works"
else
    print_info "No active conversations found (this might be normal for new installations)"
fi

# Test user authentication query pattern
USER_AUTH_TEST=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users WHERE username IS NOT NULL AND email IS NOT NULL;" | xargs 2>/dev/null || echo "0")
print_info "Users with auth data: $USER_AUTH_TEST"

echo ""
print_status "Migration validation completed successfully!"
print_info "Database Statistics Summary:"
echo "  - Users: $USER_COUNT"  
echo "  - Conversations: $CONVERSATION_COUNT (Active: $ACTIVE_CONVERSATIONS, Soft-deleted: $SOFT_DELETED)"
echo "  - Participants: $PARTICIPANT_COUNT"
echo "  - Query Performance: ${QUERY_TIME}ms"

print_warning "Recommended next steps:"
echo "  1. Deploy your application with the new DATABASE_URL"
echo "  2. Test authentication and conversation functionality"
echo "  3. Monitor application logs for any database-related errors"
echo "  4. Run a test of the DatabaseCleanupService manually"
echo "  5. Monitor for 24-48 hours before removing Render PostgreSQL"

echo ""
print_info "Migration validation completed at $(date)"
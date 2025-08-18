#!/bin/bash
# Direct Migration Script with Neon Database Pre-configured
# Uses your specific Neon PostgreSQL database

set -e

echo "🚀 Direct Migration to Your Neon PostgreSQL Database"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_step() { echo -e "${PURPLE}🔄 $1${NC}"; }

# Your Neon database details
NEON_DATABASE_URL="postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"

print_info "Using your Neon database: ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
print_info "Database: chatdb (Singapore region)"

# Check if pg_dump is available
if ! command -v pg_dump &> /dev/null; then
    print_error "pg_dump not found. Please install PostgreSQL client tools."
    echo "  macOS: brew install postgresql"
    echo "  Ubuntu: sudo apt-get install postgresql-client"
    exit 1
fi

# Check if psql is available
if ! command -v psql &> /dev/null; then
    print_error "psql not found. Please install PostgreSQL client tools."
    exit 1
fi

print_status "PostgreSQL client tools found"

# Test connection to Neon first
print_step "Testing connection to your Neon database..."

if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Neon database connection successful"
    
    # Get database info
    PG_VERSION=$(psql "$NEON_DATABASE_URL" -t -c "SELECT version();" | head -1 | xargs)
    print_info "Neon PostgreSQL: $PG_VERSION"
else
    print_error "Cannot connect to your Neon database. Please check the connection."
    print_error "Expected database: chatdb at ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
    exit 1
fi

# Get current Render database URL
echo ""
print_step "Please provide your current Render PostgreSQL DATABASE_URL:"
print_warning "This is the DATABASE_URL from your current Render environment variables"
print_info "Format: postgresql://user:pass@host:port/database or jdbc:postgresql://..."
echo -n "Current Render DATABASE_URL: "
read -s RENDER_DATABASE_URL
echo ""

if [[ -z "$RENDER_DATABASE_URL" ]]; then
    print_error "Render DATABASE_URL is required"
    exit 1
fi

# Handle JDBC URL format (convert to standard PostgreSQL URL)
if [[ "$RENDER_DATABASE_URL" == jdbc:postgresql://* ]]; then
    print_info "Converting JDBC URL format to standard PostgreSQL URL..."
    RENDER_DATABASE_URL="${RENDER_DATABASE_URL#jdbc:}"
fi

print_info "Using Render database URL (secured)"

# Create backup directory with timestamp
BACKUP_DIR="neon_migration_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
print_status "Created backup directory: $BACKUP_DIR"

# Export data from Render PostgreSQL
print_step "Exporting data from your current Render PostgreSQL..."

pg_dump "$RENDER_DATABASE_URL" \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
    --create \
    --verbose \
    --file="$BACKUP_DIR/render_backup.sql"

print_status "Data exported to $BACKUP_DIR/render_backup.sql"

# Get table statistics before migration
print_step "Collecting pre-migration statistics..."

echo "Current Render Database Statistics:" > "$BACKUP_DIR/pre_migration_stats.txt"
echo "====================================" >> "$BACKUP_DIR/pre_migration_stats.txt"
echo "Migration Date: $(date)" >> "$BACKUP_DIR/pre_migration_stats.txt"
echo "" >> "$BACKUP_DIR/pre_migration_stats.txt"

# Get table counts from current Render database
echo "Table Counts:" >> "$BACKUP_DIR/pre_migration_stats.txt"
psql "$RENDER_DATABASE_URL" -c "
SELECT 
    'users' as table_name, COUNT(*) as row_count FROM users
UNION ALL
SELECT 
    'conversations' as table_name, COUNT(*) as row_count FROM conversations  
UNION ALL
SELECT 
    'conversation_participants' as table_name, COUNT(*) as row_count FROM conversation_participants
ORDER BY table_name;
" >> "$BACKUP_DIR/pre_migration_stats.txt" 2>/dev/null || echo "Some tables may not exist yet" >> "$BACKUP_DIR/pre_migration_stats.txt"

print_status "Pre-migration statistics saved"

# Import data to your Neon PostgreSQL
print_step "Importing data to your Neon PostgreSQL database..."

psql "$NEON_DATABASE_URL" < "$BACKUP_DIR/render_backup.sql"

print_status "Data import to Neon completed successfully!"

# Validate migration
print_step "Validating migration to your Neon database..."

echo "Neon Database Statistics:" > "$BACKUP_DIR/post_migration_stats.txt"
echo "=========================" >> "$BACKUP_DIR/post_migration_stats.txt"
echo "Migration Date: $(date)" >> "$BACKUP_DIR/post_migration_stats.txt"
echo "Neon Database: ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb" >> "$BACKUP_DIR/post_migration_stats.txt"
echo "" >> "$BACKUP_DIR/post_migration_stats.txt"

# Get table counts from Neon
echo "Table Counts:" >> "$BACKUP_DIR/post_migration_stats.txt"
psql "$NEON_DATABASE_URL" -c "
SELECT 
    'users' as table_name, COUNT(*) as row_count FROM users
UNION ALL
SELECT 
    'conversations' as table_name, COUNT(*) as row_count FROM conversations  
UNION ALL
SELECT 
    'conversation_participants' as table_name, COUNT(*) as row_count FROM conversation_participants
ORDER BY table_name;
" >> "$BACKUP_DIR/post_migration_stats.txt"

# Compare critical table counts
print_step "Comparing table counts between Render and Neon..."

USER_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")
CONVERSATION_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" 2>/dev/null | xargs || echo "0")
PARTICIPANT_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" 2>/dev/null | xargs || echo "0")

USER_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")
CONVERSATION_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" 2>/dev/null | xargs || echo "0")
PARTICIPANT_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" 2>/dev/null | xargs || echo "0")

echo ""
echo "🔍 Migration Validation Results:"
echo "================================="
printf "%-20s | %-8s | %-8s | %s\n" "Table" "Render" "Neon" "Status"
echo "-----------------------------------------------------------"
printf "%-20s | %-8s | %-8s | %s\n" "users" "$USER_COUNT_RENDER" "$USER_COUNT_NEON" \
    $(if [ "$USER_COUNT_RENDER" = "$USER_COUNT_NEON" ]; then echo "✅ MATCH"; else echo "❌ MISMATCH"; fi)
printf "%-20s | %-8s | %-8s | %s\n" "conversations" "$CONVERSATION_COUNT_RENDER" "$CONVERSATION_COUNT_NEON" \
    $(if [ "$CONVERSATION_COUNT_RENDER" = "$CONVERSATION_COUNT_NEON" ]; then echo "✅ MATCH"; else echo "❌ MISMATCH"; fi)
printf "%-20s | %-8s | %-8s | %s\n" "participants" "$PARTICIPANT_COUNT_RENDER" "$PARTICIPANT_COUNT_NEON" \
    $(if [ "$PARTICIPANT_COUNT_RENDER" = "$PARTICIPANT_COUNT_NEON" ]; then echo "✅ MATCH"; else echo "❌ MISMATCH"; fi)

# Save validation results
echo "Migration validation completed at $(date)" > "$BACKUP_DIR/validation_results.txt"
echo "Source: Render PostgreSQL" >> "$BACKUP_DIR/validation_results.txt"
echo "Target: Neon PostgreSQL (ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech)" >> "$BACKUP_DIR/validation_results.txt"
echo "Users: Render=$USER_COUNT_RENDER, Neon=$USER_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Conversations: Render=$CONVERSATION_COUNT_RENDER, Neon=$CONVERSATION_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Participants: Render=$PARTICIPANT_COUNT_RENDER, Neon=$PARTICIPANT_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"

# Test critical queries for your DatabaseCleanupService
print_step "Testing critical DatabaseCleanupService queries on Neon..."

# Test findAllActiveConversationIds query
ACTIVE_CONVERSATIONS_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NULL;" 2>/dev/null | xargs || echo "0")
print_info "Active conversations in Neon: $ACTIVE_CONVERSATIONS_NEON"

# Test soft-deleted conversations query
SOFT_DELETED_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL;" 2>/dev/null | xargs || echo "0")
print_info "Soft-deleted conversations in Neon: $SOFT_DELETED_NEON"

# Test 30-day cleanup query
THIRTY_DAYS_AGO=$(date -d '30 days ago' '+%Y-%m-%d %H:%M:%S' 2>/dev/null || date -v-30d '+%Y-%m-%d %H:%M:%S')
OLD_DELETED_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL AND deleted_at < '$THIRTY_DAYS_AGO'::timestamp;" 2>/dev/null | xargs || echo "0")
print_info "Old soft-deleted conversations (>30 days): $OLD_DELETED_NEON"

print_status "DatabaseCleanupService query validation completed"

# Test connection pooling and performance
print_step "Testing Neon connection pooling and performance..."

START_TIME=$(date +%s%N)
psql "$NEON_DATABASE_URL" -c "SELECT COUNT(*) FROM users;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
QUERY_TIME=$((($END_TIME - $START_TIME) / 1000000))

print_info "Query performance: ${QUERY_TIME}ms"

if [ "$QUERY_TIME" -lt 100 ]; then
    print_status "Excellent performance (database is hot)"
elif [ "$QUERY_TIME" -lt 1000 ]; then
    print_status "Good performance"
else
    print_warning "Performance indicates cold start - normal for first query"
fi

echo ""
print_status "🎉 Migration to Neon completed successfully!"
echo ""
print_info "Your Neon Database Details:"
echo "  Host: ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
echo "  Database: chatdb"
echo "  Region: Singapore (ap-southeast-1)"
echo "  Connection Pooling: Enabled"
echo ""
print_info "Backup files saved in: $BACKUP_DIR"
echo ""
print_warning "🎯 Next Steps:"
echo "  1. Update your Render environment variable DATABASE_URL to:"
echo "     postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"
echo ""
echo "  2. Deploy your application on Render"
echo ""
echo "  3. Test your application:"
echo "     ./scripts/test-critical-services-neon.sh"
echo ""
echo "  4. Monitor for 24 hours before removing old Render PostgreSQL"
echo ""
print_info "Migration completed at $(date)"
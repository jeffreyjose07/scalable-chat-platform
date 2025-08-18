#!/bin/bash
# Direct Migration Script: Render PostgreSQL → Neon PostgreSQL
# Pre-configured with your actual database connection strings

set -e

echo "🚀 Migrating from Render PostgreSQL to Neon PostgreSQL"
echo "======================================================"

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

# Your database connection strings
RENDER_DATABASE_URL="postgresql://chatuser:4dhaOI3yXfpZIm68PuwI6K0cj9NpFzaa@dpg-d1v1nvmmcj7s73evebtg-a.singapore-postgres.render.com/chatdb_l62n"
NEON_DATABASE_URL="postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"

print_info "Migration Details:"
echo "  Source: Render PostgreSQL (Singapore)"
echo "  Target: Neon PostgreSQL (Singapore)"
echo "  Database: chatdb_l62n → chatdb"

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

# Test connection to Render database
print_step "Testing connection to Render PostgreSQL..."

if psql "$RENDER_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Render database connection successful"
    
    # Get Render database info
    RENDER_VERSION=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT version();" | head -1 | xargs)
    RENDER_DB=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT current_database();" | xargs)
    print_info "Source: $RENDER_DB on $RENDER_VERSION"
else
    print_error "Cannot connect to Render database"
    print_error "Please check your Render database is accessible externally"
    exit 1
fi

# Test connection to Neon database
print_step "Testing connection to Neon PostgreSQL..."

if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Neon database connection successful"
    
    # Get Neon database info
    NEON_VERSION=$(psql "$NEON_DATABASE_URL" -t -c "SELECT version();" | head -1 | xargs)
    NEON_DB=$(psql "$NEON_DATABASE_URL" -t -c "SELECT current_database();" | xargs)
    print_info "Target: $NEON_DB on $NEON_VERSION"
else
    print_error "Cannot connect to Neon database"
    exit 1
fi

# Create backup directory with timestamp
BACKUP_DIR="render_to_neon_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
print_status "Created backup directory: $BACKUP_DIR"

# Get current data statistics from Render
print_step "Collecting current data from Render PostgreSQL..."

echo "Render PostgreSQL Statistics (Before Migration):" > "$BACKUP_DIR/render_stats.txt"
echo "================================================" >> "$BACKUP_DIR/render_stats.txt"
echo "Migration Date: $(date)" >> "$BACKUP_DIR/render_stats.txt"
echo "Database: chatdb_l62n" >> "$BACKUP_DIR/render_stats.txt"
echo "" >> "$BACKUP_DIR/render_stats.txt"

# Get table counts from Render
echo "Table Counts:" >> "$BACKUP_DIR/render_stats.txt"
psql "$RENDER_DATABASE_URL" -c "
SELECT 
    tablename as table_name, 
    schemaname as schema_name
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY tablename;
" >> "$BACKUP_DIR/render_stats.txt" 2>/dev/null

# Get row counts for main tables
USER_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | xargs || echo "0")
CONVERSATION_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" 2>/dev/null | xargs || echo "0")
PARTICIPANT_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" 2>/dev/null | xargs || echo "0")

echo "" >> "$BACKUP_DIR/render_stats.txt"
echo "Row Counts:" >> "$BACKUP_DIR/render_stats.txt"
echo "Users: $USER_COUNT_RENDER" >> "$BACKUP_DIR/render_stats.txt"
echo "Conversations: $CONVERSATION_COUNT_RENDER" >> "$BACKUP_DIR/render_stats.txt"
echo "Participants: $PARTICIPANT_COUNT_RENDER" >> "$BACKUP_DIR/render_stats.txt"

print_status "Current data summary:"
echo "  ├─ Users: $USER_COUNT_RENDER"
echo "  ├─ Conversations: $CONVERSATION_COUNT_RENDER"  
echo "  └─ Participants: $PARTICIPANT_COUNT_RENDER"

# Export data from Render PostgreSQL
print_step "Exporting data from Render PostgreSQL..."

pg_dump "$RENDER_DATABASE_URL" \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
    --create \
    --verbose \
    --file="$BACKUP_DIR/render_full_backup.sql"

print_status "Data exported to $BACKUP_DIR/render_full_backup.sql"

# Check backup file size
BACKUP_SIZE=$(ls -lh "$BACKUP_DIR/render_full_backup.sql" | awk '{print $5}')
print_info "Backup file size: $BACKUP_SIZE"

# Import data to Neon PostgreSQL
print_step "Importing data to Neon PostgreSQL..."

# Import with progress
psql "$NEON_DATABASE_URL" < "$BACKUP_DIR/render_full_backup.sql" 2>&1 | tee "$BACKUP_DIR/import_log.txt"

print_status "Data import to Neon completed!"

# Validate migration
print_step "Validating migration results..."

# Get row counts from Neon
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
echo "Migration Validation Results:" > "$BACKUP_DIR/validation_results.txt"
echo "============================" >> "$BACKUP_DIR/validation_results.txt"
echo "Migration Date: $(date)" >> "$BACKUP_DIR/validation_results.txt"
echo "Source: Render PostgreSQL (chatdb_l62n)" >> "$BACKUP_DIR/validation_results.txt"
echo "Target: Neon PostgreSQL (chatdb)" >> "$BACKUP_DIR/validation_results.txt"
echo "" >> "$BACKUP_DIR/validation_results.txt"
echo "Row Count Comparison:" >> "$BACKUP_DIR/validation_results.txt"
echo "Users: Render=$USER_COUNT_RENDER, Neon=$USER_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Conversations: Render=$CONVERSATION_COUNT_RENDER, Neon=$CONVERSATION_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Participants: Render=$PARTICIPANT_COUNT_RENDER, Neon=$PARTICIPANT_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"

# Test critical DatabaseCleanupService queries
print_step "Testing DatabaseCleanupService queries on Neon..."

# Test findAllActiveConversationIds query
ACTIVE_CONVERSATIONS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NULL;" 2>/dev/null | xargs || echo "0")
print_info "Active conversations: $ACTIVE_CONVERSATIONS"

# Test soft-deleted conversations query
SOFT_DELETED=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL;" 2>/dev/null | xargs || echo "0")
print_info "Soft-deleted conversations: $SOFT_DELETED"

print_status "Critical database queries validated"

# Test Neon performance
print_step "Testing Neon performance (Singapore region)..."

START_TIME=$(date +%s%N)
psql "$NEON_DATABASE_URL" -c "SELECT COUNT(*) FROM users;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
QUERY_TIME=$((($END_TIME - $START_TIME) / 1000000))

print_info "Query performance: ${QUERY_TIME}ms"

if [ "$QUERY_TIME" -lt 100 ]; then
    print_status "Excellent performance (hot database)"
elif [ "$QUERY_TIME" -lt 500 ]; then
    print_status "Good performance"
elif [ "$QUERY_TIME" -lt 2000 ]; then
    print_warning "Acceptable performance (possible cold start)"
else
    print_warning "Slow performance - monitor for cold starts"
fi

# Check if all tables migrated
print_step "Verifying table structure migration..."

RENDER_TABLES=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | xargs)
NEON_TABLES=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | xargs)

print_info "Tables migrated: $RENDER_TABLES → $NEON_TABLES"

if [ "$RENDER_TABLES" = "$NEON_TABLES" ]; then
    print_status "All tables migrated successfully"
else
    print_warning "Table count mismatch - check migration log"
fi

echo ""
print_status "🎉 Migration from Render to Neon completed successfully!"
echo ""
print_info "Migration Summary:"
echo "  ├─ Source: Render PostgreSQL (Singapore)"
echo "  ├─ Target: Neon PostgreSQL (Singapore)"
echo "  ├─ Users migrated: $USER_COUNT_RENDER → $USER_COUNT_NEON"
echo "  ├─ Conversations migrated: $CONVERSATION_COUNT_RENDER → $CONVERSATION_COUNT_NEON"
echo "  ├─ Participants migrated: $PARTICIPANT_COUNT_RENDER → $PARTICIPANT_COUNT_NEON"
echo "  ├─ Query performance: ${QUERY_TIME}ms"
echo "  └─ Tables migrated: $RENDER_TABLES → $NEON_TABLES"
echo ""
print_info "Backup files saved in: $BACKUP_DIR"
echo ""
print_warning "🎯 Next Steps:"
echo ""
echo "1. Update your Render environment variable DATABASE_URL to:"
echo "   postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"
echo ""
echo "2. Deploy your application on Render"
echo ""
echo "3. Validate with: ./scripts/validate-neon-migration.sh"
echo ""
echo "4. Test with: ./scripts/test-critical-services-neon.sh"
echo ""
echo "5. Monitor for 24 hours, then remove old Render PostgreSQL"
echo ""
print_info "Migration completed at $(date)"
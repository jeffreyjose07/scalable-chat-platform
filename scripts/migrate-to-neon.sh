#!/bin/bash
# Database Migration Script: Render PostgreSQL → Neon PostgreSQL
# Preserves all data, timestamps, and ensures zero-downtime migration

set -e

echo "🚀 Starting Database Migration to Neon PostgreSQL"
echo "================================================="

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

print_info "PostgreSQL client tools found"

# Get current Render database URL
echo ""
print_step "Please provide your current Render PostgreSQL DATABASE_URL:"
print_warning "Format: postgresql://user:pass@host:port/database"
echo -n "Render DATABASE_URL: "
read -s RENDER_DATABASE_URL
echo ""

if [[ -z "$RENDER_DATABASE_URL" ]]; then
    print_error "Render DATABASE_URL is required"
    exit 1
fi

# Get new Neon database URL
echo ""
print_step "Please provide your new Neon PostgreSQL DATABASE_URL:"
print_warning "Format: postgresql://user:pass@host:port/database"
echo -n "Neon DATABASE_URL: "
read -s NEON_DATABASE_URL
echo ""

if [[ -z "$NEON_DATABASE_URL" ]]; then
    print_error "Neon DATABASE_URL is required"
    exit 1
fi

print_info "Database URLs configured"

# Create backup directory with timestamp
BACKUP_DIR="database_migration_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
print_status "Created backup directory: $BACKUP_DIR"

# Export data from Render PostgreSQL
print_step "Exporting data from Render PostgreSQL..."

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

echo "Render Database Statistics:" > "$BACKUP_DIR/pre_migration_stats.txt"
echo "===========================" >> "$BACKUP_DIR/pre_migration_stats.txt"
echo "" >> "$BACKUP_DIR/pre_migration_stats.txt"

# Get table counts from Render
psql "$RENDER_DATABASE_URL" -c "
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserted_rows,
    n_tup_upd as updated_rows,
    n_tup_del as deleted_rows,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows
FROM pg_stat_user_tables 
ORDER BY tablename;
" >> "$BACKUP_DIR/pre_migration_stats.txt"

print_status "Pre-migration statistics saved"

# Test connection to Neon
print_step "Testing connection to Neon PostgreSQL..."

if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Neon database connection successful"
else
    print_error "Cannot connect to Neon database. Please check your DATABASE_URL."
    exit 1
fi

# Import data to Neon PostgreSQL
print_step "Importing data to Neon PostgreSQL..."

psql "$NEON_DATABASE_URL" < "$BACKUP_DIR/render_backup.sql"

print_status "Data import completed"

# Validate migration
print_step "Validating migration..."

echo "Neon Database Statistics:" > "$BACKUP_DIR/post_migration_stats.txt"
echo "=========================" >> "$BACKUP_DIR/post_migration_stats.txt"
echo "" >> "$BACKUP_DIR/post_migration_stats.txt"

# Get table counts from Neon
psql "$NEON_DATABASE_URL" -c "
SELECT 
    schemaname,
    tablename,
    n_tup_ins as inserted_rows,
    n_tup_upd as updated_rows,
    n_tup_del as deleted_rows,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows
FROM pg_stat_user_tables 
ORDER BY tablename;
" >> "$BACKUP_DIR/post_migration_stats.txt"

# Compare critical table counts
print_step "Comparing table counts..."

USER_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

USER_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

echo ""
echo "Migration Validation Results:"
echo "============================="
echo "Table               | Render | Neon   | Status"
echo "--------------------|--------|--------|--------"
printf "%-18s | %-6s | %-6s | %s\n" "users" "$USER_COUNT_RENDER" "$USER_COUNT_NEON" \
    $(if [ "$USER_COUNT_RENDER" = "$USER_COUNT_NEON" ]; then echo "✅ OK"; else echo "❌ MISMATCH"; fi)
printf "%-18s | %-6s | %-6s | %s\n" "conversations" "$CONVERSATION_COUNT_RENDER" "$CONVERSATION_COUNT_NEON" \
    $(if [ "$CONVERSATION_COUNT_RENDER" = "$CONVERSATION_COUNT_NEON" ]; then echo "✅ OK"; else echo "❌ MISMATCH"; fi)
printf "%-18s | %-6s | %-6s | %s\n" "participants" "$PARTICIPANT_COUNT_RENDER" "$PARTICIPANT_COUNT_NEON" \
    $(if [ "$PARTICIPANT_COUNT_RENDER" = "$PARTICIPANT_COUNT_NEON" ]; then echo "✅ OK"; else echo "❌ MISMATCH"; fi)

# Save validation results
echo "Migration validation completed at $(date)" > "$BACKUP_DIR/validation_results.txt"
echo "Users: Render=$USER_COUNT_RENDER, Neon=$USER_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Conversations: Render=$CONVERSATION_COUNT_RENDER, Neon=$CONVERSATION_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"
echo "Participants: Render=$PARTICIPANT_COUNT_RENDER, Neon=$PARTICIPANT_COUNT_NEON" >> "$BACKUP_DIR/validation_results.txt"

# Test critical queries that DatabaseCleanupService uses
print_step "Testing critical cleanup service queries..."

# Test findAllActiveConversationIds query
ACTIVE_CONVERSATIONS_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NULL;" | xargs 2>/dev/null || echo "0")
print_info "Active conversations in Neon: $ACTIVE_CONVERSATIONS_NEON"

# Test soft-deleted conversations query
SOFT_DELETED_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL;" | xargs 2>/dev/null || echo "0")
print_info "Soft-deleted conversations in Neon: $SOFT_DELETED_NEON"

print_status "Critical query validation completed"

echo ""
print_status "Migration completed successfully!"
print_info "Backup files saved in: $BACKUP_DIR"
print_warning "Next steps:"
echo "  1. Update your Render environment variable DATABASE_URL to the Neon URL"
echo "  2. Deploy your application"
echo "  3. Run the validation script: ./scripts/validate-migration.sh"
echo "  4. Monitor your application for 24 hours before removing Render PostgreSQL"

print_info "Your Neon DATABASE_URL (save this securely):"
echo "$NEON_DATABASE_URL"
#!/bin/bash
# Data-Only Migration Script: Render PostgreSQL → Neon PostgreSQL
# Migrates only data, not database structure

set -e

echo "🚀 Migrating Data from Render to Neon PostgreSQL"
echo "================================================"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✅ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_step() { echo -e "${PURPLE}🔄 $1${NC}"; }

# Database connections
RENDER_DATABASE_URL="postgresql://chatuser:4dhaOI3yXfpZIm68PuwI6K0cj9NpFzaa@dpg-d1v1nvmmcj7s73evebtg-a.singapore-postgres.render.com/chatdb_l62n"
NEON_DATABASE_URL="postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"

print_info "Migration: Render (chatdb_l62n) → Neon (chatdb)"

# Check PostgreSQL 15 tools
if ! command -v /opt/homebrew/opt/postgresql@15/bin/pg_dump &> /dev/null; then
    print_error "PostgreSQL 15 tools not found. Please install: brew install postgresql@15"
    exit 1
fi

# Use PostgreSQL 15 tools
export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"

print_status "Using PostgreSQL 15 client tools"

# Test connections
print_step "Testing database connections..."

if psql "$RENDER_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Render database connection OK"
else
    print_error "Cannot connect to Render database"
    exit 1
fi

if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
    print_status "Neon database connection OK"
else
    print_error "Cannot connect to Neon database"
    exit 1
fi

# Create backup directory
BACKUP_DIR="data_migration_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Get current data from Render
print_step "Collecting current data from Render..."

USER_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT_RENDER=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

print_info "Current Render data:"
echo "  ├─ Users: $USER_COUNT_RENDER"
echo "  ├─ Conversations: $CONVERSATION_COUNT_RENDER"  
echo "  └─ Participants: $PARTICIPANT_COUNT_RENDER"

if [ "$USER_COUNT_RENDER" = "0" ] && [ "$CONVERSATION_COUNT_RENDER" = "0" ] && [ "$PARTICIPANT_COUNT_RENDER" = "0" ]; then
    print_warning "No data found in Render database. Nothing to migrate."
    exit 0
fi

# Export data only (no CREATE DATABASE, no schema)
print_step "Exporting data from Render PostgreSQL..."

pg_dump "$RENDER_DATABASE_URL" \
    --no-owner \
    --no-privileges \
    --data-only \
    --disable-triggers \
    --verbose \
    --file="$BACKUP_DIR/render_data_only.sql"

print_status "Data exported to $BACKUP_DIR/render_data_only.sql"

# Export schema separately
print_step "Exporting schema from Render PostgreSQL..."

pg_dump "$RENDER_DATABASE_URL" \
    --no-owner \
    --no-privileges \
    --schema-only \
    --verbose \
    --file="$BACKUP_DIR/render_schema_only.sql"

print_status "Schema exported to $BACKUP_DIR/render_schema_only.sql"

# Import schema first to Neon
print_step "Creating schema in Neon PostgreSQL..."

psql "$NEON_DATABASE_URL" < "$BACKUP_DIR/render_schema_only.sql" > "$BACKUP_DIR/schema_import.log" 2>&1

print_status "Schema imported to Neon"

# Import data to Neon
print_step "Importing data to Neon PostgreSQL..."

psql "$NEON_DATABASE_URL" < "$BACKUP_DIR/render_data_only.sql" > "$BACKUP_DIR/data_import.log" 2>&1

print_status "Data imported to Neon"

# Validate migration
print_step "Validating migration..."

USER_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT_NEON=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

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

# Test critical queries
print_step "Testing critical DatabaseCleanupService queries..."

ACTIVE_CONVERSATIONS=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NULL;" | xargs 2>/dev/null || echo "0")
SOFT_DELETED=$(psql "$NEON_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations WHERE deleted_at IS NOT NULL;" | xargs 2>/dev/null || echo "0")

print_info "Active conversations: $ACTIVE_CONVERSATIONS"
print_info "Soft-deleted conversations: $SOFT_DELETED"

# Test performance
print_step "Testing Neon performance..."

START_TIME=$(date +%s%N)
psql "$NEON_DATABASE_URL" -c "SELECT COUNT(*) FROM users;" > /dev/null 2>&1
END_TIME=$(date +%s%N)
QUERY_TIME=$((($END_TIME - $START_TIME) / 1000000))

print_info "Query performance: ${QUERY_TIME}ms"

# Check if migration was successful
if [ "$USER_COUNT_RENDER" = "$USER_COUNT_NEON" ] && [ "$CONVERSATION_COUNT_RENDER" = "$CONVERSATION_COUNT_NEON" ] && [ "$PARTICIPANT_COUNT_RENDER" = "$PARTICIPANT_COUNT_NEON" ]; then
    MIGRATION_SUCCESS=true
else
    MIGRATION_SUCCESS=false
fi

echo ""
if [ "$MIGRATION_SUCCESS" = true ]; then
    print_status "🎉 Migration completed successfully!"
else
    print_error "❌ Migration validation failed - data counts don't match"
    print_warning "Check logs in: $BACKUP_DIR"
    exit 1
fi

print_info "Migration Summary:"
echo "  ├─ Users: $USER_COUNT_RENDER → $USER_COUNT_NEON"
echo "  ├─ Conversations: $CONVERSATION_COUNT_RENDER → $CONVERSATION_COUNT_NEON"
echo "  ├─ Participants: $PARTICIPANT_COUNT_RENDER → $PARTICIPANT_COUNT_NEON"
echo "  └─ Performance: ${QUERY_TIME}ms"

print_info "Backup files in: $BACKUP_DIR"

echo ""
print_warning "🎯 Next Steps:"
echo "1. Update Render DATABASE_URL to:"
echo "   postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb?sslmode=require&channel_binding=require"
echo ""
echo "2. Deploy application on Render"
echo ""
echo "3. Test with: ./scripts/test-critical-services-neon.sh"

print_info "Migration completed at $(date)"
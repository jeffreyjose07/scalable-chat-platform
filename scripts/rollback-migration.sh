#!/bin/bash
# Rollback Script for Neon Migration
# Restores from backup if migration issues occur

set -e

echo "🔙 Database Migration Rollback Script"
echo "===================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }

print_warning "CAUTION: This will rollback your database to the pre-migration state"
print_warning "Make sure you have a valid backup before proceeding"

echo ""
echo "Available backup directories:"
ls -la | grep "database_migration_" 2>/dev/null || echo "No backup directories found"

echo ""
echo -n "Enter the backup directory name (e.g., database_migration_20241218_143022): "
read BACKUP_DIR

if [[ ! -d "$BACKUP_DIR" ]]; then
    print_error "Backup directory $BACKUP_DIR not found"
    exit 1
fi

if [[ ! -f "$BACKUP_DIR/render_backup.sql" ]]; then
    print_error "Backup file $BACKUP_DIR/render_backup.sql not found"
    exit 1
fi

print_info "Found backup file: $BACKUP_DIR/render_backup.sql"

echo ""
echo -n "Enter your current Render PostgreSQL DATABASE_URL: "
read -s RENDER_DATABASE_URL
echo ""

if [[ -z "$RENDER_DATABASE_URL" ]]; then
    print_error "Render DATABASE_URL is required"
    exit 1
fi

echo ""
print_warning "This will restore your Render database to the backup state"
echo -n "Are you sure you want to proceed? (yes/no): "
read CONFIRM

if [[ "$CONFIRM" != "yes" ]]; then
    print_info "Rollback cancelled"
    exit 0
fi

print_info "Starting rollback process..."

# Test connection
if ! psql "$RENDER_DATABASE_URL" -c "SELECT 1;" > /dev/null 2>&1; then
    print_error "Cannot connect to Render database"
    exit 1
fi

print_status "Connection to Render database confirmed"

# Create rollback backup (backup current state before rollback)
ROLLBACK_BACKUP="rollback_backup_$(date +%Y%m%d_%H%M%S).sql"
print_info "Creating current state backup: $ROLLBACK_BACKUP"

pg_dump "$RENDER_DATABASE_URL" \
    --no-owner \
    --no-privileges \
    --clean \
    --if-exists \
    --create \
    --verbose \
    --file="$ROLLBACK_BACKUP"

print_status "Current state backed up to $ROLLBACK_BACKUP"

# Restore from backup
print_info "Restoring from backup: $BACKUP_DIR/render_backup.sql"

psql "$RENDER_DATABASE_URL" < "$BACKUP_DIR/render_backup.sql"

print_status "Database restored from backup"

# Validate rollback
print_info "Validating rollback..."

USER_COUNT=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM users;" | xargs 2>/dev/null || echo "0")
CONVERSATION_COUNT=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversations;" | xargs 2>/dev/null || echo "0")
PARTICIPANT_COUNT=$(psql "$RENDER_DATABASE_URL" -t -c "SELECT COUNT(*) FROM conversation_participants;" | xargs 2>/dev/null || echo "0")

print_status "Rollback validation:"
echo "  - Users: $USER_COUNT"
echo "  - Conversations: $CONVERSATION_COUNT"
echo "  - Participants: $PARTICIPANT_COUNT"

print_status "Rollback completed successfully!"
print_warning "Next steps:"
echo "  1. Update your Render environment variable DATABASE_URL back to Render PostgreSQL"
echo "  2. Deploy your application"
echo "  3. Investigate the issues that caused the rollback"
echo "  4. Current state backup saved as: $ROLLBACK_BACKUP"

print_info "Rollback completed at $(date)"
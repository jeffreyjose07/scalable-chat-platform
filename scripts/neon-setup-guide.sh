#!/bin/bash
# Neon PostgreSQL Setup Guide
# Step-by-step guide to create Neon account and database

echo "🌟 Neon PostgreSQL Setup Guide"
echo "=============================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

print_step() { echo -e "${PURPLE}📋 STEP $1: $2${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_action() { echo -e "${CYAN}👉 $1${NC}"; }
print_important() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }

echo "This guide will help you:"
echo "1. Create a free Neon account"
echo "2. Set up your PostgreSQL database"
echo "3. Get your connection string"
echo "4. Prepare for migration"
echo ""

print_step "1" "Create Neon Account"
echo ""
print_action "Go to: https://neon.tech"
print_action "Click 'Sign up' (top right)"
print_action "Choose one of these options:"
echo "   • Sign up with GitHub (recommended for developers)"
echo "   • Sign up with Google"
echo "   • Sign up with email"
echo ""
print_info "Why Neon is perfect for your chat platform:"
echo "  ✓ 3GB storage (vs Render's 1GB)"
echo "  ✓ Unlimited databases"
echo "  ✓ Built-in connection pooling"
echo "  ✓ No time limits (vs Render's 90-day expiration)"
echo "  ✓ Better performance with serverless scaling"
echo ""
read -p "Press Enter when you've created your account..."

print_step "2" "Create Your Database Project"
echo ""
print_action "After signing up, you'll see the Neon dashboard"
print_action "Click 'Create Project' (if not done automatically)"
print_action "Configure your project:"
echo ""
echo "   Project Name: chat-platform"
echo "   Database Name: chatdb"
echo "   Region: Choose closest to your users (US East, EU, etc.)"
echo "   PostgreSQL Version: 15 (recommended)"
echo ""
print_info "Leave other settings as default - they're optimized for most applications"
echo ""
read -p "Press Enter when your project is created..."

print_step "3" "Get Your Connection String"
echo ""
print_action "In your Neon dashboard:"
print_action "1. Click on your 'chat-platform' project"
print_action "2. Go to 'Connection Details' or 'Connect' tab"
print_action "3. Make sure 'Pooled connection' is selected (recommended)"
print_action "4. Copy the connection string"
echo ""
print_important "Your connection string will look like:"
echo "postgresql://username:password@ep-xyz-pooler.region.neon.tech/chatdb?sslmode=require"
echo ""
print_important "Keep this connection string secure - you'll need it for migration!"
echo ""
read -p "Press Enter when you have your connection string..."

print_step "4" "Verify Database Access"
echo ""
print_info "Let's test your connection before migration"
echo ""
echo -n "Please paste your Neon DATABASE_URL here: "
read -s NEON_DATABASE_URL
echo ""

if [[ -z "$NEON_DATABASE_URL" ]]; then
    echo "❌ No URL provided. Please run this script again with your connection string."
    exit 1
fi

print_info "Testing connection to Neon..."

# Test connection
if command -v psql &> /dev/null; then
    if psql "$NEON_DATABASE_URL" -c "SELECT version();" > /dev/null 2>&1; then
        print_success "Connection to Neon successful!"
        
        # Get database info
        PG_VERSION=$(psql "$NEON_DATABASE_URL" -t -c "SELECT version();" | head -1 | xargs)
        DB_NAME=$(psql "$NEON_DATABASE_URL" -t -c "SELECT current_database();" | xargs)
        
        print_info "Database Details:"
        echo "  Database: $DB_NAME"
        echo "  Version: $PG_VERSION"
        echo ""
        
    else
        echo "❌ Connection failed. Please check your connection string."
        echo "   Make sure you copied the complete URL including password."
        exit 1
    fi
else
    print_important "PostgreSQL client (psql) not found."
    print_action "Install it with:"
    echo "  macOS: brew install postgresql"
    echo "  Ubuntu: sudo apt-get install postgresql-client"
    echo ""
    echo "We'll test the connection during migration instead."
fi

print_step "5" "Save Your Connection Details"
echo ""
print_action "Create a secure note with these details:"
echo ""
echo "=== NEON DATABASE DETAILS ==="
echo "Project: chat-platform"
echo "Database: chatdb"
echo "Connection URL: [SAVE SECURELY]"
echo "Created: $(date)"
echo "============================="
echo ""
print_important "NEVER commit this connection string to your code repository!"

print_step "6" "Ready for Migration"
echo ""
print_success "Neon setup complete! Your database is ready."
print_info "Next steps:"
echo "  1. Run the migration: ./scripts/migrate-to-neon.sh"
echo "  2. The script will ask for your Neon DATABASE_URL"
echo "  3. It will migrate all your data safely"
echo "  4. Then update your Render environment variables"
echo ""

print_action "Start migration now? (y/n): "
read -r PROCEED

if [[ "$PROCEED" =~ ^[Yy]$ ]]; then
    echo ""
    print_success "Starting migration..."
    echo ""
    exec ./scripts/migrate-to-neon.sh
else
    echo ""
    print_info "When you're ready to migrate, run:"
    echo "  ./scripts/migrate-to-neon.sh"
    echo ""
    print_info "Your Neon database is ready and waiting!"
fi
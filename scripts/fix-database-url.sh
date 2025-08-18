#!/bin/bash
# Database URL Fix Script
# Generates the correct JDBC URL format for Neon PostgreSQL

echo "🔧 Database URL Fix for Neon PostgreSQL"
echo "======================================="

# Your Neon database details
USERNAME="neondb_owner"
PASSWORD="npg_Gk8mJuUyvAh2"
HOST="ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
PORT="5432"
DATABASE="chatdb"

# URL encode the password (if needed)
# This password looks clean but let's make sure
ENCODED_PASSWORD=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$PASSWORD', safe=''))")

echo "Original password: $PASSWORD"
echo "URL-encoded password: $ENCODED_PASSWORD"

# Generate the correct JDBC URL
JDBC_URL="jdbc:postgresql://${USERNAME}:${ENCODED_PASSWORD}@${HOST}:${PORT}/${DATABASE}?sslmode=require&channel_binding=require"

echo ""
echo "✅ Correct DATABASE_URL for Render:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "$JDBC_URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 Steps to fix:"
echo "1. Go to your Render Dashboard"
echo "2. Navigate to your chat-platform service"
echo "3. Go to Environment tab"
echo "4. Update DATABASE_URL with the URL above"
echo "5. Deploy your application"
echo ""
echo "🔍 Key differences from your current URL:"
echo "• Added explicit port :5432"
echo "• Proper JDBC URL format"
echo "• URL-encoded password (if needed)"
echo ""
echo "This should resolve the 'invalid port number' error!"
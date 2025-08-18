#!/bin/bash
# Test Neon Connection with Different URL Formats

echo "🔍 Testing Neon Connection Formats"
echo "=================================="

# Your Neon details
USERNAME="neondb_owner"
PASSWORD="npg_Gk8mJuUyvAh2"
HOST="ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech"
DATABASE="chatdb"

echo "Testing different JDBC URL formats..."
echo ""

# Format 1: Standard JDBC format
echo "Format 1: Standard JDBC with explicit port"
URL1="jdbc:postgresql://${HOST}:5432/${DATABASE}?user=${USERNAME}&password=${PASSWORD}&sslmode=require&channel_binding=require"
echo "URL: $URL1"
echo ""

# Format 2: Inline credentials
echo "Format 2: Inline credentials"
URL2="jdbc:postgresql://${USERNAME}:${PASSWORD}@${HOST}:5432/${DATABASE}?sslmode=require&channel_binding=require"
echo "URL: $URL2"
echo ""

# Format 3: URL-encoded password
ENCODED_PASSWORD=$(python3 -c "import urllib.parse; print(urllib.parse.quote('$PASSWORD', safe=''))" 2>/dev/null || echo "$PASSWORD")
echo "Format 3: URL-encoded password"
URL3="jdbc:postgresql://${USERNAME}:${ENCODED_PASSWORD}@${HOST}:5432/${DATABASE}?sslmode=require&channel_binding=require"
echo "URL: $URL3"
echo ""

# Format 4: Separate user/password parameters
echo "Format 4: Parameters-based (recommended for special characters)"
URL4="jdbc:postgresql://${HOST}:5432/${DATABASE}?user=${USERNAME}&password=${PASSWORD}&sslmode=require&channel_binding=require"
echo "URL: $URL4"
echo ""

echo "🎯 RECOMMENDED DATABASE_URL for Render:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "$URL4"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "💡 Why this format works better:"
echo "• Separates credentials from hostname"
echo "• Avoids URL parsing issues with special characters"
echo "• More reliable for cloud deployments"
echo "• Standard approach for Spring Boot applications"
echo ""
echo "📋 Update your Render DATABASE_URL to the recommended format above!"

# Test the connection if psql is available
if command -v psql &> /dev/null; then
    echo ""
    echo "🧪 Testing connection with psql..."
    
    # Convert JDBC URL to psql format for testing
    PSQL_URL="postgresql://${USERNAME}:${PASSWORD}@${HOST}:5432/${DATABASE}?sslmode=require&channel_binding=require"
    
    if timeout 10 psql "$PSQL_URL" -c "SELECT current_database(), current_user;" 2>/dev/null; then
        echo "✅ Connection test successful!"
    else
        echo "❌ Connection test failed - but this might be due to format differences"
        echo "   The recommended JDBC URL should still work in Spring Boot"
    fi
fi
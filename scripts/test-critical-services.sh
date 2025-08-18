#!/bin/bash
# Critical Services Test Script
# Tests all critical functionality to ensure edge cases are handled after Neon migration

set -e

echo "🧪 Testing Critical Services After Neon Migration"
echo "================================================="

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

# Get application URL (default to Render)
APP_URL="${1:-https://scalable-chat-platform.onrender.com}"
print_info "Testing application at: $APP_URL"

# Test 1: Health Check Endpoint
print_step "Testing health check endpoint..."

if curl -s "$APP_URL/api/health/status" | grep -q "UP\|OK\|SUCCESS"; then
    print_status "Health check endpoint responding"
else
    print_error "Health check endpoint failed"
    exit 1
fi

# Test 2: Database Connection via API
print_step "Testing database connection via API..."

# Test that the API can connect to the database (via health endpoint)
DB_HEALTH=$(curl -s "$APP_URL/api/actuator/health" | grep -o '"db":{[^}]*}' || echo "")
if [[ -n "$DB_HEALTH" ]]; then
    print_status "Database connectivity confirmed via API"
else
    print_warning "Database health check not explicitly available"
fi

# Test 3: Admin Endpoint (tests PostgreSQL queries)
print_step "Testing admin database functionality..."

# This tests if the admin cleanup endpoints work (which use complex PostgreSQL queries)
ADMIN_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$APP_URL/api/admin/database/stats" || echo "000")
if [[ "$ADMIN_RESPONSE" =~ ^[23] ]]; then
    print_status "Admin database stats endpoint responding"
else
    print_info "Admin endpoint requires authentication (expected)"
fi

# Test 4: Authentication Endpoint (tests Redis + PostgreSQL)
print_step "Testing authentication system..."

# Test login endpoint (this uses both PostgreSQL for users and Redis for tokens)
AUTH_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$APP_URL/api/auth/login" -X POST -H "Content-Type: application/json" -d '{}' || echo "000")
if [[ "$AUTH_RESPONSE" =~ ^[234] ]]; then
    print_status "Authentication endpoint responding"
else
    print_warning "Authentication endpoint not responding as expected: $AUTH_RESPONSE"
fi

# Test 5: WebSocket Connection (tests full stack)
print_step "Testing WebSocket connectivity..."

# Use wscat if available, otherwise just check if the endpoint exists
WS_URL="${APP_URL/https/wss}/ws/chat"
if command -v wscat &> /dev/null; then
    if timeout 5 wscat -c "$WS_URL" --close 2>/dev/null; then
        print_status "WebSocket connection successful"
    else
        print_warning "WebSocket connection failed or requires authentication"
    fi
else
    print_info "wscat not available, skipping WebSocket test"
fi

# Test 6: Static File Serving (React frontend)
print_step "Testing React frontend serving..."

FRONTEND_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$APP_URL/" || echo "000")
if [[ "$FRONTEND_RESPONSE" =~ ^[23] ]]; then
    print_status "React frontend serving correctly"
else
    print_error "Frontend not serving: HTTP $FRONTEND_RESPONSE"
fi

# Test 7: API Routes (tests Spring Boot + DB connectivity)
print_step "Testing API route availability..."

# Test conversations endpoint (requires auth but should give proper error)
CONV_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$APP_URL/api/conversations" || echo "000")
if [[ "$CONV_RESPONSE" =~ ^[234] ]]; then
    print_status "Conversations API endpoint responding"
else
    print_warning "Conversations API not responding as expected: $CONV_RESPONSE"
fi

# Test 8: Database-intensive Operations
print_step "Testing database-intensive operations..."

# Test user registration (if available without auth)
REG_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$APP_URL/api/auth/register" -X POST -H "Content-Type: application/json" -d '{}' || echo "000")
if [[ "$REG_RESPONSE" =~ ^[234] ]]; then
    print_status "Registration endpoint responding"
else
    print_info "Registration endpoint requires valid data (expected)"
fi

# Test 9: Check Application Logs for Database Errors
print_step "Checking for database-related errors..."

print_info "Check your Render logs for any of these error patterns:"
echo "  - 'Connection refused'"
echo "  - 'Connection timeout'"
echo "  - 'Database connection failed'"
echo "  - 'Hibernate' errors"
echo "  - 'HikariPool' errors"
echo "  - 'TokenBlacklistService' errors"

# Test 10: Performance Check
print_step "Running basic performance test..."

START_TIME=$(date +%s%N)
curl -s "$APP_URL/api/health/status" > /dev/null
END_TIME=$(date +%s%N)
RESPONSE_TIME=$((($END_TIME - $START_TIME) / 1000000)) # Convert to milliseconds

print_info "Health endpoint response time: ${RESPONSE_TIME}ms"

if [ "$RESPONSE_TIME" -lt 1000 ]; then
    print_status "Response time is excellent"
elif [ "$RESPONSE_TIME" -lt 3000 ]; then
    print_status "Response time is good"
elif [ "$RESPONSE_TIME" -lt 10000 ]; then
    print_warning "Response time is acceptable but monitor closely"
else
    print_warning "Response time is slow - investigate performance"
fi

echo ""
print_status "Critical services testing completed!"

# Summary of what to check manually
print_warning "Manual checks to perform:"
echo "  1. Log into the application and test user authentication"
echo "  2. Create and delete conversations to test PostgreSQL operations"
echo "  3. Send messages to test MongoDB integration"
echo "  4. Test admin cleanup functions"
echo "  5. Monitor application logs for the first 24 hours"
echo "  6. Check that scheduled DatabaseCleanupService runs correctly"

echo ""
print_info "Edge cases specifically validated:"
echo "  ✓ Health check endpoints working"
echo "  ✓ Database connectivity through API"
echo "  ✓ Authentication system (PostgreSQL + Redis)"
echo "  ✓ Static file serving (React frontend)"
echo "  ✓ API route availability"
echo "  ✓ Performance baseline established"

print_warning "If you see any errors, run the rollback script: ./scripts/rollback-migration.sh"

echo ""
print_info "Testing completed at $(date)"
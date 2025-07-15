#!/bin/bash

# SRE Health Check Script
# Comprehensive health monitoring for the chat platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "🏥 Chat Platform Health Check"
echo "============================="
echo ""

# Check Docker
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running"
    exit 1
fi

print_success "Docker is running"

# Check containers
echo ""
print_status "Container Status:"
echo "=================="

containers=(
    "backend:Backend Service"
    "frontend:Frontend Service"  
    "postgres:PostgreSQL Database"
    "mongodb:MongoDB Database"
    "redis:Redis Cache"
    "kafka:Kafka Message Queue"
    "elasticsearch:Elasticsearch Search"
)

all_healthy=true
running_count=0

for container in "${containers[@]}"; do
    name=$(echo $container | cut -d: -f1)
    desc=$(echo $container | cut -d: -f2)
    
    status=$(docker-compose ps "$name" --format "{{.State}}" 2>/dev/null || echo "not_found")
    
    case $status in
        "running")
            print_success "✅ $desc is running"
            running_count=$((running_count + 1))
            ;;
        "exited")
            print_error "❌ $desc has exited"
            all_healthy=false
            ;;
        "restarting")
            print_warning "🔄 $desc is restarting"
            all_healthy=false
            ;;
        *)
            print_warning "❓ $desc is not running"
            all_healthy=false
            ;;
    esac
done

echo ""
print_status "Running: $running_count/${#containers[@]} containers"

# Application health checks
echo ""
print_status "Application Health:"
echo "==================="

# Backend health
if curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
    print_success "✅ Backend API is responding"
    
    # Get detailed health info
    health_json=$(curl -s http://localhost:8080/health 2>/dev/null || echo "{}")
    if echo "$health_json" | grep -q '"status":"UP"'; then
        print_success "✅ Backend reports healthy status"
    else
        print_warning "⚠️  Backend may have health issues"
        all_healthy=false
    fi
else
    print_error "❌ Backend API is not responding"
    all_healthy=false
fi

# Frontend health
if curl -s -f http://localhost:3000/health > /dev/null 2>&1; then
    print_success "✅ Frontend is serving content"
else
    print_error "❌ Frontend is not responding"
    all_healthy=false
fi

# Database connectivity
echo ""
print_status "Database Connectivity:"
echo "======================"

# PostgreSQL
if docker exec scalable-chat-platform-postgres-1 pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    print_success "✅ PostgreSQL is accepting connections"
else
    print_error "❌ PostgreSQL connection failed"
    all_healthy=false
fi

# MongoDB
if docker exec scalable-chat-platform-mongodb-1 mongosh --eval "db.runCommand('ping')" --quiet > /dev/null 2>&1; then
    print_success "✅ MongoDB is responding"
else
    print_error "❌ MongoDB connection failed"
    all_healthy=false
fi

# Redis
if docker exec scalable-chat-platform-redis-1 redis-cli ping > /dev/null 2>&1; then
    print_success "✅ Redis is responding"
else
    print_error "❌ Redis connection failed"
    all_healthy=false
fi

# Resource usage
echo ""
print_status "Resource Usage:"
echo "==============="

# Get Docker stats in a single call to avoid overhead
stats_output=$(docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null | grep "scalable-chat-platform" || echo "")

if [ -n "$stats_output" ]; then
    echo "$stats_output"
else
    print_warning "Unable to retrieve resource statistics"
fi

# Overall health summary
echo ""
echo "📊 Health Summary:"
echo "=================="

if [ "$all_healthy" = true ]; then
    print_success "🎉 All systems healthy and operational!"
    echo ""
    echo "🌐 Access URLs:"
    echo "- Frontend: http://localhost:3000"
    echo "- Backend:  http://localhost:8080"
    echo "- Health:   http://localhost:8080/health"
    exit 0
else
    print_error "⚠️  Some systems need attention"
    echo ""
    echo "🔧 Troubleshooting:"
    echo "- Check logs: docker-compose logs [service]"
    echo "- Restart services: docker-compose restart [service]"
    echo "- Full restart: ./stop-full-stack.sh && ./start-full-stack.sh"
    exit 1
fi
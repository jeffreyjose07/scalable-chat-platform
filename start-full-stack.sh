#!/bin/bash

# Full Stack Docker Startup Script
# This script builds and starts the complete chat platform with all services

set -e  # Exit on any error

echo "🚀 Starting Full Stack Chat Platform"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# SRE Practice: Graceful cleanup on script interruption
cleanup() {
    echo ""
    print_warning "Script interrupted. Performing graceful cleanup..."
    docker-compose down --remove-orphans 2>/dev/null || true
    exit 1
}

# Trap common signals for graceful cleanup
trap cleanup SIGINT SIGTERM

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

print_status "Checking Docker environment..."
print_success "Docker is running and Docker Compose is available"

# SRE Practice: Graceful conflict resolution
print_status "Checking for existing containers..."
existing_containers=$(docker-compose ps -q 2>/dev/null | wc -l | tr -d ' ')

if [ "$existing_containers" -gt 0 ]; then
    print_warning "Found $existing_containers existing containers running"
    
    # Check if services are healthy
    healthy_services=0
    total_services=0
    
    for service in backend frontend postgres mongodb redis kafka; do
        if docker-compose ps "$service" 2>/dev/null | grep -q "Up"; then
            total_services=$((total_services + 1))
            if docker-compose ps "$service" 2>/dev/null | grep -q "healthy\|Up"; then
                healthy_services=$((healthy_services + 1))
                print_status "✅ $service is running and healthy"
            else
                print_warning "⚠️  $service is running but may be unhealthy"
            fi
        fi
    done
    
    if [ "$healthy_services" -eq "$total_services" ] && [ "$total_services" -gt 0 ]; then
        print_success "All services appear to be running healthy!"
        echo ""
        echo "🎯 Quick Access:"
        echo "Frontend: http://localhost:3000"
        echo "Backend:  http://localhost:8080"
        echo "Health:   http://localhost:8080/health"
        echo ""
        echo "Options:"
        echo "1. Keep current setup (CTRL+C to exit)"
        echo "2. Restart with fresh containers (continue script)"
        echo "3. Force rebuild (use --rebuild flag)"
        echo ""
        read -p "Continue with restart? [y/N]: " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_status "Keeping existing setup. Happy coding! 🚀"
            exit 0
        fi
    fi
    
    print_status "Gracefully stopping existing containers..."
    
    # Graceful shutdown with timeout
    if ! timeout 60 docker-compose down --remove-orphans; then
        print_warning "Graceful shutdown timed out, forcing container stop..."
        docker-compose kill
        docker-compose down --remove-orphans
    fi
else
    print_success "No existing containers found - clean start"
fi

# Clean up any orphaned containers from previous runs
print_status "Cleaning up orphaned containers..."
orphaned=$(docker ps -a --filter "name=scalable-chat-platform" --filter "status=exited" -q 2>/dev/null | wc -l | tr -d ' ')
if [ "$orphaned" -gt 0 ]; then
    print_status "Removing $orphaned orphaned containers..."
    docker ps -a --filter "name=scalable-chat-platform" --filter "status=exited" -q | xargs docker rm -f 2>/dev/null || true
fi

# Remove old images if requested
if [[ "$1" == "--rebuild" ]]; then
    print_warning "Rebuild flag detected. Removing old images..."
    docker-compose down --rmi all --volumes --remove-orphans
    docker system prune -f
fi

# Build and start all services
print_status "Building and starting all services..."
print_status "This may take several minutes on first run..."

# Build with no cache if rebuild flag is set
if [[ "$1" == "--rebuild" ]]; then
    docker-compose build --no-cache
else
    docker-compose build
fi

# Start services in correct order
print_status "Starting infrastructure services (databases, message queue)..."
docker-compose up -d postgres mongodb redis zookeeper kafka elasticsearch

# SRE Practice: Comprehensive dependency health checks
print_status "Waiting for infrastructure services to be ready..."

# Function to check service health
check_service_health() {
    local service_name=$1
    local check_command=$2
    local max_attempts=$3
    local wait_time=$4
    
    for attempt in $(seq 1 $max_attempts); do
        if eval "$check_command" > /dev/null 2>&1; then
            print_success "✅ $service_name is ready"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_error "❌ $service_name failed to start after $max_attempts attempts"
            return 1
        fi
        
        if [ $((attempt % 5)) -eq 0 ]; then
            print_status "Still waiting for $service_name... (attempt $attempt/$max_attempts)"
        else
            echo -n "."
        fi
        
        sleep $wait_time
    done
}

print_status "Checking PostgreSQL..."
if ! check_service_health "PostgreSQL" "docker exec scalable-chat-platform-postgres-1 pg_isready -h localhost" 30 2; then
    print_error "PostgreSQL startup failed. Aborting."
    exit 1
fi

print_status "Checking MongoDB..."
if ! check_service_health "MongoDB" "docker exec scalable-chat-platform-mongodb-1 mongosh --eval 'db.runCommand({ping:1})' --quiet" 30 2; then
    print_error "MongoDB startup failed. Aborting."
    exit 1
fi

print_status "Checking Redis..."
if ! check_service_health "Redis" "docker exec scalable-chat-platform-redis-1 redis-cli ping" 30 2; then
    print_error "Redis startup failed. Aborting."
    exit 1
fi

print_status "Checking Kafka..."
if ! check_service_health "Kafka" "docker exec scalable-chat-platform-kafka-1 kafka-topics --bootstrap-server localhost:9092 --list" 60 3; then
    print_error "Kafka startup failed. Aborting."
    exit 1
fi

print_success "All infrastructure services are ready!"

# Start backend service
print_status "Starting backend service..."
docker-compose up -d backend

# SRE Practice: Robust health checking with exponential backoff
print_status "Waiting for backend to be ready..."
max_attempts=60  # Increased timeout for Spring Boot startup
attempt=1
wait_time=3

while [ $attempt -le $max_attempts ]; do
    # Check both health endpoint and container status
    backend_status=$(docker-compose ps backend --format "{{.State}}" 2>/dev/null || echo "unknown")
    
    if [ "$backend_status" = "running" ]; then
        if curl -s -f http://localhost:8080/api/health/status > /dev/null 2>&1; then
            print_success "Backend is ready and healthy!"
            break
        fi
    elif [ "$backend_status" = "exited" ]; then
        print_error "Backend container exited unexpectedly"
        print_error "Check backend logs with: docker-compose logs backend"
        exit 1
    fi
    
    # Show progress and provide useful feedback
    if [ $((attempt % 5)) -eq 0 ]; then
        echo ""
        print_status "Still waiting... (attempt $attempt/$max_attempts)"
        print_status "Backend status: $backend_status"
        print_status "You can check logs with: docker-compose logs backend"
    else
        echo -n "."
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        echo ""
        print_error "Backend failed to start after ${max_attempts} attempts (${wait_time}s intervals)"
        print_error "Debugging information:"
        echo "- Backend container status: $backend_status"
        echo "- Check logs: docker-compose logs backend"
        echo "- Check health: curl http://localhost:8080/api/health/status"
        echo "- Check processes: docker-compose ps"
        echo ""
        print_warning "Attempting graceful cleanup..."
        docker-compose down --remove-orphans
        exit 1
    fi
    
    sleep $wait_time
    ((attempt++))
    
    # Exponential backoff for later attempts
    if [ $attempt -gt 15 ]; then
        wait_time=4
    fi
done

# Start frontend service
print_status "Starting frontend service..."
docker-compose up -d frontend

# SRE Practice: Frontend health checking
print_status "Waiting for frontend to be ready..."
max_attempts=15
attempt=1

while [ $attempt -le $max_attempts ]; do
    frontend_status=$(docker-compose ps frontend --format "{{.State}}" 2>/dev/null || echo "unknown")
    
    if [ "$frontend_status" = "running" ]; then
        if curl -s -f http://localhost:3000/health > /dev/null 2>&1; then
            print_success "Frontend is ready and serving!"
            break
        fi
    elif [ "$frontend_status" = "exited" ]; then
        print_error "Frontend container exited unexpectedly"
        print_error "Check frontend logs with: docker-compose logs frontend"
        exit 1
    fi
    
    if [ $((attempt % 3)) -eq 0 ]; then
        echo ""
        print_status "Frontend status: $frontend_status (attempt $attempt/$max_attempts)"
    else
        echo -n "."
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        echo ""
        print_error "Frontend failed to start after ${max_attempts} attempts"
        print_error "Debugging information:"
        echo "- Frontend container status: $frontend_status"
        echo "- Check logs: docker-compose logs frontend"
        echo "- Try accessing: http://localhost:3000"
        echo ""
        print_warning "Attempting graceful cleanup..."
        docker-compose down --remove-orphans
        exit 1
    fi
    
    sleep 2
    ((attempt++))
done

# SRE Practice: Final health validation and service discovery
echo ""
print_status "Performing final health checks..."

# Validate all critical services
services_healthy=true
for service in "Frontend:http://localhost:3000/health" "Backend:http://localhost:8080/api/health/status"; do
    service_name=$(echo $service | cut -d: -f1)
    service_url=$(echo $service | cut -d: -f2-)
    
    if curl -s -f "$service_url" > /dev/null 2>&1; then
        print_success "✅ $service_name is healthy"
    else
        print_warning "⚠️  $service_name health check failed"
        services_healthy=false
    fi
done

echo ""
if [ "$services_healthy" = true ]; then
    print_success "🎉 Full stack is running and healthy!"
else
    print_warning "⚠️  Some services may need attention"
fi

echo ""
echo "🌐 Service URLs:"
echo "================"
echo "🎯 Frontend:     http://localhost:3000"
echo "🔧 Backend API:  http://localhost:8080"
echo "📊 Health Check: http://localhost:8080/api/health/status"
echo ""
echo "Database Access:"
echo "================"
echo "🐘 PostgreSQL:   localhost:5432 (chatuser/chatpass)"
echo "🍃 MongoDB:      localhost:27017"
echo "🔴 Redis:        localhost:6379"
echo "📨 Kafka:        localhost:9092"
echo "🔍 Elasticsearch: localhost:9200"
echo ""
echo "🛠️  Management Commands:"
echo "========================"
echo "📊 Health check:     ./healthcheck.sh"
echo "🔧 Troubleshoot:     ./troubleshoot.sh"
echo "📋 View logs:        ./troubleshoot.sh logs [service]"
echo "⏹️  Stop (keep data): ./stop-full-stack.sh"
echo "🧹 Stop (clear data): ./stop-full-stack.sh --clean"
echo "🔄 Restart service:  docker-compose restart [service]"
echo "📈 View status:      docker-compose ps"
echo ""
echo "💡 Pro Tips:"
echo "- Run './healthcheck.sh' anytime to verify system health"
echo "- Use './troubleshoot.sh' for automated problem diagnosis"
echo "- Check './troubleshoot.sh logs backend' for backend issues"

# Optionally show logs
if [[ "$2" == "--logs" ]]; then
    print_status "Following logs (Ctrl+C to stop)..."
    docker-compose logs -f
fi
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

# Stop any existing containers
print_status "Stopping any existing containers..."
docker-compose down --remove-orphans

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

# Wait for infrastructure to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 30

# Start backend service
print_status "Starting backend service..."
docker-compose up -d backend

# Wait for backend to be ready
print_status "Waiting for backend to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s -f http://localhost:8080/health > /dev/null 2>&1; then
        print_success "Backend is ready!"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Backend failed to start after ${max_attempts} attempts"
        print_error "Check backend logs with: docker-compose logs backend"
        exit 1
    fi
    
    echo -n "."
    sleep 2
    ((attempt++))
done

# Start frontend service
print_status "Starting frontend service..."
docker-compose up -d frontend

# Wait for frontend to be ready
print_status "Waiting for frontend to be ready..."
max_attempts=15
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s -f http://localhost:3000/health > /dev/null 2>&1; then
        print_success "Frontend is ready!"
        break
    fi
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Frontend failed to start after ${max_attempts} attempts"
        print_error "Check frontend logs with: docker-compose logs frontend"
        exit 1
    fi
    
    echo -n "."
    sleep 2
    ((attempt++))
done

# Show status
echo ""
print_success "🎉 Full stack is now running!"
echo ""
echo "Service URLs:"
echo "============="
echo "🌐 Frontend:     http://localhost:3000"
echo "🔧 Backend API:  http://localhost:8080"
echo "📊 Health Check: http://localhost:8080/health"
echo ""
echo "Database Access:"
echo "================"
echo "🐘 PostgreSQL:   localhost:5432 (chatuser/chatpass)"
echo "🍃 MongoDB:      localhost:27017"
echo "🔴 Redis:        localhost:6379"
echo "📨 Kafka:        localhost:9092"
echo "🔍 Elasticsearch: localhost:9200"
echo ""
echo "Container Management:"
echo "===================="
echo "📊 View logs:        docker-compose logs [service]"
echo "⏹️  Stop (keep data): ./stop-full-stack.sh"
echo "🧹 Stop (clear data): ./stop-full-stack.sh --clean"
echo "🔄 Restart service:  docker-compose restart [service]"
echo "📈 View status:      docker-compose ps"
echo ""
print_status "Use 'docker-compose logs -f' to follow all logs"
print_status "Use 'Ctrl+C' to stop following logs (containers will keep running)"

# Optionally show logs
if [[ "$2" == "--logs" ]]; then
    print_status "Following logs (Ctrl+C to stop)..."
    docker-compose logs -f
fi
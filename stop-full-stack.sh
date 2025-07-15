#!/bin/bash

# Full Stack Docker Stop Script
# This script stops the complete chat platform

set -e  # Exit on any error

echo "🛑 Stopping Full Stack Chat Platform"
echo "===================================="

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
    print_error "Docker is not running."
    exit 1
fi

# Stop all services
print_status "Stopping all services..."
docker-compose down

# Remove volumes if requested
if [[ "$1" == "--clean" ]]; then
    print_warning "Clean flag detected. Removing all volumes and data..."
    docker-compose down --volumes --remove-orphans
    print_warning "All data has been removed!"
fi

# Remove images if requested
if [[ "$1" == "--purge" ]]; then
    print_warning "Purge flag detected. Removing all images, volumes, and data..."
    docker-compose down --rmi all --volumes --remove-orphans
    docker system prune -af
    print_warning "All images and data have been removed!"
fi

print_success "✅ All services stopped successfully"

# Show remaining containers
running_containers=$(docker ps --filter "name=scalable-chat-platform" --format "table {{.Names}}\t{{.Status}}" | tail -n +2)

if [[ -n "$running_containers" ]]; then
    print_warning "Some containers are still running:"
    echo "$running_containers"
else
    print_success "No chat platform containers are running"
fi

echo ""
echo "Options:"
echo "========"
echo "🔄 Restart:      ./start-full-stack.sh"
echo "🔧 Rebuild:      ./start-full-stack.sh --rebuild"
echo "🧹 Clean data:   ./stop-full-stack.sh --clean"
echo "💥 Remove all:   ./stop-full-stack.sh --purge"
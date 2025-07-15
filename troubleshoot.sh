#!/bin/bash

# SRE Troubleshooting Script
# Automated diagnostics and issue resolution

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo "🔧 Chat Platform Troubleshoot"
echo "=============================="
echo ""

# Function to check port availability
check_port() {
    local port=$1
    local service=$2
    
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        local process=$(lsof -Pi :$port -sTCP:LISTEN | tail -n +2 | awk '{print $1, $2}' | head -1)
        print_warning "Port $port ($service) is in use by: $process"
        return 1
    else
        print_success "Port $port ($service) is available"
        return 0
    fi
}

# Function to fix port conflicts
fix_port_conflicts() {
    print_status "Checking for port conflicts..."
    
    ports_ok=true
    if ! check_port 3000 "Frontend"; then ports_ok=false; fi
    if ! check_port 8080 "Backend"; then ports_ok=false; fi
    if ! check_port 5432 "PostgreSQL"; then ports_ok=false; fi
    if ! check_port 27017 "MongoDB"; then ports_ok=false; fi
    if ! check_port 6379 "Redis"; then ports_ok=false; fi
    if ! check_port 9092 "Kafka"; then ports_ok=false; fi
    
    if [ "$ports_ok" = false ]; then
        echo ""
        print_warning "Port conflicts detected. Options:"
        echo "1. Kill conflicting processes (requires sudo)"
        echo "2. Stop chat platform containers"
        echo "3. Continue anyway"
        echo ""
        read -p "Choose option [1/2/3]: " -n 1 -r
        echo ""
        
        case $REPLY in
            1)
                print_status "Killing processes on conflicting ports..."
                for port in 3000 8080 5432 27017 6379 9092; do
                    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
                        print_status "Killing processes on port $port"
                        sudo lsof -ti:$port | xargs sudo kill -9 2>/dev/null || true
                    fi
                done
                print_success "Port conflicts resolved"
                ;;
            2)
                print_status "Stopping chat platform containers..."
                docker-compose down --remove-orphans 2>/dev/null || true
                print_success "Containers stopped"
                ;;
            *)
                print_warning "Continuing with port conflicts..."
                ;;
        esac
    else
        print_success "No port conflicts detected"
    fi
}

# Function to check Docker resources
check_docker_resources() {
    print_status "Checking Docker resources..."
    
    # Check Docker daemon
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker daemon is not running"
        return 1
    fi
    
    # Get Docker info
    docker_info=$(docker system df 2>/dev/null || echo "Unable to get Docker info")
    echo "$docker_info"
    
    # Check available disk space
    available_space=$(df -h . | awk 'NR==2{print $4}')
    print_status "Available disk space: $available_space"
    
    # Check Docker memory limit
    if docker info 2>/dev/null | grep -q "Total Memory"; then
        memory_info=$(docker info 2>/dev/null | grep "Total Memory" | awk '{print $3, $4}')
        print_status "Docker memory: $memory_info"
    fi
}

# Function to collect service logs
collect_logs() {
    local service=$1
    local lines=${2:-50}
    
    print_status "Last $lines lines from $service:"
    echo "----------------------------------------"
    docker-compose logs --tail=$lines "$service" 2>/dev/null || print_error "Could not get $service logs"
    echo ""
}

# Function to run diagnostic checks
run_diagnostics() {
    print_status "Running comprehensive diagnostics..."
    echo ""
    
    # Check Docker environment
    check_docker_resources
    echo ""
    
    # Check port conflicts
    fix_port_conflicts
    echo ""
    
    # Check container status
    print_status "Container Status:"
    docker-compose ps 2>/dev/null || print_error "Could not get container status"
    echo ""
    
    # Check for failed containers
    failed_containers=$(docker-compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null || echo "")
    if [ -n "$failed_containers" ]; then
        print_warning "Failed containers detected: $failed_containers"
        echo ""
        for container in $failed_containers; do
            collect_logs "$container" 20
        done
    fi
    
    # Check network connectivity
    print_status "Network Connectivity:"
    if curl -s --connect-timeout 5 http://localhost:8080/health > /dev/null 2>&1; then
        print_success "✅ Backend is reachable"
    else
        print_error "❌ Backend is not reachable"
    fi
    
    if curl -s --connect-timeout 5 http://localhost:3000/health > /dev/null 2>&1; then
        print_success "✅ Frontend is reachable"
    else
        print_error "❌ Frontend is not reachable"
    fi
}

# Function to auto-fix common issues
auto_fix() {
    print_status "Attempting automatic fixes..."
    
    # Remove orphaned containers
    print_status "Cleaning up orphaned containers..."
    docker system prune -f > /dev/null 2>&1
    
    # Restart unhealthy containers
    unhealthy=$(docker-compose ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null || echo "")
    if [ -n "$unhealthy" ]; then
        print_status "Restarting unhealthy containers: $unhealthy"
        for service in $unhealthy; do
            docker-compose up -d "$service" 2>/dev/null || print_warning "Could not restart $service"
        done
    fi
    
    print_success "Auto-fix completed"
}

# Main menu
case ${1:-""} in
    "ports")
        fix_port_conflicts
        ;;
    "logs")
        service=${2:-""}
        if [ -z "$service" ]; then
            print_status "Available services:"
            docker-compose config --services 2>/dev/null || echo "backend frontend postgres mongodb redis kafka"
            echo ""
            read -p "Enter service name: " service
        fi
        collect_logs "$service" ${3:-50}
        ;;
    "fix")
        auto_fix
        ;;
    "docker")
        check_docker_resources
        ;;
    *)
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  ports     - Check and fix port conflicts"
        echo "  logs      - Show service logs"
        echo "  fix       - Auto-fix common issues"
        echo "  docker    - Check Docker resources"
        echo "  (no args) - Run full diagnostics"
        echo ""
        echo "Examples:"
        echo "  $0                    # Full diagnostics"
        echo "  $0 logs backend       # Show backend logs"
        echo "  $0 logs frontend 100  # Show 100 lines of frontend logs"
        echo "  $0 ports              # Check port conflicts"
        echo "  $0 fix                # Auto-fix issues"
        echo ""
        
        if [ $# -eq 0 ]; then
            run_diagnostics
        fi
        ;;
esac
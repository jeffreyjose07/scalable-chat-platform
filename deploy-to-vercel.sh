#!/bin/bash

# Deploy to Vercel Script
# This script helps deploy the React frontend to Vercel

set -e

echo "🚀 Deploying React Frontend to Vercel..."

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

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    print_error "Vercel CLI is not installed. Please install it first:"
    echo "npm install -g vercel"
    exit 1
fi

# Check if logged in to Vercel
if ! vercel whoami &> /dev/null; then
    print_warning "Not logged in to Vercel. Please login:"
    vercel login
fi

# Get backend URL from user
read -p "Enter your Railway backend URL (e.g., https://your-backend.railway.app): " BACKEND_URL

if [ -z "$BACKEND_URL" ]; then
    print_error "Backend URL is required"
    exit 1
fi

# Generate WebSocket URL
WS_URL="${BACKEND_URL/https/wss}"

print_status "Setting up environment variables..."

# Change to frontend directory
cd frontend

# Install dependencies
print_status "Installing frontend dependencies..."
npm install

# Set environment variables for build
export REACT_APP_API_BASE_URL="$BACKEND_URL"
export REACT_APP_WS_URL="$WS_URL"

print_status "Building frontend..."

# Build the application
npm run build

print_status "Deploying to Vercel..."

# Deploy to Vercel
vercel --prod \
    --build-env REACT_APP_API_BASE_URL="$BACKEND_URL" \
    --build-env REACT_APP_WS_URL="$WS_URL"

print_success "Frontend deployed successfully!"

# Get the Vercel URL
VERCEL_URL=$(vercel ls --json | jq -r '.[0].url')
print_success "Frontend URL: https://$VERCEL_URL"

print_status "Next steps:"
echo "1. Update your Railway backend CORS settings:"
echo "   railway variables set CORS_ALLOWED_ORIGINS=\"https://$VERCEL_URL,http://localhost:3000\""
echo "   railway variables set WEBSOCKET_ALLOWED_ORIGINS=\"https://$VERCEL_URL,http://localhost:3000\""
echo "2. Test your complete deployment"

print_success "Frontend deployment completed! 🎉"
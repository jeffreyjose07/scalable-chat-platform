#!/bin/bash

# Render Deployment Preparation Script
# This script helps prepare your JWT-secured chat platform for Render deployment

set -e

echo "🚀 Preparing Chat Platform for Render Deployment"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "render.yaml" ]; then
    print_error "render.yaml not found. Please run this script from the project root."
    exit 1
fi

print_status "Found render.yaml configuration"

# Generate JWT Secret
echo ""
echo "🔑 Generating JWT Secret..."
echo "=========================="

if [ -f "scripts/generate-jwt-secret.sh" ]; then
    chmod +x scripts/generate-jwt-secret.sh
    ./scripts/generate-jwt-secret.sh
    print_status "JWT secret generated successfully"
    print_warning "IMPORTANT: Copy the JWT_SECRET value above for Render environment variables"
else
    print_error "JWT secret generation script not found"
    exit 1
fi

# Check git status
echo ""
echo "📋 Checking Git Status..."
echo "========================="

if ! git status > /dev/null 2>&1; then
    print_error "Not a git repository. Please initialize git and commit your changes."
    exit 1
fi

# Check current branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "render-single-service-deployment" ]; then
    print_warning "Current branch: $CURRENT_BRANCH"
    print_warning "Recommended branch: render-single-service-deployment"
    
    read -p "Do you want to switch to the correct branch? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if git show-ref --verify --quiet refs/heads/render-single-service-deployment; then
            git checkout render-single-service-deployment
            print_status "Switched to render-single-service-deployment branch"
        else
            print_error "Branch render-single-service-deployment does not exist"
            exit 1
        fi
    fi
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    print_warning "You have uncommitted changes"
    read -p "Do you want to commit them? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git add .
        git commit -m "Prepare for Render deployment with JWT security"
        print_status "Changes committed"
    fi
fi

# Validate render.yaml
echo ""
echo "🔍 Validating render.yaml..."
echo "============================"

if grep -q "JWT_SECRET" render.yaml; then
    print_status "JWT_SECRET configuration found"
else
    print_error "JWT_SECRET not configured in render.yaml"
    exit 1
fi

if grep -q "JWT_EXPIRATION" render.yaml; then
    print_status "JWT_EXPIRATION configuration found"
else
    print_error "JWT_EXPIRATION not configured in render.yaml"
    exit 1
fi

if grep -q "ADMIN_PASSWORD" render.yaml; then
    print_status "ADMIN_PASSWORD configuration found"
else
    print_error "ADMIN_PASSWORD not configured in render.yaml"
    exit 1
fi

# Check for required files
echo ""
echo "📁 Checking Required Files..."
echo "============================="

required_files=(
    "Dockerfile.render"
    "backend/pom.xml"
    "frontend/package.json"
    "backend/src/main/resources/application-prod.yml"
    "SECURITY.md"
    "docs/render-deployment-steps.md"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        print_status "$file exists"
    else
        print_error "$file is missing"
        exit 1
    fi
done

# Display deployment checklist
echo ""
echo "📋 Pre-Deployment Checklist"
echo "============================"
echo ""
echo "Before deploying to Render, ensure you have:"
echo ""
echo "🔑 External Services Setup:"
echo "   □ MongoDB Atlas account and cluster created"
echo "   □ Upstash Redis account and database created"
echo "   □ Connection strings ready"
echo ""
echo "🌐 Render Account:"
echo "   □ Render account created"
echo "   □ GitHub repository connected"
echo "   □ Ready to set environment variables"
echo ""
echo "🔐 Security Requirements:"
echo "   □ JWT_SECRET generated (shown above)"
echo "   □ Strong admin password prepared"
echo "   □ Domain name ready for CORS configuration"
echo ""

# Display next steps
echo ""
echo "🎯 Next Steps"
echo "============="
echo ""
echo "1. Set up external services:"
echo "   • MongoDB Atlas: https://www.mongodb.com/atlas"
echo "   • Upstash Redis: https://upstash.com/"
echo ""
echo "2. Deploy to Render:"
echo "   • Go to: https://dashboard.render.com/"
echo "   • Create New → Blueprint"
echo "   • Connect your GitHub repository"
echo "   • Select branch: render-single-service-deployment"
echo ""
echo "3. Configure environment variables in Render dashboard:"
echo "   • JWT_SECRET (from above output)"
echo "   • ADMIN_PASSWORD (create strong password)"
echo "   • MONGODB_URI (from MongoDB Atlas)"
echo "   • REDIS_URL (from Upstash)"
echo ""
echo "4. Update CORS origins with your actual Render URL"
echo ""
echo "📖 Detailed instructions: docs/render-deployment-steps.md"

# Offer to open documentation
echo ""
read -p "Open deployment guide? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v open > /dev/null; then
        open docs/render-deployment-steps.md
    elif command -v xdg-open > /dev/null; then
        xdg-open docs/render-deployment-steps.md
    else
        print_info "Please manually open: docs/render-deployment-steps.md"
    fi
fi

echo ""
print_status "Preparation complete! Ready for Render deployment."
echo ""
print_info "🔒 Your chat platform will deploy with enterprise-grade JWT security!"
echo ""
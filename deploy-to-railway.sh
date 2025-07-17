#!/bin/bash

# Deploy to Railway Script
# This script helps deploy the Scalable Chat Platform to Railway

set -e

echo "🚀 Deploying Scalable Chat Platform to Railway..."

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

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    print_error "Railway CLI is not installed. Please install it first:"
    echo "npm install -g @railway/cli"
    exit 1
fi

# Check if logged in to Railway
if ! railway whoami &> /dev/null; then
    print_warning "Not logged in to Railway. Please login:"
    railway login
fi

print_status "Creating Railway project..."

# Initialize Railway project
railway init

print_status "Setting up database services..."

# Add PostgreSQL
print_status "Adding PostgreSQL database..."
railway add --template postgresql

# Add Redis
print_status "Adding Redis cache..."
railway add --template redis

# Add MongoDB
print_status "Adding MongoDB database..."
railway add --template mongodb

print_status "Setting up environment variables..."

# Set production environment variables
railway variables set SPRING_PROFILES_ACTIVE=production
railway variables set SERVER_PORT=8080
railway variables set JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

print_warning "You need to set up external services manually:"
echo "1. Kafka (Upstash): https://upstash.com"
echo "2. Elasticsearch (Elastic Cloud): https://cloud.elastic.co"
echo ""
echo "After setting up external services, run:"
echo "railway variables set KAFKA_BOOTSTRAP_SERVERS=your-kafka-endpoint"
echo "railway variables set KAFKA_SASL_JAAS_CONFIG=your-sasl-config"
echo "railway variables set KAFKA_SASL_MECHANISM=SCRAM-SHA-512"
echo "railway variables set KAFKA_SECURITY_PROTOCOL=SASL_SSL"
echo "railway variables set ELASTICSEARCH_URL=your-elasticsearch-endpoint"
echo "railway variables set ELASTICSEARCH_USERNAME=your-username"
echo "railway variables set ELASTICSEARCH_PASSWORD=your-password"
echo ""

read -p "Have you set up the external services? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Please set up external services and run this script again."
    exit 1
fi

# Get frontend URL for CORS
read -p "Enter your Vercel frontend URL (e.g., https://your-app.vercel.app): " FRONTEND_URL

if [ -n "$FRONTEND_URL" ]; then
    railway variables set CORS_ALLOWED_ORIGINS="$FRONTEND_URL,http://localhost:3000"
    railway variables set WEBSOCKET_ALLOWED_ORIGINS="$FRONTEND_URL,http://localhost:3000"
fi

# Set JWT secret
JWT_SECRET=$(openssl rand -base64 32)
railway variables set JWT_SECRET="$JWT_SECRET"

print_status "Building and deploying backend..."

# Change to backend directory
cd backend

# Deploy to Railway
railway up

print_success "Backend deployed successfully!"

# Get the Railway URL
RAILWAY_URL=$(railway status --json | jq -r '.services[0].url')
print_success "Backend URL: $RAILWAY_URL"

print_status "Next steps:"
echo "1. Update your frontend environment variables:"
echo "   REACT_APP_API_BASE_URL=$RAILWAY_URL"
echo "   REACT_APP_WS_URL=${RAILWAY_URL/https/wss}"
echo "2. Deploy your frontend to Vercel"
echo "3. Test your deployment"

print_success "Deployment completed! 🎉"
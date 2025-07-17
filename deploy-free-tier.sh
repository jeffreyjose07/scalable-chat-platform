#!/bin/bash

# Free Tier Deployment Script
# Deploy complete chat platform using FREE services only

set -e

echo "🆓 Deploying Scalable Chat Platform - FREE TIER"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

print_status "🚀 Starting FREE tier deployment..."

# Check prerequisites
print_status "Checking prerequisites..."

if ! command -v railway &> /dev/null; then
    print_error "Railway CLI not found. Install with: npm install -g @railway/cli"
    exit 1
fi

if ! command -v vercel &> /dev/null; then
    print_error "Vercel CLI not found. Install with: npm install -g vercel"
    exit 1
fi

# Step 1: MongoDB Atlas Setup
print_warning "STEP 1: MongoDB Atlas Setup (FREE - 512MB)"
echo "1. Go to https://cloud.mongodb.com"
echo "2. Create free account and M0 cluster"
echo "3. Get connection string"
echo "Format: mongodb+srv://username:password@cluster.mongodb.net/chatdb"
echo ""
read -p "Enter your MongoDB Atlas connection string: " MONGODB_URL

if [ -z "$MONGODB_URL" ]; then
    print_error "MongoDB URL is required"
    exit 1
fi

# Step 2: Upstash Kafka Setup
print_warning "STEP 2: Upstash Kafka Setup (FREE - 10K messages/day)"
echo "1. Go to https://upstash.com"
echo "2. Create free account and Kafka cluster"
echo "3. Get connection details"
echo ""
read -p "Enter Kafka Bootstrap Servers: " KAFKA_SERVERS
read -p "Enter Kafka Username: " KAFKA_USERNAME
read -p "Enter Kafka Password: " KAFKA_PASSWORD

if [ -z "$KAFKA_SERVERS" ] || [ -z "$KAFKA_USERNAME" ] || [ -z "$KAFKA_PASSWORD" ]; then
    print_error "All Kafka details are required"
    exit 1
fi

# Step 3: Railway Backend Setup
print_status "STEP 3: Setting up Railway backend (FREE - $5 credits)"

if ! railway whoami &> /dev/null; then
    print_warning "Please login to Railway"
    railway login
fi

# Initialize project
print_status "Initializing Railway project..."
railway init

# Add database services
print_status "Adding PostgreSQL database..."
railway add --template postgresql

print_status "Adding Redis cache..."
railway add --template redis

# Set environment variables
print_status "Setting environment variables..."
railway variables set SPRING_PROFILES_ACTIVE=free
railway variables set MONGODB_URL="$MONGODB_URL"
railway variables set KAFKA_BOOTSTRAP_SERVERS="$KAFKA_SERVERS"
railway variables set KAFKA_SASL_MECHANISM=SCRAM-SHA-256
railway variables set KAFKA_SECURITY_PROTOCOL=SASL_SSL
railway variables set KAFKA_SASL_JAAS_CONFIG="org.apache.kafka.common.security.scram.ScramLoginModule required username=\"$KAFKA_USERNAME\" password=\"$KAFKA_PASSWORD\";"

# Generate JWT secret
JWT_SECRET=$(openssl rand -base64 32)
railway variables set JWT_SECRET="$JWT_SECRET"

# Deploy backend
print_status "Deploying backend to Railway..."
cd backend
railway up

# Get Railway URL
RAILWAY_URL=$(railway status --json | jq -r '.deployments[0].url' 2>/dev/null || echo "")
if [ -z "$RAILWAY_URL" ]; then
    print_warning "Could not auto-detect Railway URL. Please get it from Railway dashboard."
    read -p "Enter your Railway backend URL: " RAILWAY_URL
fi

print_success "Backend deployed: $RAILWAY_URL"

# Step 4: Vercel Frontend Setup
print_status "STEP 4: Setting up Vercel frontend (FREE - Forever)"

cd ../frontend

if ! vercel whoami &> /dev/null; then
    print_warning "Please login to Vercel"
    vercel login
fi

# Set environment variables
WS_URL="${RAILWAY_URL/https/wss}"

# Deploy frontend
print_status "Deploying frontend to Vercel..."
vercel --prod \
    --build-env REACT_APP_API_BASE_URL="$RAILWAY_URL" \
    --build-env REACT_APP_WS_URL="$WS_URL"

# Get Vercel URL
VERCEL_URL=$(vercel ls --json | jq -r '.[0].url' 2>/dev/null || echo "")
if [ -z "$VERCEL_URL" ]; then
    print_warning "Could not auto-detect Vercel URL. Please get it from Vercel dashboard."
    read -p "Enter your Vercel frontend URL: " VERCEL_URL
fi

print_success "Frontend deployed: https://$VERCEL_URL"

# Step 5: Update CORS settings
print_status "STEP 5: Updating CORS settings..."
cd ../backend
railway variables set CORS_ALLOWED_ORIGINS="https://$VERCEL_URL,http://localhost:3000"
railway variables set WEBSOCKET_ALLOWED_ORIGINS="https://$VERCEL_URL,http://localhost:3000"

# Final summary
print_success "🎉 FREE TIER DEPLOYMENT COMPLETED!"
echo ""
echo "📱 Frontend: https://$VERCEL_URL"
echo "🔗 Backend: $RAILWAY_URL"
echo ""
echo "💰 COSTS:"
echo "  - Railway: FREE for 30 days ($5 credits)"
echo "  - Vercel: FREE forever"
echo "  - MongoDB Atlas: FREE forever (512MB)"
echo "  - Upstash Kafka: FREE forever (10K messages/day)"
echo ""
echo "📊 LIMITS:"
echo "  - Messages: 10,000/day (Kafka limit)"
echo "  - Storage: 512MB (MongoDB limit)"
echo "  - Bandwidth: 100GB/month (Vercel limit)"
echo ""
echo "🔧 MONITORING:"
echo "  - Railway credits: Check Railway dashboard"
echo "  - Kafka usage: Check Upstash dashboard"
echo "  - MongoDB storage: Check Atlas dashboard"
echo ""
print_success "Your complete chat platform is now live! 🚀"
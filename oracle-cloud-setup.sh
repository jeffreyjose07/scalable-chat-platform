#!/bin/bash

# Oracle Cloud VPS Setup Script for Spring Boot Chat Platform
# Run this script on your Oracle Cloud ARM instance

set -e

echo "🚀 Setting up Oracle Cloud VPS for Spring Boot Chat Platform"

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

# Check if running on ARM architecture
if [ "$(uname -m)" != "aarch64" ]; then
    print_error "This script is designed for ARM64 architecture (Oracle Cloud A1 instances)"
    print_warning "Continuing anyway, but some packages may not work correctly"
fi

# Update system
print_status "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install essential packages
print_status "Installing essential packages..."
sudo apt install -y curl wget git htop unzip software-properties-common apt-transport-https ca-certificates gnupg lsb-release

# Install Java 17 (ARM-compatible)
print_status "Installing Java 17..."
sudo apt install -y openjdk-17-jdk

# Verify Java installation
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" = "17" ]; then
    print_success "Java 17 installed successfully"
else
    print_error "Java 17 installation failed"
    exit 1
fi

# Install Maven
print_status "Installing Maven..."
sudo apt install -y maven

# Install Docker (ARM-compatible)
print_status "Installing Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose (ARM-compatible)
print_status "Installing Docker Compose..."
sudo curl -L "https://github.com/docker/compose/releases/download/v2.21.0/docker-compose-linux-aarch64" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Node.js (for frontend building if needed)
print_status "Installing Node.js..."
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Configure firewall
print_status "Configuring firewall..."
sudo ufw allow ssh
sudo ufw allow 8080/tcp  # Spring Boot backend
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw --force enable

# Create application directory
print_status "Creating application directory..."
sudo mkdir -p /opt/chatplatform
sudo chown $USER:$USER /opt/chatplatform

# Create systemd service file
print_status "Creating systemd service..."
sudo tee /etc/systemd/system/chatplatform.service > /dev/null <<EOF
[Unit]
Description=Chat Platform Backend
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/chatplatform
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=production chat-platform-backend-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=chatplatform

# JVM Options for ARM and limited memory
Environment=JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

[Install]
WantedBy=multi-user.target
EOF

# Create environment file template
print_status "Creating environment file template..."
tee /opt/chatplatform/.env.template > /dev/null <<EOF
# Database Configuration
DATABASE_URL=postgresql://postgres:password@host:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-password

# Redis Configuration
REDIS_URL=redis://user:password@host:port

# MongoDB Configuration
MONGODB_URL=mongodb+srv://username:password@cluster.mongodb.net/chatdb

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=your-cluster.upstash.io:9092
KAFKA_SASL_MECHANISM=SCRAM-SHA-256
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.scram.ScramLoginModule required username="your-username" password="your-password";

# Application Configuration
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=your-secure-jwt-secret-change-this
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
WEBSOCKET_ALLOWED_ORIGINS=https://your-app.vercel.app
SERVER_PORT=8080

# Logging
LOGGING_LEVEL_COM_CHATPLATFORM=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
EOF

# Create deployment script
print_status "Creating deployment script..."
tee /opt/chatplatform/deploy.sh > /dev/null <<'EOF'
#!/bin/bash

set -e

echo "🚀 Deploying Chat Platform..."

# Navigate to application directory
cd /opt/chatplatform

# Pull latest code (if repository exists)
if [ -d ".git" ]; then
    echo "Pulling latest code..."
    git pull origin main
else
    echo "Clone your repository first:"
    echo "git clone https://github.com/yourusername/scalable-chat-platform.git ."
    exit 1
fi

# Build the application
echo "Building application..."
cd backend
mvn clean package -DskipTests

# Copy JAR to deployment directory
cp target/chat-platform-backend-1.0.0.jar /opt/chatplatform/

# Load environment variables
if [ -f "/opt/chatplatform/.env" ]; then
    source /opt/chatplatform/.env
    echo "Environment variables loaded"
else
    echo "Warning: .env file not found. Using defaults."
fi

# Restart the service
echo "Restarting service..."
sudo systemctl restart chatplatform
sudo systemctl enable chatplatform

# Check service status
echo "Checking service status..."
sudo systemctl status chatplatform --no-pager

echo "✅ Deployment completed!"
echo "Service logs: sudo journalctl -u chatplatform -f"
echo "Service status: sudo systemctl status chatplatform"
EOF

chmod +x /opt/chatplatform/deploy.sh

# Create monitoring script
print_status "Creating monitoring script..."
tee /opt/chatplatform/monitor.sh > /dev/null <<'EOF'
#!/bin/bash

echo "📊 Chat Platform Monitoring"
echo "=========================="

# System resources
echo "💻 System Resources:"
echo "CPU Usage: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')"
echo "Memory Usage: $(free -h | awk 'NR==2{printf "%.1f%%", $3*100/$2}')"
echo "Disk Usage: $(df -h / | awk 'NR==2{print $5}')"
echo ""

# Service status
echo "🔧 Service Status:"
sudo systemctl is-active chatplatform
echo ""

# Application health
echo "🏥 Application Health:"
curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || echo "Health check failed"
echo ""

# Recent logs
echo "📋 Recent Logs:"
sudo journalctl -u chatplatform -n 10 --no-pager
echo ""

# Network connections
echo "🌐 Network Connections:"
sudo netstat -tlnp | grep :8080
echo ""

echo "For real-time logs: sudo journalctl -u chatplatform -f"
EOF

chmod +x /opt/chatplatform/monitor.sh

# Create backup script
print_status "Creating backup script..."
tee /opt/chatplatform/backup.sh > /dev/null <<'EOF'
#!/bin/bash

BACKUP_DIR="/opt/chatplatform/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

echo "📦 Creating backup..."

# Backup configuration
cp /opt/chatplatform/.env $BACKUP_DIR/env_$DATE.backup 2>/dev/null || echo "No .env file found"

# Backup JAR file
cp /opt/chatplatform/chat-platform-backend-1.0.0.jar $BACKUP_DIR/app_$DATE.jar 2>/dev/null || echo "No JAR file found"

# Backup logs
sudo journalctl -u chatplatform > $BACKUP_DIR/logs_$DATE.log

echo "✅ Backup completed: $BACKUP_DIR"
echo "Files created:"
ls -la $BACKUP_DIR/*$DATE*
EOF

chmod +x /opt/chatplatform/backup.sh

# Install SSL certificate tool (optional)
print_status "Installing Certbot for SSL certificates..."
sudo apt install -y certbot

# Install Nginx for reverse proxy (optional)
print_status "Installing Nginx..."
sudo apt install -y nginx

# Create Nginx configuration template
print_status "Creating Nginx configuration template..."
sudo tee /etc/nginx/sites-available/chatplatform > /dev/null <<'EOF'
server {
    listen 80;
    server_name your-domain.com;  # Replace with your domain
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400;
    }
    
    # Health check endpoint
    location /health {
        proxy_pass http://localhost:8080/actuator/health;
    }
}
EOF

# Enable Nginx site (commented out by default)
# sudo ln -s /etc/nginx/sites-available/chatplatform /etc/nginx/sites-enabled/
# sudo nginx -t
# sudo systemctl restart nginx

print_success "Oracle Cloud VPS setup completed!"
print_warning "Next steps:"
echo "1. Configure your environment variables:"
echo "   cp /opt/chatplatform/.env.template /opt/chatplatform/.env"
echo "   nano /opt/chatplatform/.env"
echo ""
echo "2. Clone your repository:"
echo "   cd /opt/chatplatform"
echo "   git clone https://github.com/yourusername/scalable-chat-platform.git ."
echo ""
echo "3. Deploy your application:"
echo "   ./deploy.sh"
echo ""
echo "4. Monitor your application:"
echo "   ./monitor.sh"
echo ""
echo "5. (Optional) Set up SSL with custom domain:"
echo "   sudo certbot --nginx -d your-domain.com"
echo ""
echo "📊 Service Management Commands:"
echo "Start: sudo systemctl start chatplatform"
echo "Stop: sudo systemctl stop chatplatform"
echo "Restart: sudo systemctl restart chatplatform"
echo "Status: sudo systemctl status chatplatform"
echo "Logs: sudo journalctl -u chatplatform -f"
echo ""
print_success "Your Oracle Cloud VPS is ready for deployment! 🚀"
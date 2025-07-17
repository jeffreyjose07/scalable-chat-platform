#!/bin/bash

# Google Cloud e2-micro Setup Script for Spring Boot Chat Platform
# Optimized for 1GB RAM free tier instance

set -e

echo "🚀 Setting up Google Cloud e2-micro for Spring Boot Chat Platform"

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

# Check available memory
TOTAL_MEM=$(free -m | awk 'NR==2{print $2}')
print_status "Available memory: ${TOTAL_MEM}MB"

if [ "$TOTAL_MEM" -lt 900 ]; then
    print_error "Insufficient memory for Spring Boot. Need at least 900MB."
    exit 1
fi

# Update system
print_status "Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install essential packages
print_status "Installing essential packages..."
sudo apt install -y curl wget git htop unzip software-properties-common

# Install Java 17 (optimized for limited memory)
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

# Configure swap (important for 1GB RAM)
print_status "Setting up swap file for memory optimization..."
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

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

# Create systemd service file (optimized for 1GB RAM)
print_status "Creating systemd service..."
sudo tee /etc/systemd/system/chatplatform.service > /dev/null <<EOF
[Unit]
Description=Chat Platform Backend
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/chatplatform
ExecStart=/usr/bin/java -jar chat-platform-backend-1.0.0.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=chatplatform

# JVM Options optimized for 1GB RAM
Environment="JAVA_OPTS=-Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication -XX:G1HeapRegionSize=16m"
Environment="SPRING_PROFILES_ACTIVE=production"

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

# Memory optimization settings
SPRING_JPA_OPEN_IN_VIEW=false
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
SPRING_REDIS_LETTUCE_POOL_MAX_ACTIVE=3
SPRING_REDIS_LETTUCE_POOL_MAX_IDLE=2

# Logging (reduced for memory)
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
LOGGING_LEVEL_ORG_HIBERNATE=WARN
LOGGING_LEVEL_COM_CHATPLATFORM=INFO
EOF

# Create optimized application.properties for 1GB RAM
print_status "Creating memory-optimized configuration..."
tee /opt/chatplatform/application-gcp.properties > /dev/null <<EOF
# Google Cloud e2-micro optimized configuration
server.port=8080
server.tomcat.threads.max=20
server.tomcat.threads.min-spare=5
server.tomcat.accept-count=10
server.tomcat.max-connections=50

# Database connection pool (reduced for 1GB RAM)
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Redis connection pool (reduced)
spring.redis.lettuce.pool.max-active=3
spring.redis.lettuce.pool.max-idle=2
spring.redis.lettuce.pool.min-idle=0

# JPA optimizations
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# Kafka optimizations for low memory
spring.kafka.producer.batch-size=1000
spring.kafka.producer.buffer-memory=16777216
spring.kafka.consumer.max-poll-records=5

# Logging optimizations
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=WARN
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
EOF

# Create deployment script
print_status "Creating deployment script..."
tee /opt/chatplatform/deploy.sh > /dev/null <<'EOF'
#!/bin/bash

set -e

echo "🚀 Deploying Chat Platform to Google Cloud..."

# Navigate to application directory
cd /opt/chatplatform

# Pull latest code (if repository exists)
if [ -d ".git" ]; then
    echo "Pulling latest code..."
    git pull origin feature/free-forever-deployment
else
    echo "Cloning repository..."
    git clone -b feature/free-forever-deployment https://github.com/jeffreyjose07/scalable-chat-platform.git .
fi

# Build the application with memory optimizations
echo "Building application..."
cd backend
export MAVEN_OPTS="-Xmx512m"
mvn clean package -DskipTests -Dspring.profiles.active=production

# Copy JAR to deployment directory
cp target/chat-platform-backend-1.0.0.jar /opt/chatplatform/

# Copy optimized configuration
cp /opt/chatplatform/application-gcp.properties /opt/chatplatform/application.properties

# Load environment variables
if [ -f "/opt/chatplatform/.env" ]; then
    export $(cat /opt/chatplatform/.env | xargs)
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
sleep 5
sudo systemctl status chatplatform --no-pager

echo "✅ Deployment completed!"
echo "Backend URL: http://34.93.30.238:8080"
echo "Health check: curl http://34.93.30.238:8080/actuator/health"
EOF

chmod +x /opt/chatplatform/deploy.sh

# Create monitoring script
print_status "Creating monitoring script..."
tee /opt/chatplatform/monitor.sh > /dev/null <<'EOF'
#!/bin/bash

echo "📊 Chat Platform Monitoring (Google Cloud e2-micro)"
echo "================================================"

# System resources
echo "💻 System Resources:"
echo "Memory Usage: $(free -h | awk 'NR==2{printf "Used: %s/%s (%.1f%%)", $3, $2, $3*100/$2}')"
echo "Swap Usage: $(free -h | awk 'NR==3{printf "Used: %s/%s", $3, $2}')"
echo "CPU Usage: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')"
echo "Disk Usage: $(df -h / | awk 'NR==2{print $5}')"
echo ""

# Service status
echo "🔧 Service Status:"
sudo systemctl is-active chatplatform
echo ""

# Application health
echo "🏥 Application Health:"
curl -s --max-time 5 http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || echo "Health check failed or service starting"
echo ""

# JVM memory info
echo "☕ JVM Memory Info:"
if pgrep -f "chat-platform-backend" > /dev/null; then
    PID=$(pgrep -f "chat-platform-backend")
    echo "Java process memory: $(ps -p $PID -o rss= | awk '{printf "%.1f MB", $1/1024}')"
else
    echo "Java process not running"
fi
echo ""

# Recent logs
echo "📋 Recent Logs:"
sudo journalctl -u chatplatform -n 10 --no-pager
echo ""

echo "For real-time logs: sudo journalctl -u chatplatform -f"
echo "For detailed memory info: free -h && cat /proc/meminfo"
EOF

chmod +x /opt/chatplatform/monitor.sh

# Install Node.js (for potential frontend building)
print_status "Installing Node.js..."
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Configure system for low memory usage
print_status "Optimizing system for low memory usage..."
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
echo 'vm.vfs_cache_pressure=50' | sudo tee -a /etc/sysctl.conf
sudo sysctl -p

# Create health check script
print_status "Creating health check script..."
tee /opt/chatplatform/health-check.sh > /dev/null <<'EOF'
#!/bin/bash

# Simple health check for Google Cloud
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$response" = "200" ]; then
    echo "✅ Service healthy"
    exit 0
else
    echo "❌ Service unhealthy (HTTP $response)"
    exit 1
fi
EOF

chmod +x /opt/chatplatform/health-check.sh

print_success "Google Cloud e2-micro setup completed!"
print_warning "Next steps:"
echo "1. Configure your environment variables:"
echo "   cp /opt/chatplatform/.env.template /opt/chatplatform/.env"
echo "   nano /opt/chatplatform/.env"
echo ""
echo "2. Deploy your application:"
echo "   ./deploy.sh"
echo ""
echo "3. Monitor your application:"
echo "   ./monitor.sh"
echo ""
echo "4. Your backend will be available at:"
echo "   http://34.93.30.238:8080"
echo ""
echo "📊 Memory Management Tips:"
echo "- Monitor memory usage: free -h"
echo "- Check swap usage: swapon -s"
echo "- Restart if memory issues: sudo systemctl restart chatplatform"
echo ""
echo "🔧 Service Management Commands:"
echo "Start: sudo systemctl start chatplatform"
echo "Stop: sudo systemctl stop chatplatform"
echo "Restart: sudo systemctl restart chatplatform"
echo "Status: sudo systemctl status chatplatform"
echo "Logs: sudo journalctl -u chatplatform -f"
echo ""
print_success "Your Google Cloud e2-micro is ready for deployment! 🚀"
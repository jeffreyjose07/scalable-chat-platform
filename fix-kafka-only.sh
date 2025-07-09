#!/bin/bash

echo "🔧 Fixing Kafka cluster ID mismatch while preserving user data"
echo ""

# Stop services
echo "🛑 Stopping services..."
docker-compose down

# Remove only Kafka and Zookeeper volumes
echo "🧹 Removing only Kafka and Zookeeper volumes..."
docker volume rm scalable-chat-platform_kafka_data 2>/dev/null || true
docker volume rm scalable-chat-platform_zookeeper_data 2>/dev/null || true  
docker volume rm scalable-chat-platform_zookeeper_log 2>/dev/null || true

echo "✅ Kafka/Zookeeper volumes removed. PostgreSQL, MongoDB, Redis data preserved."

# Restart services
echo "🚀 Restarting services..."
./start-dev.sh
#!/bin/bash

echo "🚀 Starting Chat Platform Development Environment"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

# Check for Kafka cluster ID mismatch and fix it
echo "🔧 Checking Kafka cluster ID consistency..."
if docker-compose logs kafka 2>/dev/null | grep -q "InconsistentClusterIdException"; then
    echo "⚠️  Kafka cluster ID mismatch detected. Fixing..."
    echo "🧹 Removing only Kafka and Zookeeper volumes to preserve user data..."
    docker-compose down
    docker volume rm scalable-chat-platform_kafka_data scalable-chat-platform_zookeeper_data scalable-chat-platform_zookeeper_log 2>/dev/null || true
    echo "✅ Kafka/Zookeeper volumes cleared. User data preserved."
fi

echo "📦 Starting infrastructure services..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 15

echo "🔧 Setting up Kafka topics..."
# Wait for Kafka to be fully ready
until docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    echo "⏳ Waiting for Kafka to be ready..."
    sleep 2
done

# Create chat-messages topic if it doesn't exist
if ! docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092 | grep -q "chat-messages"; then
    echo "📝 Creating chat-messages topic..."
    docker exec scalable-chat-platform-kafka-1 kafka-topics --create --topic chat-messages --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3
    echo "✅ Topic created successfully"
else
    echo "✅ Topic chat-messages already exists"
fi

echo "🗄️ Checking service health..."
echo "PostgreSQL: $(docker-compose ps postgres --format 'table {{.State}}')"
echo "MongoDB: $(docker-compose ps mongodb --format 'table {{.State}}')"
echo "Redis: $(docker-compose ps redis --format 'table {{.State}}')"
echo "Kafka: $(docker-compose ps kafka --format 'table {{.State}}')"
echo "Elasticsearch: $(docker-compose ps elasticsearch --format 'table {{.State}}')"

echo ""
echo "✅ Infrastructure services are starting up!"
echo ""
echo "Next steps:"
echo "1. Start the backend: cd backend && mvn spring-boot:run"
echo "2. Start the frontend: cd frontend && npm install && npm start"
echo "3. Open http://localhost:3000 in your browser"
echo ""
echo "To stop services: docker-compose down"
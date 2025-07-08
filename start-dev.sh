#!/bin/bash

echo "🚀 Starting Chat Platform Development Environment"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

echo "📦 Starting infrastructure services..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 10

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
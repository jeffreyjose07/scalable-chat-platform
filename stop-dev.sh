#!/bin/bash

echo "🛑 Stopping Chat Platform Development Environment"
echo ""

# Stop Docker services
echo "📦 Stopping infrastructure services..."
docker-compose down

echo ""
echo "✅ All services stopped!"
echo ""
echo "Infrastructure services (PostgreSQL, MongoDB, Redis, Kafka, Elasticsearch) have been stopped."
echo "If you were running the backend or frontend separately, you may need to stop them manually:"
echo "- Backend: Press Ctrl+C in the terminal running 'mvn spring-boot:run'"
echo "- Frontend: Press Ctrl+C in the terminal running 'npm start'"
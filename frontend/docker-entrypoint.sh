#!/bin/sh

# Docker entrypoint script for React app with runtime environment variables

# Set default values if not provided
REACT_APP_API_BASE_URL=${REACT_APP_API_BASE_URL:-"http://localhost:8080"}
REACT_APP_WS_URL=${REACT_APP_WS_URL:-"ws://localhost:8080"}

# Create runtime config file
cat > /usr/share/nginx/html/env-config.js << EOF
window._env_ = {
  REACT_APP_API_BASE_URL: "${REACT_APP_API_BASE_URL}",
  REACT_APP_WS_URL: "${REACT_APP_WS_URL}"
};
EOF

echo "Frontend configuration:"
echo "API Base URL: ${REACT_APP_API_BASE_URL}"
echo "WebSocket URL: ${REACT_APP_WS_URL}"

# Replace placeholders in built files if they exist
find /usr/share/nginx/html -type f -name "*.js" -exec sed -i "s|REACT_APP_API_BASE_URL_PLACEHOLDER|${REACT_APP_API_BASE_URL}|g" {} \;
find /usr/share/nginx/html -type f -name "*.js" -exec sed -i "s|REACT_APP_WS_URL_PLACEHOLDER|${REACT_APP_WS_URL}|g" {} \;

# Execute the main container command
exec "$@"
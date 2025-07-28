#!/bin/bash

# JWT Secret Generation Script
# This script generates a cryptographically secure JWT secret

echo "🔑 Generating JWT Secret..."
echo "=========================="

# Generate a 256-bit (32 bytes) random secret
JWT_SECRET=$(openssl rand -base64 32)

echo "Your new JWT secret:"
echo "JWT_SECRET=$JWT_SECRET"
echo ""

# Save to .env file for local development
if [ -f ".env" ]; then
    # Update existing .env file
    if grep -q "JWT_SECRET=" .env; then
        # Replace existing JWT_SECRET
        sed -i.bak "s/^JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" .env
        echo "✅ Updated JWT_SECRET in existing .env file"
    else
        # Add JWT_SECRET to .env
        echo "JWT_SECRET=$JWT_SECRET" >> .env
        echo "✅ Added JWT_SECRET to .env file"
    fi
else
    # Create new .env file
    echo "JWT_SECRET=$JWT_SECRET" > .env
    echo "✅ Created new .env file with JWT_SECRET"
fi

echo ""
echo "🚨 IMPORTANT SECURITY NOTES:"
echo "1. Keep this secret confidential - never commit it to version control"
echo "2. Use different secrets for different environments (dev/staging/prod)"
echo "3. For production, set this as an environment variable on your server"
echo "4. Rotate this secret periodically (every 3-6 months)"
echo ""
echo "📋 Production deployment commands:"
echo "   Docker: -e JWT_SECRET=$JWT_SECRET"
echo "   Kubernetes: kubectl create secret generic jwt-secret --from-literal=JWT_SECRET=$JWT_SECRET"
echo "   Heroku: heroku config:set JWT_SECRET=$JWT_SECRET"
echo "   AWS: aws ssm put-parameter --name /app/jwt-secret --value $JWT_SECRET --type SecureString"
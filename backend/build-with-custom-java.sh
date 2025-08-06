#!/bin/bash

# Script to build with specific Java installation using Gradle
# Uses locally installed Java 17 for Gradle builds

# Store current JAVA_HOME and PATH
ORIGINAL_JAVA_HOME="$JAVA_HOME"
ORIGINAL_PATH="$PATH"

# Java 17 installation for Gradle builds
JAVA17_HOME="/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home"

# Function to restore original Java settings
restore_java() {
    echo "Restoring original Java environment..."
    export JAVA_HOME="$ORIGINAL_JAVA_HOME"
    export PATH="$ORIGINAL_PATH"
    echo "Java restored to: $(java -version 2>&1 | head -1)"
}

# Set trap to ensure we always restore Java, even if script fails
trap restore_java EXIT

echo "Current Java: $(java -version 2>&1 | head -1)"
echo "Switching to Java 17 for Gradle build..."

# Set Java 17 for the build
export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA17_HOME/bin:$PATH"

echo "Java switched to: $(java -version 2>&1 | head -1)"

echo "Running Gradle build..."
./gradlew clean build --no-daemon

echo "Gradle build completed with exit code: $?"

# Optional: Build for Render deployment
if [ "$1" = "--render" ]; then
    echo "Building for Render deployment..."
    ./gradlew buildForRender --no-daemon
    echo "Render build completed with exit code: $?"
fi
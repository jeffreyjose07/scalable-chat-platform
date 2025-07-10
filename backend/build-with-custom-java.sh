#!/bin/bash

# Script to build with specific Java installation that has certificates
# Uses locally installed Java 8 Corretto 1.8.0_432 with certificates

# Store current JAVA_HOME and PATH
ORIGINAL_JAVA_HOME="$JAVA_HOME"
ORIGINAL_PATH="$PATH"

# Your specific Java installation with certificates
JAVA_WITH_CERTS="/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-1.8.0_432/Contents/Home"
JAVA17_HOME="/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.10/Contents/Home"

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
echo "Switching to Java 17 for build..."

# Set Java 17 for the build
export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA17_HOME/bin:$PATH"

echo "Java switched to: $(java -version 2>&1 | head -1)"

echo "Creating test settings to override corporate Maven settings..."
cat > test-settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>central</id>
      <mirrorOf>*</mirrorOf>
      <url>https://repo1.maven.org/maven2</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>default</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>default</activeProfile>
  </activeProfiles>
</settings>
EOF

echo "Running Maven clean install with test settings..."
mvn clean install -s test-settings.xml

# Clean up
rm -f test-settings.xml

echo "Maven build completed with exit code: $?"
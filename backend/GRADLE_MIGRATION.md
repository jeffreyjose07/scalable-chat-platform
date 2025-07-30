# Maven to Gradle Migration Guide

## Migration Summary

Successfully migrated from Maven to Gradle to avoid Maven security profile issues while preserving all functionality.

## Prerequisites

- **Java 17** or higher is required
- Set JAVA_HOME to Java 17: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk` (Linux) or use `java_home` utility on macOS

## Gradle Build Commands

### Basic Commands
```bash
# Build the entire application (backend + frontend)
./gradlew build

# Build for Render deployment
./gradlew buildForRender

# Run the application (development)
./gradlew bootRun

# Run with specific profile
./gradlew bootRunDev
./gradlew bootRunProd

# Clean build
./gradlew clean

# Compile Java only
./gradlew compileJava

# Run tests
./gradlew test

# Run integration tests
./gradlew integrationTest
```

### Frontend-Specific Commands
```bash
# Install frontend dependencies only
./gradlew frontendInstall

# Build frontend only
./gradlew frontendBuild

# Copy frontend build to static resources
./gradlew copyFrontendBuild
```

### Development Commands
```bash
# Quick development build (no frontend)
./gradlew devBuild

# Show build information
./gradlew buildInfo

# Run security scan
./gradlew securityScan
```

## Key Features Preserved

1. **All Spring Boot Dependencies**: Web, WebSocket, Security, Data JPA, MongoDB, Redis
2. **JWT Authentication**: Complete JWT implementation with all JJWT libraries
3. **Database Support**: PostgreSQL driver and H2 for testing
4. **Monitoring**: Spring Boot Actuator with Prometheus metrics
5. **Testing**: TestContainers for PostgreSQL and MongoDB
6. **Frontend Integration**: Automatic React build and integration
7. **Security**: All security configurations and rate limiting

## Build Structure

```
backend/
├── build.gradle                 # Main build configuration
├── settings.gradle              # Project settings
├── gradle.properties           # Build properties
├── gradlew                     # Gradle wrapper (Unix)
├── gradlew.bat                 # Gradle wrapper (Windows)
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar   # Gradle wrapper JAR
        └── gradle-wrapper.properties
```

## Frontend Integration

The build automatically:
1. Installs Node.js and npm
2. Runs `npm install` in the frontend directory
3. Builds the React application with `npm run build`
4. Copies the build output to `src/main/resources/static`

## Environment Configuration

### gradle.properties
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
java.version=17
```

## Migration Benefits

1. **No Maven Security Issues**: Avoids Maven security profile blockers
2. **Better Performance**: Gradle daemon and parallel execution
3. **Industry Standard**: Modern build tool with better dependency management
4. **Flexible Configuration**: Easier to customize and extend
5. **Better IDE Support**: Enhanced IDE integration and incremental builds

## Common Issues & Solutions

### Java Version Issues
```bash
# Check Java version
java -version

# Set Java 17 (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Set Java 17 (Linux)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### Permission Issues
```bash
# Make gradlew executable
chmod +x gradlew
```

### Clean Build
```bash
# If facing caching issues
./gradlew clean build --refresh-dependencies
```

## Production Deployment

For Render deployment, use:
```bash
./gradlew buildForRender
```

This creates an optimized JAR with the frontend embedded at:
`build/libs/chat-platform-backend-1.0.0.jar`
# Multi-stage build for Spring Boot application with Gradle
FROM eclipse-temurin:17-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files first to leverage Docker cache
COPY gradle/ ./gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN ./gradlew bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

# Install curl for health checks (do this as root)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -g 1001 appuser && \
    useradd -r -u 1001 -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=5 \
  CMD curl -f http://localhost:8080/api/health/status || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
# =============================================================================
# Stage 1: Build Stage (JDK + Maven)
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Cache dependencies: Copy only pom.xml first
COPY pom.xml .

# Download dependencies in an isolated layer (cached unless pom.xml changes!)
RUN mvn dependency:go-offline -B

# 2. Copy source code and build the application JAR
COPY src ./src
RUN mvn clean package -DskipTests

# =============================================================================
# Stage 2: Production Runtime Stage (Minimal JRE, non-root user)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a secure, non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the compiled JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Give appuser ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user for security
USER appuser

# Expose the application port
EXPOSE 8081

# JVM flags optimized for containerized environments:
# - MaxRAMPercentage=75: ensures JVM respects container cgroups memory limits
# - Xmx256m: caps heap memory to keep laptop fast with multiple services
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Xmx256m -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

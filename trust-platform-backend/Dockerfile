# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create receipts directory with proper permissions
RUN mkdir -p /app/receipts

# Non-root user for security
RUN addgroup -S trustapp && adduser -S trustapp -G trustapp
RUN chown -R trustapp:trustapp /app
USER trustapp

# Copy built JAR from builder stage
COPY --from=builder --chown=trustapp:trustapp /app/target/*.jar app.jar

# Health check using the public health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/public/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-jar", \
  "-Dspring.profiles.active=prod", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "/app/app.jar"]

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first (layer cache — only re-downloads deps when pom changes)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -q

# Copy source and build, skipping tests (tests run in CI, not in Docker build)
COPY src src
RUN ./mvnw package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S eventflow && adduser -S eventflow -G eventflow

WORKDIR /app

# Copy only the fat jar from the builder stage
COPY --from=builder /build/target/eventflow-*.jar app.jar

# Switch to non-root user
USER eventflow

# Expose the Spring Boot port
EXPOSE 8080

# JVM tuning for containers:
#   -XX:+UseContainerSupport        honours Docker memory limits
#   -XX:MaxRAMPercentage=75.0       use up to 75% of container RAM for heap
#   -Djava.security.egd=...         faster startup (avoids /dev/random blocking)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
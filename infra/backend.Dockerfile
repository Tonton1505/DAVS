# ============================================================================
# Stage 1: Build Spring Boot application with Maven & JDK 21
# ============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Cache Maven dependencies first
COPY backend-springboot/pom.xml backend-springboot/checkstyle.xml ./
RUN mvn -B -q dependency:go-offline

# Copy source code and package executable JAR
COPY backend-springboot/src ./src
RUN mvn -B -q clean package -DskipTests

# ============================================================================
# Stage 2: Minimal JRE 21 runtime running as non-root user
# ============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install curl for container healthcheck and create non-root user/group
RUN apk add --no-cache curl \
    && addgroup -g 10001 -S davs \
    && adduser -u 10001 -S -G davs -H -D davs \
    && chown -R davs:davs /app

COPY --from=build --chown=davs:davs /workspace/target/*.jar /app/app.jar

USER davs

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

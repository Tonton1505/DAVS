# Stage 1: Build JAR bằng Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production JRE Alpine siêu nhẹ
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app
RUN addgroup -S davsgroup && adduser -S davsuser -G davsgroup
COPY --from=builder /build/target/*.jar app.jar
USER davsuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

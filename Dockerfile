# --- Stage 1: Build the application ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy only the pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Create the final lightweight image ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Standard Spring Boot environment variables (can be overridden)
ENV APP_SYNC_FREQUENCY_MS=300000 \
    GLUETUN_URL=http://gluetun:8000 \
    QBIT_URL=http://qbittorrent:8081

ENTRYPOINT ["java", "-jar", "app.jar"]
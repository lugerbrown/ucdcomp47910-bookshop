# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
LABEL authors="lmarron"
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim

# Create non-root user for security (CWE-250 mitigation)
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY docker/mysql-ssl/truststore.jks /app/truststore.jks

# Set ownership to non-root user
RUN chown -R app:app /app

# Switch to non-root user
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
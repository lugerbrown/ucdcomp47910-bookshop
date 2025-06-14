# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
LABEL authors="lmarron"
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY docker/mysql-ssl/truststore.jks /app/truststore.jks
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
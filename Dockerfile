# syntax=docker/dockerfile:1

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY backend/pom.xml .
RUN mvn -q dependency:go-offline
COPY backend/src ./src
RUN mvn -q package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app
COPY --from=build /src/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
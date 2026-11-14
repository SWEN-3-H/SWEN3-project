FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /src

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /src/target/ocr-worker-*.jar app.jar

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
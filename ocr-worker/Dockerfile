FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /src

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
        ghostscript \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-deu \
    && rm -rf /var/lib/apt/lists/*

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /src/target/ocr-worker-*.jar app.jar

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
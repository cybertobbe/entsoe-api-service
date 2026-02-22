FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/entsoe-api-service-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
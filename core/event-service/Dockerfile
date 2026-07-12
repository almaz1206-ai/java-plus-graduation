FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/event-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

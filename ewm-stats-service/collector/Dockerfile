FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/collector-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

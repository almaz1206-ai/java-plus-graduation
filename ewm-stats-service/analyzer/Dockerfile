FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/analyzer-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

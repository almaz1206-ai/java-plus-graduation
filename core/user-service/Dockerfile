FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/user-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

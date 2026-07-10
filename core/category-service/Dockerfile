FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/category-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

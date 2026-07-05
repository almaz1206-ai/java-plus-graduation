FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/gateway-server-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

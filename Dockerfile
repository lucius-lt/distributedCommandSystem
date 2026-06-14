FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# The Spring Boot Maven plugin builds the jar in the target directory
# with the format artifactId-version.jar
COPY target/c2-0.0.1-SNAPSHOT.jar app.jar

# Explicitly set server to bind to all interfaces (although Spring Boot does this by default mostly)
ENV SERVER_PORT=8080
ENV SERVER_ADDRESS=0.0.0.0

# Expose the correct port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

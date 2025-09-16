# Stage 1: Build with Maven and JDK 17
FROM maven:3.9.8 AS build-stage
LABEL name=tf-order-service

WORKDIR /app

# Copy only the pom.xml first to leverage Docker layer caching
COPY pom.xml .
RUN mvn -Dmaven.repo.local=/app/.m3/repository dependency:go-offline

# Copy the rest of the application source code and build
COPY src ./src
RUN mvn -Dmaven.repo.local=/app/.m3/repository package -DskipTests=true

# Stage 2: Run with OpenJDK 17
FROM openjdk:17-jdk-slim AS production-stage
WORKDIR /app

# Install additional packages if needed
RUN apt-get update && \
    apt-get install -y curl telnet nano tcpdump less bash && \
    rm -rf /var/lib/apt/lists/*

# Expose the port your app runs on
EXPOSE 8083

# Copy the compiled artifact from the build stage
COPY --from=build-stage /app/target/*.jar order-service.jar
COPY src/main/resources/application.properties /app/application.properties

# Set the timezone and other JVM options
ENV TZ=GMT
ENV JAVA_OPTS="-Djk.internal.httpclient.disableHostnameVertification=true"

# Command to run the application
ENTRYPOINT ["java", "-jar", "order-service.jar", "--spring.config.location=file:/app/application.properties", "--spring.profiles.active=default", "--server.port=8083"]
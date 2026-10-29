# === STAGE 1: Build the Java Application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the build configuration files and source code
COPY pom.xml .
COPY src ./src

# Compile the application and pack it into a execution .jar file
RUN mvn clean package -DskipTests

# === STAGE 2: Create the Lightweight Runtime Image ===
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the compiled jar from the build stage into our slim runtime container
COPY --from=build /app/target/*.jar app.jar

# Inform Docker that the container will listen on port 8080 at runtime
EXPOSE 8080

# The execution command to launch the Spring Boot app when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]

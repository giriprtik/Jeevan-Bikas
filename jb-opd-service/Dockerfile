# =========================================================================
# Stage 1: Build the application artifact using Maven and compile Lombok
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy only the pom.xml file first to cache dependency layers cleanly
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the actual application source code files
COPY src ./src

# Compile the source code and assemble the executable fat JAR file
# This layer executes annotation processing for Lombok variables automatically
RUN mvn clean package -DskipTests

# =========================================================================
# Stage 2: Create a minimal, lightweight production runtime container image
# =========================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-privileged system user account for enhanced application security
RUN useradd -ms /bin/bash opduser
USER opduser

# Copy the compiled JAR artifact from the builder phase stage workspace
COPY --from=builder /build/target/*.jar app.jar

# Expose the dedicated port used by this microservice
EXPOSE 8082

# Configure JVM flags for efficient resource mapping inside cloud spaces
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]

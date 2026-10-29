# =========================================================================
# STAGE 1: The Build Environment (FIXED: Highly compatible multi-platform build layer)
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# =========================================================================
# STAGE 2: The Runtime Environment (Ubuntu Jammy Jellyfish JRE Base)
# =========================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Uses Ubuntu-compatible Debian syntax to provision a secure, non-root system account
RUN groupadd --system hospitalgroup && \
    useradd --system --gid hospitalgroup --no-create-home --shell /bin/false hospitaluser

# Assign execution context boundaries to the secure user
USER hospitaluser

# Copy the compiled JAR binary file cleanly out of the first builder stage container
COPY --from=builder /app/target/jb-gateway-0.0.1-SNAPSHOT.jar app.jar

# Inform the Docker engine that this container listens on the standard gateway port
EXPOSE 8080

# Configure low-overhead JVM parameters optimized for containerized memory footprints
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Xms256m -Xmx512m"

# Execute the Spring Cloud Gateway bootstrap lifecycle
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

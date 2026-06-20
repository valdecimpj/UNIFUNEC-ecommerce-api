# Stage 1: Build the Eureka Server JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom first to cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Run with a minimal JRE image
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build the project
FROM maven:3.9-amazoncorretto-23-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -B -DskipTests

# Stage 2: Run the project
FROM amazoncorretto:23-alpine AS runner
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
EXPOSE 8080

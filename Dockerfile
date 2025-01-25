## Stage 1: Fetch dependencies
#FROM maven:3.9-amazoncorretto-23-alpine AS deps
#WORKDIR /app
#COPY market/pom.xml market/pom.xml
#COPY trader/pom.xml trader/pom.xml
#COPY pom.xml .
#RUN mvn -B -e org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=trader
#
## Stage 1: Build the project
#FROM maven:3.9-amazoncorretto-23-alpine AS builder
#WORKDIR /app
#COPY --from=deps /root/.m2 /root/.m2
#COPY --from=deps /app /app
#COPY market/src /app/market/src
#COPY trader/src /app/trader/src
#RUN mvn -B -e clean install -DskipTests
#
## Stage 2: Run the project
#FROM amazoncorretto:23-alpine
#WORKDIR /app
#COPY --from=builder /app/target/market-1.0-SNAPSHOT.jar market.jar
#CMD ["java", "-jar", "market.jar"]
#EXPOSE 8080

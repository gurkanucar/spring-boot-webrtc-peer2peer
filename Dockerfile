## Build stage
FROM maven:3.8.4-jdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ /app/src/
RUN mvn clean package -DskipTests

# Step : Package image
FROM openjdk:11-jdk-slim
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080 8000
ENTRYPOINT ["java", "-jar" , "app.jar"]
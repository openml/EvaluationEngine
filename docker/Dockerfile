FROM maven:3.9.11-amazoncorretto-11-alpine AS build
COPY src /app/src
COPY pom.xml /app
RUN mvn -f /app/pom.xml clean package -DskipTests

FROM amazoncorretto:11-alpine3.22

COPY --from=build /app/target/EvaluationEngine-*-jar-with-dependencies.jar /usr/local/lib/evaluation-engine.jar
ENTRYPOINT ["java", "-jar", "/usr/local/lib/evaluation-engine.jar"]

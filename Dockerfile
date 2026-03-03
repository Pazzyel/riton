FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/riton-1.0.0.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8045

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]

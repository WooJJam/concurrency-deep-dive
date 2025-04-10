FROM bellsoft/liberica-openjdk-alpine:17
CMD ["./gradlew", "clean", "build"]

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ARG PROFILE=test
ENV PROFILE=${PROFILE}

ENTRYPOINT ["java", "-jar", "/app.jar","--spring.profiles.active=${PROFILE}"]

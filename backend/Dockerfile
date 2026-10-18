FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S drivecare && adduser -S drivecare -G drivecare
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER drivecare
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

FROM gradle:8.11-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/discord-ticketbot.jar ./app.jar
VOLUME /app/Tickets
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM gradle:8.10-jdk21 AS build

WORKDIR /app
COPY app .

RUN chmod +x gradlew
RUN ./gradlew clean build installDist

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/install/app /app

EXPOSE 7070
CMD ["/app/bin/app"]
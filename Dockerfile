FROM gradle:8.10-jdk21 AS build

WORKDIR /app
COPY app .

RUN chmod +x gradlew
RUN ./gradlew shadowJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/app.jar /app/app.jar

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]

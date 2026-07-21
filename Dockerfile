FROM gradle:8.10-jdk21 AS build

WORKDIR /app
COPY app .

RUN chmod +x gradlew
RUN ./gradlew shadowJar

FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем JAR
COPY --from=build /app/build/libs/app.jar /app/app.jar

# Копируем шаблоны отдельно
COPY --from=build /app/src/main/resources/templates /app/src/main/resources/templates

EXPOSE 7070
CMD ["java", "-jar", "app.jar"]

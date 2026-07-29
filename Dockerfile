FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Копируем Gradle-файлы
COPY app/gradle gradle
COPY app/build.gradle.kts .
COPY app/settings.gradle.kts .
COPY app/gradlew .

# Даём права на выполнение
RUN chmod +x gradlew

# Скачиваем зависимости
RUN ./gradlew --no-daemon dependencies

# Копируем исходный код и ресурсы
COPY app/src src
COPY app/config config

# Собираем проект
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/app.jar /app/app.jar

# ✅ Отключаем компиляцию JTE на лету через системное свойство
ENV JAVA_OPTS="-Djte.compileMode=OFF"
EXPOSE 7070

# ✅ Используем sh -c для передачи JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

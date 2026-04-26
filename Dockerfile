FROM gradle:8.10-jdk21

WORKDIR /app

# Копируем всё содержимое папки app
COPY app .

# Даем права на выполнение
RUN chmod +x gradlew

# Сборка через wrapper
RUN ./gradlew clean build

EXPOSE 7070

CMD ["./gradlew", "run"]

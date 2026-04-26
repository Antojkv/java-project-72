FROM gradle:8.10-jdk21

WORKDIR /app

# Копируем ВСЁ, включая config/checkstyle/
COPY app /app

WORKDIR /app

RUN chmod +x gradlew
RUN ./gradlew clean build

EXPOSE 7070

CMD ["./gradlew", "run"]

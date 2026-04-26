FROM gradle:8.10-jdk21

WORKDIR /app

# Копируем всё
COPY app .

# Используем системный gradle, а не wrapper
RUN gradle clean build

EXPOSE 7070

CMD ["gradle", "run"]

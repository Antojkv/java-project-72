FROM gradle:8.10-jdk21

WORKDIR /app

COPY app/ app

RUN chmod +x /app/gradlew
RUN /app/gradlew clean build

EXPOSE 7070

CMD ["./gradlew", "run"]

FROM gradle:8.10-jdk21

WORKDIR /app

COPY app/ .

RUN chmod +x gradlew
RUN ./gradlew clean build

EXPOSE 7070

CMD ["./gradlew", "run"]

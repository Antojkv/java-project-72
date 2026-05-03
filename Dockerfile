FROM gradle:8.10-jdk21

WORKDIR /app

COPY app .

RUN gradle clean build

EXPOSE 7070

CMD ["gradle", "run"]

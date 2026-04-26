FROM gradle:8.10-jdk21

WORKDIR /app

COPY app/src ./src
COPY app/build.gradle.kts .
COPY app/settings.gradle.kts .

RUN gradle clean build

EXPOSE 7070

CMD ["gradle", "run"]

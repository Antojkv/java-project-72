# Образ для деплоя на render. Править его не нужно: сборка зовёт `make build`,
# запуск зовёт `make render-start`, и всё, что меняется по шагам, живёт в app/Makefile.
# Запуску нужен JDK, а не JRE: Jte компилирует шаблоны прямо в работающем приложении.
FROM eclipse-temurin:21-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends make \
    && rm -rf /var/lib/apt/lists/*

# Node.js нужен сборке стилей, Tailwind требует версию не ниже 20.
COPY --from=node:24-slim /usr/local/ /usr/local/

WORKDIR /project

COPY . .
RUN make -C app build

CMD ["make", "-C", "app", "render-start"]

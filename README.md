# Анализатор страниц (java)

[![JavaCI](https://github.com/Antojkv/java-project-72/actions/workflows/build.yml/badge.svg)](https://github.com/Antojkv/java-project-72/actions/workflows/build.yml)
[![Actions Status](https://github.com/Antojkv/java-project-72/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/Antojkv/java-project-72/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Antojkv_java-project-72&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Antojkv_java-project-72)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Antojkv_java-project-72&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Antojkv_java-project-72)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Antojkv_java-project-72&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Antojkv_java-project-72)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=Antojkv_java-project-72&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=Antojkv_java-project-72)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Antojkv_java-project-72&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Antojkv_java-project-72)

## 📖 О проекте

**Анализатор страниц** — это веб-приложение для SEO-аудита сайтов. Сервис позволяет:

- 🔗 **Добавлять URL** для последующего анализа
- ✅ **Проверять доступность** сайта и получать HTTP-статус ответа
- 📊 **Анализировать мета-данные** страницы (заголовки H1, title, description)
- 📅 **Отслеживать историю проверок** каждого сайта
- 📈 **Визуализировать результаты** проверок в удобной таблице

Проект разработан в рамках обучения на [Хекслет](https://hexlet.io) и демонстрирует навыки работы с:
- Java и Javalin Framework
- Базами данных (PostgreSQL/H2)
- Шаблонизатором JTE
- Docker и CI/CD

## 🚀 Деплой

Приложение доступно по ссылке: 
👉 **[https://java-project-72-fivd.onrender.com](https://java-project-72-fivd.onrender.com)**

## 🛠️ Локальный запуск

### Требования
- Java 21 или выше
- Gradle 8.10 или выше
- PostgreSQL (опционально, для production режима)

### Установка и запуск

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/Antojkv/java-project-72.git
   cd java-project-72

2. **Перейдите в директорию приложения:**
   ```bash
   cd app

3. **Запустите сборку:**
   ```bash
   ./gradlew build

4. **Запустите приложение:**
   ```bash
   ./gradlew run

5. **Откройте в браузере:**
   http://localhost:7070

# ⚡ Быстрый старт

## Требования
- Java 21 (JDK)
- Docker & Docker Compose
- Windows PowerShell (если используете Windows)

## Шаги

### 1️⃣ Соберите приложение на хосте

**Windows (PowerShell):**
```powershell
.\build-docker-image.ps1
```

**Linux/Mac (Bash):**
```bash
chmod +x build-docker-image.sh
./build-docker-image.sh
```

**Или вручную:**
```bash
# Собрать JAR
./gradlew clean build -x test

# Создать Docker образ
docker build -t orders-app:latest -f docker/Dockerfile.runtime .
```

### 2️⃣ Запустите все контейнеры

```bash
docker-compose up -d
```

### 3️⃣ Проверьте статус

```bash
docker-compose ps
```

Все контейнеры должны быть в статусе `Up` и `healthy` ✅

### 4️⃣ Проверьте логи

```bash
docker-compose logs -f
```

## 🌐 Доступные сервисы

| Сервис | URL | Учетные данные |
|--------|-----|----------------|
| Spring Boot API | http://localhost:8080 | — |
| Redis-UI | http://localhost:8001 | — |
| Kafka-UI | http://localhost:8002 | — |

## 🔑 Подключение к сервисам

### Redis CLI
```bash
docker exec -it redis redis-cli -a redis_password
```

### PostgreSQL
```bash
docker exec -it postgres psql -U postgres_user -d orders_db
```

### Kafka (внутри контейнера)
```bash
docker exec -it kafka bash
kafka-broker-api-versions --bootstrap-server kafka:9092
```

## 🛑 Остановка

```bash
# Остановить контейнеры
docker-compose down

# Удалить контейнеры и данные
docker-compose down -v
```

## 📚 Дополнительно

Для полной документации смотрите:
- `.ai/README.md` - Полное резюме и конфигурация
- `.ai/00_generate_container_structure.md` - Подробная документация задачи
- `DOCKER_BUILD_GUIDE.md` - Расширенное руководство

## 🚀 Готово!

Система обработки заказов готова к использованию! 🎉


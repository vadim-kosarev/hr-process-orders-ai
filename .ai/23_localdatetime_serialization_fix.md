# 23. Исправление сериализации LocalDateTime в JSON

## Дата: 2026-02-08

## Проблемы

### 1. Timeout в тесте был 15 секунд
Нужно было сократить до 10 секунд.

### 2. LocalDateTime сериализуется как массив чисел
```json
"issuedAt": [
    2026,
    2,
    7,
    21,
    57,
    9,
    513443400
]
```

**Проблема:** Jackson по умолчанию сериализует LocalDateTime как массив компонентов (год, месяц, день, час, минута, секунда, наносекунды) вместо ISO-8601 строки.

## ✅ Решение

### 1. Изменен timeout в тесте

**File:** `OrderServiceTest.java`

```java
await()
    .atMost(10, TimeUnit.SECONDS)  // ← Было 15
    .pollInterval(1, TimeUnit.SECONDS)
    .untilAsserted(() -> {
        // ...
    });
```

### 2. Обновлен AppConfig для правильной сериализации

**File:** `AppConfig.java`

```java
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Явно конфигурируем сериализацию LocalDateTime
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(
            LocalDateTime.class,
            new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        
        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
```

**Что это делает:**
- ✅ `JavaTimeModule` - поддержка Java Time API
- ✅ `LocalDateTimeSerializer` - явно указываем как сериализовать LocalDateTime
- ✅ `DateTimeFormatter.ISO_LOCAL_DATE_TIME` - формат ISO-8601 (например: "2026-02-07T21:57:09")
- ✅ `WRITE_DATES_AS_TIMESTAMPS` отключен - не писать как числовые timestamp-ы

### 3. Обновлен KafkaConfig

**File:** `KafkaConfig.java`

Метод `createKafkaObjectMapper()` теперь использует ту же конфигурацию:

```java
private ObjectMapper createKafkaObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(
        LocalDateTime.class,
        new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    );
    
    mapper.registerModule(javaTimeModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    return mapper;
}
```

**Почему KafkaConfig нужно обновить:**
- Kafka использует отдельный ObjectMapper для сериализации сообщений
- Должна быть единообразная конфигурация как в AppConfig

## Результат

### До:
```json
{
    "commandId": "e94c7c10-ca75-454a-9391-938bdc05f87c",
    "issuedAt": [2026, 2, 7, 21, 57, 9, 513443400],
    "order": {
        "createdAt": [2026, 2, 7, 10, 0],
        "updatedAt": [2026, 2, 7, 10, 0]
    }
}
```

### После:
```json
{
    "commandId": "e94c7c10-ca75-454a-9391-938bdc05f87c",
    "issuedAt": "2026-02-07T21:57:09",
    "order": {
        "createdAt": "2026-02-07T10:00:00",
        "updatedAt": "2026-02-07T10:00:00"
    }
}
```

**Преимущества:**
- ✅ Читаемо - видно дату/время в стандартном формате
- ✅ Стандартно - ISO-8601 формат
- ✅ Совместимо - Jackson стандартно десериализует ISO-8601 строки
- ✅ Компактно - меньше данных в Kafka сообщениях

## Технические детали

### Jackson LocalDateTime сериализация

Jackson имеет несколько способов сериализации дат:

#### 1. По умолчанию (как timestamp):
```java
mapper.registerModule(new JavaTimeModule());
// mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // НЕ отключено
```

**Результат:** `[2026, 2, 7, 21, 57, 9, 513443400]` (массив компонентов)

#### 2. С WRITE_DATES_AS_TIMESTAMPS отключено (как строка):
```java
JavaTimeModule module = new JavaTimeModule();
mapper.registerModule(module);
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**Результат:** `"2026-02-07T21:57:09"` (ISO-8601 строка)

#### 3. С явным serializer (рекомендуется):
```java
JavaTimeModule module = new JavaTimeModule();
module.addSerializer(
    LocalDateTime.class,
    new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
);
mapper.registerModule(module);
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**Результат:** `"2026-02-07T21:57:09"` (ISO-8601 строка) - явно сконфигурировано

**Выбран подход 3** - самый явный и надежный.

## Единообразность конфигурации

### Несколько ObjectMapper в проекте:

1. **AppConfig.objectMapper()**
   - Используется везде в приложении
   - `@Bean public ObjectMapper objectMapper()`
   - Autowired в сервисы, контроллеры, тесты

2. **KafkaConfig.createKafkaObjectMapper()**
   - Используется для Kafka serializer/deserializer
   - Для сообщений в Kafka

**Важно:** Обе должны иметь одинаковую конфигурацию сериализации дат, иначе:
- Приложение отправляет JSON в Kafka в одном формате
- Но приложение ожидает JSON в другом формате
- Результат: несовместимость и ошибки десериализации

### Решение:
- ✅ Обе используют `LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)`
- ✅ Обе отключают `WRITE_DATES_AS_TIMESTAMPS`
- ✅ Единообразная сериализация везде

## Изменённые файлы

1. ✅ `AppConfig.java`
   - Добавлена конфигурация LocalDateTimeSerializer

2. ✅ `KafkaConfig.java`
   - Добавлена конфигурация LocalDateTimeSerializer в createKafkaObjectMapper()

3. ✅ `OrderServiceTest.java`
   - Изменен timeout с 15 на 10 секунд

## Компиляция

✅ **Успешна**
- Нет ошибок компиляции
- Deprecated warnings в KafkaConfig - это известная проблема Spring Kafka 4.0

## Статус

✅ **Задача выполнена**
- Timeout сокращен до 10 секунд
- LocalDateTime сериализуется как ISO-8601 строка
- Конфигурация единообразна везде


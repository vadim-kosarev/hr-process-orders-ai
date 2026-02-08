# 25. LocalDateTime Kafka Serialization Fix
## Дата: 2026-02-08

## Проблема
LocalDateTime в Kafka сообщениях сериализовался как массив интов вместо ISO-8601 строки:
```json
{
    "issuedAt": [2026, 2, 7, 21, 57, 9, 513443400],  // ❌ Массив
    "createdAt": [2026, 2, 7, 10, 0],
    "updatedAt": [2026, 2, 7, 10, 0]
}
```

## Причина
Spring Kafka использует встроенный JsonSerializer, который не использовал настроенный в AppConfig ObjectMapper с LocalDateTimeSerializer.

## ✅ Решение

### 1. Обновлен AppConfig.java

Добавлены Kafka factories с custom ObjectMapper:

```java
@Bean
public ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper) {
    // ...
    DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
    factory.setValueSerializer(new JsonSerializer<>(objectMapper));  // ← Важно!
    return factory;
}

@Bean
public ConsumerFactory<String, Object> consumerFactory(ObjectMapper objectMapper) {
    // ...
    JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, objectMapper);
    jsonDeserializer.setUseTypeHeaders(false);
    factory.setValueDeserializer(jsonDeserializer);
    return factory;
}
```

**Ключевые точки:**
- ProducerFactory передает ObjectMapper в JsonSerializer
- ConsumerFactory передает ObjectMapper в JsonDeserializer
- Оба используют тот же ObjectMapper, что и остаток приложения

### 2. ObjectMapper конфигурация

ObjectMapper в AppConfig уже правильно настроен:

```java
@Bean
public ObjectMapper objectMapper() {
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

### 3. KafkaProducerConfig обновлен

Теперь использует ProducerFactory из AppConfig:

```java
@Bean
public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
    return new KafkaTemplate<>(producerFactory);
}
```

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

## Изменённые файлы

1. ✅ **AppConfig.java**
   - Добавлены ProducerFactory<String, Object> и ConsumerFactory<String, Object>
   - Оба beans используют custom ObjectMapper с LocalDateTimeSerializer

2. ✅ **KafkaProducerConfig.java**
   - Обновлен для использования ProducerFactory из AppConfig

## Как это работает

1. **Отправка сообщения:**
   ```
   CreateOrderCommand
   ↓ (JsonSerializer с ObjectMapper)
   JSON с ISO-8601 датами
   ↓ (KafkaTemplate.send())
   Kafka топик "order-commands"
   ```

2. **Получение сообщения:**
   ```
   Kafka топик "order-commands"
   ↓ (JsonDeserializer с ObjectMapper)
   JSON с ISO-8601 датами парсится
   ↓ (ObjectMapper.readValue())
   CreateOrderCommand с LocalDateTime
   ```

## Dependency Injection

Spring автоматически связывает:
1. `AppConfig.objectMapper()` → ProducerFactory
2. `AppConfig.objectMapper()` → ConsumerFactory
3. `ProducerFactory` → KafkaTemplate (из KafkaProducerConfig)
4. `ConsumerFactory` → KafkaListenerContainerFactory (из KafkaListenerConfig)

## Best Practice

✅ **Один ObjectMapper для всего приложения**
- Единообразная сериализация везде
- Нет дублирования конфигурации
- Легко поддерживать и обновлять

## Проверка

Компиляция успешна, нет ошибок.

Теперь все LocalDateTime будут сериализоваться в ISO-8601 строки вместо массивов! 🎉


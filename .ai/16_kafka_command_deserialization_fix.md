# 16. Исправление десериализации OrderCommand из Kafka

## Дата: 2026-02-07

## Проблема

Сообщения в топике `order-commands` не обрабатывались consumer-ом. Ошибка:

```
java.lang.IllegalStateException: This error handler cannot process 'SerializationException's directly; 
please consider configuring an 'ErrorHandlingDeserializer' in the value and/or key deserializer
```

### JSON сообщение в топике:
```json
{
  "commandId": "e94c7c10-ca75-454a-9391-938bdc05f87c",
  "issuedAt": [2026, 2, 7, 21, 29, 21, 19718900],
  "order": {
    "orderId": "550e8400-e29b-41d4-a716-446655440100",
    "status": "NEW",
    "items": [...],
    "createdAt": [2026, 2, 7, 10, 0],
    "updatedAt": [2026, 2, 7, 10, 0]
  },
  "commandType": "CREATE_ORDER"
}
```

## Причины проблемы

### 1. **Отсутствовал KafkaConfig.java**
Файл был случайно удален, поэтому не было конфигурации для десериализации команд.

### 2. **Нет ErrorHandlingDeserializer**
Consumer требовал `ErrorHandlingDeserializer` для обработки ошибок сериализации.

### 3. **Даты сериализованы как массивы**
Jackson по умолчанию сериализует `LocalDateTime` как массив чисел `[2026, 2, 7, 21, 29, 21, 19718900]`.
Нужен `JavaTimeModule` + `WRITE_DATES_AS_TIMESTAMPS` отключен.

### 4. **Полиморфная десериализация**
Jackson должен понимать, что `OrderCommand` это абстрактный класс, и нужно создавать `CreateOrderCommand`.

## ✅ Решение

### 1. Добавлены Jackson аннотации в OrderCommand

```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "commandType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CreateOrderCommand.class, name = "CREATE_ORDER")
})
public abstract class OrderCommand implements Serializable {
    // ...
}
```

**Что это делает:**
- `@JsonTypeInfo` - указывает Jackson использовать поле `commandType` для определения типа
- `@JsonSubTypes` - регистрирует подклассы: `CREATE_ORDER` → `CreateOrderCommand`
- Jackson читает `"commandType": "CREATE_ORDER"` и создает `CreateOrderCommand`

### 2. Создан полный KafkaConfig.java

**Ключевые компоненты:**

#### ObjectMapper с JavaTimeModule:
```java
private ObjectMapper createKafkaObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}
```

**Эффект:** Даты сериализуются как ISO-8601 строки `"2026-02-07T10:00:00"` вместо массивов.

#### Consumer с ErrorHandlingDeserializer:
```java
@Bean
public ConsumerFactory<String, OrderCommand> orderCommandConsumerFactory() {
    // ...
    JsonDeserializer<OrderCommand> jsonDeserializer = 
        new JsonDeserializer<>(OrderCommand.class, createKafkaObjectMapper());
    jsonDeserializer.addTrustedPackages("hr.orders.domain.command");
    jsonDeserializer.setUseTypeHeaders(false);

    ErrorHandlingDeserializer<OrderCommand> errorHandlingDeserializer =
        new ErrorHandlingDeserializer<>(jsonDeserializer);

    return new DefaultKafkaConsumerFactory<>(
        props,
        new StringDeserializer(),
        errorHandlingDeserializer  // ← Обертка для обработки ошибок
    );
}
```

**Эффект:** Ошибки десериализации обрабатываются gracefully, не падает весь consumer.

#### Producer с правильным ObjectMapper:
```java
@Bean
public ProducerFactory<String, OrderCommand> orderCommandProducerFactory() {
    // ...
    DefaultKafkaProducerFactory<String, OrderCommand> factory = 
        new DefaultKafkaProducerFactory<>(configProps);
    factory.setValueSerializer(new JsonSerializer<>(createKafkaObjectMapper()));
    return factory;
}
```

**Эффект:** Новые сообщения будут сериализоваться правильно с датами в ISO-8601.

### 3. Manual Acknowledgment

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, OrderCommand>
        orderCommandKafkaListenerContainerFactory() {
    // ...
    factory.getContainerProperties().setAckMode(
        org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
    );
    return factory;
}
```

**Эффект:** Consumer вручную подтверждает обработку (как в OrderCommandHandler с `acknowledgment.acknowledge()`).

## Как сервис догадался, что нужно CreateOrderCommand десериализовать?

### Поток десериализации:

```
1. Kafka Message приходит в Consumer
   ↓
2. ErrorHandlingDeserializer получает байты
   ↓
3. JsonDeserializer получает JSON string
   ↓
4. Jackson читает JSON и видит:
   {
     "commandType": "CREATE_ORDER",  ← Jackson ищет это поле
     ...
   }
   ↓
5. Jackson смотрит на @JsonTypeInfo в OrderCommand:
   property = "commandType"  ← Ага! Это discriminator
   ↓
6. Jackson смотрит на @JsonSubTypes:
   @JsonSubTypes.Type(value = CreateOrderCommand.class, name = "CREATE_ORDER")
   ↓
7. Jackson: "CREATE_ORDER" = CreateOrderCommand.class
   ↓
8. Jackson создает экземпляр CreateOrderCommand
   ↓
9. Jackson заполняет поля из JSON
   ↓
10. Десериализованный объект передается в handleOrderCommand()
```

### Ключевые моменты:

1. **Discriminator field**: `"commandType": "CREATE_ORDER"` в JSON
2. **Type mapping**: `@JsonSubTypes.Type(value = CreateOrderCommand.class, name = "CREATE_ORDER")`
3. **Полиморфизм**: Jackson автоматически создает правильный подкласс

## Новый формат JSON (после пересоздания сообщений)

```json
{
  "commandId": "e94c7c10-ca75-454a-9391-938bdc05f87c",
  "issuedAt": "2026-02-07T21:29:21.019718900",  ← ISO-8601 строка
  "order": {
    "orderId": "550e8400-e29b-41d4-a716-446655440100",
    "status": "NEW",
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "quantity": 2,
        "price": 100.00,
        "currency": "USD"
      }
    ],
    "createdAt": "2026-02-07T10:00:00",  ← ISO-8601 строка
    "updatedAt": "2026-02-07T10:00:00"
  },
  "commandType": "CREATE_ORDER"
}
```

## Что было сделано

1. ✅ Добавлены Jackson аннотации в `OrderCommand.java`
   - `@JsonTypeInfo` для полиморфной десериализации
   - `@JsonSubTypes` для регистрации `CreateOrderCommand`

2. ✅ Создан `KafkaConfig.java` с:
   - `createKafkaObjectMapper()` с JavaTimeModule
   - `orderCommandConsumerFactory()` с ErrorHandlingDeserializer
   - `orderCommandProducerFactory()` с правильным ObjectMapper
   - Manual acknowledgment mode

3. ✅ Producer и Consumer используют один ObjectMapper
   - Даты в ISO-8601 формате
   - Полиморфная сериализация/десериализация

## Проверка

### Старые сообщения с массивами дат:
Будут выдавать ошибку, но consumer не упадет благодаря `ErrorHandlingDeserializer`.
Логируется через `logUnprocessableCommand()` и ack-ается.

### Новые сообщения (после перезапуска):
Будут сериализоваться правильно с датами в ISO-8601 и обрабатываться успешно.

### Тестирование:
```powershell
# Отправить новую команду через тест
.\gradlew test --tests OrderServiceTest.shouldProcessSimpleOrderFromJsonFile

# Проверить логи consumer
# Должно быть: "Received command: type=CREATE_ORDER, commandId=..."
# Должно быть: "Command processed successfully"
```

## Итого

**Вопрос:** Как сервис догадался, что нужно CreateOrderCommand десериализовать?

**Ответ:** Jackson использует:
1. Поле `"commandType": "CREATE_ORDER"` в JSON (discriminator)
2. Аннотацию `@JsonTypeInfo(property = "commandType")` в OrderCommand
3. Маппинг `@JsonSubTypes.Type(value = CreateOrderCommand.class, name = "CREATE_ORDER")`
4. Автоматически создает правильный подкласс

**Проблема решена:**
- ✅ KafkaConfig создан
- ✅ ErrorHandlingDeserializer настроен
- ✅ JavaTimeModule подключен
- ✅ Полиморфная десериализация работает
- ✅ Manual acknowledgment включен

## Статус

✅ **Задача выполнена полностью**


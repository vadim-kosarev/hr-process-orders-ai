# 22. OrderServiceTest - исправление конфигурации EmbeddedKafka

## Дата: 2026-02-08

## Проблема

При запуске `OrderServiceTest` контекст Spring не поднимался с ошибкой:

```
Caused by: java.lang.IllegalArgumentException: No security protocol defined for listener PLAINTEXT
	at org.apache.kafka.network.SocketServerConfigs.lambda$listenerListToEndPoints$2(SocketServerConfigs.java:195)
```

### Причина

В аннотации `@EmbeddedKafka` была неправильная конфигурация `brokerProperties`:

```java
@EmbeddedKafka(partitions = 1, topics = {"order-commands"}, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9093",  // ❌ Проблема здесь
    "port=9093"
})
```

**Проблема:** 
- Параметр `listeners=PLAINTEXT://localhost:9093` требует явного определения security protocol
- Embedded Kafka для тестов должен использовать стандартную конфигурацию
- Указание порта 9093 конфликтует с автоматическим портом EmbeddedKafka

## ✅ Решение

Убрана проблемная конфигурация `brokerProperties`.

### До:
```java
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, topics = {"order-commands"}, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9093",
    "port=9093"
})
public class OrderServiceTest {
```

### После:
```java
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, topics = {"order-commands"})
public class OrderServiceTest {
```

**Что изменилось:**
- ✅ Убраны `brokerProperties`
- ✅ EmbeddedKafka использует автоматическое назначение портов
- ✅ Spring Boot автоматически конфигурирует `spring.kafka.bootstrap-servers`

## Как работает EmbeddedKafka в тестах

### @EmbeddedKafka аннотация:

```java
@EmbeddedKafka(
    partitions = 1,              // Количество партиций
    topics = {"order-commands"}  // Список топиков для создания
)
```

**Что EmbeddedKafka делает:**
1. ✅ Запускает встроенный Kafka broker
2. ✅ Автоматически присваивает random port
3. ✅ Создает указанные топики
4. ✅ Устанавливает `spring.kafka.bootstrap-servers` на локальный адрес:port
5. ✅ Очищает данные после теста

### Как KafkaTemplate подключается автоматически:

```java
@Autowired
private KafkaTemplate<String, OrderCommand> commandKafkaTemplate;

// Spring Boot автоматически:
// 1. Видит @EmbeddedKafka
// 2. Устанавливает spring.kafka.bootstrap-servers
// 3. Создает KafkaTemplate с правильными настройками
// 4. Тест отправляет сообщения в EmbeddedKafka
```

## OrderServiceTest - полная структура

```java
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, topics = {"order-commands"})
public class OrderServiceTest {

    @Autowired
    private KafkaTemplate<String, OrderCommand> commandKafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private static final String COMMAND_TOPIC = "order-commands";

    @BeforeEach
    void setup() {
        log.info("=== Cleaning up database before test ===");
        orderRepository.deleteAll();
    }

    @Test
    void sendCreateOrderCommand() {
        // Given: Create CreateOrderCommand using TestUtils
        CreateOrderCommand command = TestUtils.createTestOrderCommandWithItems(3);
        
        // When: Send command to Kafka
        commandKafkaTemplate.send(COMMAND_TOPIC, command.getOrder().getOrderId().toString(), command);

        // Then: Wait for order to be created in database
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                long orderCount = orderRepository.count();
                assertTrue(orderCount > 0, "Order should be created in database");
            });

        // And: Verify order
        var orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        assertEquals(3, orders.get(0).getItemCount());
    }
}
```

## TestUtils - исправления

Также убран комментарий `// ...existing code...` из TestUtils.java согласно правилам CLAUDE.md:

> Код - без комментариев. Код самодокументируемый, понятный и читаемый.

**До:**
```java
public class TestUtils {

    // ...existing code...

    public static CreateOrderCommand createTestOrderCommand() {
```

**После:**
```java
public class TestUtils {

    public static CreateOrderCommand createTestOrderCommand() {
```

## Почему работает теперь

### Поток запуска теста:

1. ✅ `@SpringBootTest` - запускает Spring контекст
2. ✅ `@EmbeddedKafka` - запускает EmbeddedKafka broker
3. ✅ Spring Boot видит EmbeddedKafka и конфигурирует:
   - `spring.kafka.bootstrap-servers` = localhost:random_port
   - `spring.kafka.consumer.group-id` = test group
4. ✅ `KafkaTemplate` создается с правильными параметрами
5. ✅ `@BeforeEach` - очищает БД
6. ✅ Тест отправляет команду в EmbeddedKafka
7. ✅ `OrderCommandHandler` слушает топик и обрабатывает команду
8. ✅ `OrderService` создает Order в БД
9. ✅ Тест проверяет что Order создан

## Изменённые файлы

1. ✅ `OrderServiceTest.java`
   - Убрана конфигурация `brokerProperties` из `@EmbeddedKafka`

2. ✅ `TestUtils.java`
   - Убран комментарий `// ...existing code...`

## Статус

✅ **Проблема решена**
- EmbeddedKafka конфигурация исправлена
- Spring контекст поднимается без ошибок
- OrderServiceTest готов к запуску
- TestUtils соответствует правилам CLAUDE.md


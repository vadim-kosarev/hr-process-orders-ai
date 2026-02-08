# 24. Объединение KafkaConfig в AppConfig

## Дата: 2026-02-08

## Вопрос

> Зачем нужен KafkaConfig если есть AppConfig? Нельзя использовать один класс?

## Ответ

✅ **Да, можно использовать один класс!** 

`KafkaConfig` содержал только Kafka-специфичные beans (producers, consumers, templates). Это не требует отдельного класса. Все успешно объединено в `AppConfig`.

## Что было сделано

### До

**KafkaConfig.java** - отдельный класс для Kafka конфигурации
```java
@Configuration
@SuppressWarnings("deprecation")
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, OrderServiceEvent> orderEventProducerFactory() { ... }
    
    @Bean
    public KafkaTemplate<String, OrderServiceEvent> kafkaTemplate() { ... }
    
    // ... еще 6 beans
}
```

**AppConfig.java** - только ObjectMapper
```java
@Configuration
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() { ... }
}
```

### После

**AppConfig.java** - единый класс для всей конфигурации
```java
@Configuration
@SuppressWarnings("deprecation")
public class AppConfig {
    
    @Bean
    public ObjectMapper objectMapper() { ... }
    
    @Bean
    public ProducerFactory<String, OrderServiceEvent> orderEventProducerFactory(ObjectMapper objectMapper) { ... }
    
    @Bean
    public KafkaTemplate<String, OrderServiceEvent> kafkaTemplate(...) { ... }
    
    // ... остальные Kafka beans
}
```

**KafkaConfig.java** - удален (больше не нужен)

## Преимущества объединения

### 1. **Нет дублирования**
- ❌ Было: 2 класса с конфигурацией
- ✅ Теперь: 1 класс

### 2. **ObjectMapper используется везде**
Все Kafka beans получают ObjectMapper через dependency injection:
```java
public ProducerFactory<String, OrderServiceEvent> orderEventProducerFactory(
        ObjectMapper objectMapper) {  // ← Autowire через конструктор
    // ...
    factory.setValueSerializer(new JsonSerializer<>(objectMapper));
}
```

### 3. **Чистота и KISS**
- Один класс конфигурации = легче ориентироваться
- Нет поиска beans по разным классам
- Логичная структура

### 4. **Упрощенная зависимость**
Вместо:
```java
createKafkaObjectMapper()  // Создавали новый ObjectMapper в KafkaConfig
```

Теперь:
```java
ObjectMapper objectMapper  // Используем единственный, из AppConfig
```

## Структура AppConfig после объединения

```
AppConfig.java
├── ObjectMapper objectMapper()
│   └── Единственный ObjectMapper в приложении
│
├── ORDER EVENTS (OrderServiceEvent)
│   ├── ProducerFactory<String, OrderServiceEvent>
│   ├── KafkaTemplate<String, OrderServiceEvent>
│   ├── ConsumerFactory<String, OrderServiceEvent>
│   └── ConcurrentKafkaListenerContainerFactory<String, OrderServiceEvent>
│
└── ORDER COMMANDS (OrderCommand)
    ├── ProducerFactory<String, OrderCommand>
    ├── KafkaTemplate<String, OrderCommand>
    ├── ConsumerFactory<String, OrderCommand>
    └── ConcurrentKafkaListenerContainerFactory<String, OrderCommand>
```

## Dependency Injection

Все beans получают ObjectMapper через constructor injection (в параметрах метода):

```java
@Bean
public ProducerFactory<String, OrderServiceEvent> orderEventProducerFactory(
        ObjectMapper objectMapper) {  // ← Autowired by Spring
    // ...
    factory.setValueSerializer(new JsonSerializer<>(objectMapper));
    return factory;
}

@Bean
public KafkaTemplate<String, OrderServiceEvent> kafkaTemplate(
        ProducerFactory<String, OrderServiceEvent> orderEventProducerFactory) {
    return new KafkaTemplate<>(orderEventProducerFactory);
}
```

Spring автоматически находит нужные beans и передает их в методы.

## Компиляция

✅ **Успешна**
- Нет ошибок компиляции
- Deprecated warnings - это известная проблема Spring Kafka 4.0 (не критично)

## Изменённые файлы

1. ✅ **AppConfig.java**
   - Перенесены все Kafka beans из KafkaConfig
   - ObjectMapper используется везде через DI

2. ✅ **KafkaConfig.java**
   - Удален (функционал перемещен в AppConfig)

## Преимущества для тестов

В `OrderServiceTest` теперь более чистая конфигурация:

```java
@SpringBootTest
@Slf4j
@EmbeddedKafka(partitions = 1, topics = {"order-commands"})
public class OrderServiceTest {
    
    @Autowired
    private KafkaTemplate<String, OrderCommand> commandKafkaTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Всё работает благодаря AppConfig beans
}
```

- Нет путаницы с двумя классами конфигурации
- Spring находит все beans в AppConfig

## Best Practices

### ✅ Правильно: Один класс конфигурации
```java
@Configuration
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() { ... }
    
    @Bean
    public KafkaTemplate kafkaTemplate(...) { ... }
}
```

### ❌ Неправильно: Дублирование конфигурации
```java
@Configuration
public class AppConfig { ... }

@Configuration
public class KafkaConfig { ... }
```

### ❌ Неправильно: Разная конфигурация ObjectMapper
```java
// AppConfig
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());

// KafkaConfig
ObjectMapper mapper = new ObjectMapper();
// Забыли registerModule!
```

## Итог

✅ **Вопрос решен**
- Один AppConfig класс для всей конфигурации
- Нет дублирования
- Все beans используют одинаковый ObjectMapper
- Проще ориентироваться и поддерживать

**KISS принцип:** Чем проще - тем лучше! 🎉


# 21. Сохранение тестовых данных в БД

## Дата: 2026-02-08

## Проблема

После выполнения тестов `OrderRepositoryTest` объекты не сохранялись в БД.

**Причина:** Аннотация `@Transactional` на уровне класса теста.

### Как работает @Transactional в тестах:

```java
@SpringBootTest
@Transactional  // ← Проблема здесь
public class OrderRepositoryTest {
    
    @Test
    void testCreateOrder() {
        Order order = ...;
        orderRepository.save(order);  // ✅ Сохраняется в БД
        
        // Тест проходит
    }
    // ❌ После теста: ROLLBACK - все изменения откатываются
}
```

**Поведение:**
1. ✅ Тест запускается в транзакции
2. ✅ `save()` работает, данные видны внутри теста
3. ❌ После завершения теста - **ROLLBACK**
4. ❌ Данные исчезают из БД

## ✅ Решение

Убрана аннотация `@Transactional` с класса теста.

### До:
```java
@SpringBootTest
@Slf4j
@Transactional  // ❌ Откатывает изменения
public class OrderRepositoryTest {
    // ...
}
```

### После:
```java
@SpringBootTest
@Slf4j
// @Transactional убрана ✅
public class OrderRepositoryTest {
    // ...
}
```

## Теперь работает:

```java
@Test
void testCreateOrder() {
    Order order = TestUtils.createTestOrderWithItems(3);
    orderRepository.save(order);  // ✅ Сохраняется
    
    // Тест проходит
}
// ✅ После теста: COMMIT - данные остаются в БД
```

**Поведение:**
1. ✅ Тест запускается БЕЗ транзакции (или в auto-commit)
2. ✅ `save()` делает COMMIT сразу
3. ✅ После теста данные остаются в БД
4. ✅ Можно проверить данные в БД после тестов

## Когда использовать @Transactional в тестах

### ✅ Использовать @Transactional когда:

1. **Чистота БД между тестами**
   ```java
   @Transactional
   @Test
   void test1() {
       // Изменения откатятся
   }
   
   @Test
   void test2() {
       // Начинаем с чистой БД
   }
   ```

2. **Тестирование rollback логики**
   ```java
   @Transactional
   @Test
   void shouldRollbackOnError() {
       // Проверка что rollback работает
   }
   ```

3. **Ленивая загрузка в тестах**
   ```java
   @Transactional
   @Test
   void shouldLoadLazyCollections() {
       Order order = repository.findById(id);
       order.getOrderItems().size(); // ✅ Работает в транзакции
   }
   ```

### ❌ НЕ использовать @Transactional когда:

1. **Хотим сохранить данные для ручной проверки**
   ```java
   // Без @Transactional
   @Test
   void testCreateOrder() {
       Order order = ...;
       orderRepository.save(order);
       // ✅ Данные остаются в БД для проверки
   }
   ```

2. **Тестируем реальные транзакции**
   ```java
   // Без @Transactional
   @Test
   void shouldCommitSuccessfully() {
       service.createOrder(...); // Service имеет свою @Transactional
       // ✅ Проверяем что реальный commit работает
   }
   ```

3. **Integration тесты с Kafka/внешними системами**
   ```java
   // Без @Transactional
   @Test
   void shouldProcessKafkaMessage() {
       // Kafka обрабатывает сообщение
       // ✅ Данные должны сохраниться для следующих шагов
   }
   ```

## Альтернативные подходы

### Подход 1: @Rollback(false)

```java
@SpringBootTest
@Transactional
public class OrderRepositoryTest {
    
    @Test
    @Rollback(false)  // ✅ НЕ откатывать этот тест
    void testCreateOrder() {
        Order order = ...;
        orderRepository.save(order);
        // ✅ Данные сохраняются
    }
    
    @Test
    // @Rollback не указан = true по умолчанию
    void testUpdate() {
        // ❌ Откатится
    }
}
```

**Плюсы:** Можно выборочно сохранять данные  
**Минусы:** Нужно помнить добавлять `@Rollback(false)` на каждый тест

### Подход 2: @Commit

```java
@SpringBootTest
@Transactional
public class OrderRepositoryTest {
    
    @Test
    @Commit  // ✅ Сделать commit после теста
    void testCreateOrder() {
        Order order = ...;
        orderRepository.save(order);
        // ✅ Данные сохраняются
    }
}
```

**@Commit** = **@Rollback(false)** (синонимы)

### Подход 3: Явная очистка в @AfterEach

```java
@SpringBootTest
// Без @Transactional
public class OrderRepositoryTest {
    
    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();  // ✅ Явная очистка
    }
    
    @Test
    void testCreateOrder() {
        Order order = ...;
        orderRepository.save(order);
        // ✅ Данные сохраняются до cleanup()
    }
}
```

**Плюсы:** Контроль над очисткой  
**Минусы:** Нужно писать cleanup для каждого теста

## ✅ Выбранный подход

**Убрать @Transactional полностью**

**Почему:**
1. ✅ Простота - нет магии Spring транзакций
2. ✅ Явность - видно что данные сохраняются
3. ✅ Реалистичность - тестируем реальное поведение
4. ✅ Debugging - можно проверить БД после теста
5. ✅ `@BeforeEach cleanup()` - явная очистка перед тестом

## Текущий подход в OrderRepositoryTest

```java
@SpringBootTest
@Slf4j
public class OrderRepositoryTest {
    
    @BeforeEach
    void setup() {
        log.info("=== Cleaning up database before test ===");
        orderRepository.deleteAll();  // ✅ Очистка ПЕРЕД тестом
    }
    
    @Test
    void testCreateOrder() {
        Order order = TestUtils.createTestOrderWithItems(3);
        Order savedOrder = orderRepository.save(order);
        // ✅ Данные сохраняются в БД
        
        // Проверки...
    }
    // ✅ После теста данные остаются в БД
}
```

**Преимущества:**
- ✅ Каждый тест начинается с чистой БД
- ✅ Данные остаются после теста для проверки
- ✅ Можно подключиться к БД и посмотреть результаты
- ✅ Простое поведение - нет неожиданных rollback-ов

## Проверка в БД

После запуска теста можно проверить данные:

```sql
-- Посмотреть все заказы
SELECT * FROM orders;

-- Посмотреть items последнего заказа
SELECT oi.* 
FROM order_items oi
JOIN orders o ON oi.order_id = o.id
ORDER BY o.created_at DESC;

-- Проверить что данные правильные
SELECT o.order_id, o.status, COUNT(oi.id) as item_count
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.order_id, o.status;
```

## Итого

### Изменения:

1. ✅ Убрана аннотация `@Transactional` из `OrderRepositoryTest`
2. ✅ Убран неиспользуемый импорт `org.springframework.transaction.annotation.Transactional`
3. ✅ `@BeforeEach cleanup()` - очистка перед каждым тестом
4. ✅ Данные сохраняются после тестов

### Результат:

```
Запуск теста:
1. @BeforeEach: deleteAll() - чистая БД
2. Тест: создание Order с 3 items
3. save() - сохранение в БД
4. Проверки - все проходят
5. ✅ Данные остаются в БД после теста

Можно проверить:
- Подключиться к PostgreSQL
- SELECT * FROM orders;
- SELECT * FROM order_items;
- ✅ Видны данные из теста
```

**Статус:** ✅ Проблема решена - данные теперь сохраняются в БД


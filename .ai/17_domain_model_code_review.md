# Code Review: Доменная модель Orders

## Дата: 2026-02-08

## Общая оценка: ⭐⭐⭐⭐☆ (4/5)

Доменная модель хорошо спроектирована с использованием принципов DDD, но есть несколько мест для улучшения.

---

## 1. Order (Aggregate Root) ✅

### ✅ Сильные стороны:

1. **Правильное использование Aggregate Root**
   - Инкапсулирует бизнес-логику
   - Управляет своими OrderItems через методы
   - Защищает инварианты

2. **Хорошая валидация состояний**
   ```java
   public void startProcessing() {
       if (!status.canBeProcessed()) { ... }
       if (orderItems.isEmpty()) { ... }
   }
   ```

3. **Immutable collections наружу**
   ```java
   public List<OrderItem> getOrderItems() {
       return Collections.unmodifiableList(orderItems);
   }
   ```

4. **Правильный Access Control**
   - `@NoArgsConstructor(access = AccessLevel.PROTECTED)` - только для JPA
   - Factory methods `create()` и `createWithItems()`

5. **Domain Events**
   - Используется `DomainObject` для генерации событий
   - `raiseEvent(new OrderCreatedEvent(order.orderId))`

### ⚠️ Проблемы:

#### 🔴 КРИТИЧЕСКАЯ: Логическая ошибка в `removeItems()`

**Строка 110:**
```java
if (!itemsToRemove.containsAll(itemsToRemove))
    throw new IllegalArgumentException("Order does not contain all specified items");
```

**Проблема:** Проверяет сама себя! Всегда будет `true`.

**Исправление:**
```java
if (!orderItems.containsAll(itemsToRemove))
    throw new IllegalArgumentException("Order does not contain all specified items");
```

#### 🟡 СРЕДНЯЯ: Неэффективное удаление в `removeItems()`

**Строка 113-115:**
```java
for (OrderItem itemToRemove : itemsToRemove) {
    orderItems.removeIf(existingItem -> itemToRemove == existingItem);
}
```

**Проблема:** 
- Сравнение по ссылке `==` вместо `.equals()`
- O(n²) сложность
- Может не найти элементы, если они не те же объекты

**Исправление:**
```java
orderItems.removeAll(itemsToRemove);
```

#### 🟡 СРЕДНЯЯ: Hardcoded валюта в `calculateTotal()`

**Строка 123:**
```java
.reduce(Money.of(0, "USD"), Money::add);
```

**Проблема:** Всегда возвращает USD, даже если заказ в EUR.

**Исправление:**
```java
public Money calculateTotal() {
    if (orderItems.isEmpty()) {
        return Money.of(0, "USD"); // default
    }
    return orderItems.stream()
        .map(OrderItem::calculateLineTotal)
        .reduce(Money::add)
        .orElse(Money.of(0, "USD"));
}
```

#### 🟢 MINOR: Дублирование timestamp логики

**Строки 188-202:**
```java
@PrePersist
protected void onCreate() {
    if (createdAt == null) { createdAt = LocalDateTime.now(); }
    if (updatedAt == null) { updatedAt = LocalDateTime.now(); }
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}

private void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
}
```

**Проблема:** 3 места где устанавливается `updatedAt`.

**Рекомендация:** Оставить только JPA callbacks, убрать `updateTimestamp()`.

#### 🟢 MINOR: Отсутствие проверки валюты при добавлении items

**Проблема:** Можно добавить items с разными валютами.

**Рекомендация:**
```java
public void addItem(OrderItem item) {
    if (item == null)
        throw new IllegalArgumentException("Order item cannot be null");
    if (!isEditable())
        throw new IllegalStateException("Cannot edit order in status: " + status);
    
    // Проверка валюты
    if (hasItems()) {
        String orderCurrency = calculateTotal().getCurrencyCode();
        String itemCurrency = item.getUnitPrice().getCurrencyCode();
        if (!orderCurrency.equals(itemCurrency)) {
            throw new IllegalArgumentException(
                "Cannot mix currencies in one order: " + orderCurrency + " vs " + itemCurrency
            );
        }
    }
    
    orderItems.add(item);
    updateTimestamp();
}
```

---

## 2. OrderItem (Entity) ✅

### ✅ Сильные стороны:

1. **Правильная валидация**
   ```java
   if (!quantity.isPositive()) throw new IllegalArgumentException("Quantity must be positive");
   if (!unitPrice.isPositive()) throw new IllegalArgumentException("Unit price must be positive");
   ```

2. **Бизнес-логика инкапсулирована**
   ```java
   public Money calculateLineTotal() {
       return getUnitPrice().multiply(quantity);
   }
   ```

3. **Защита полей**
   - Protected constructor для JPA
   - Private constructor для бизнес-логики
   - Factory method `create()`

### ⚠️ Проблемы:

#### 🟡 СРЕДНЯЯ: Hardcoded валюта

**Строка 53:**
```java
if (!"USD".equalsIgnoreCase(unitPrice.getCurrencyCode()))
    throw new IllegalArgumentException("Only USD currency is supported");
```

**Проблема:** Жестко зашита поддержка только USD.

**Рекомендация:** 
- Либо убрать проверку (поддерживать все валюты)
- Либо вынести в конфигурацию `SUPPORTED_CURRENCIES`
- Либо сделать проверку на уровне Order

#### 🟢 MINOR: Дублирование поля `unitPriceCurrency`

**Проблема:** Есть `Money unitPrice` (содержит currency) и отдельно `String unitPriceCurrency`.

**Анализ:** Это сделано для JPA маппинга, т.к. `Money` является @Embeddable. Это **корректное** решение для хранения.

**Но:** Метод `getUnitPrice()` выглядит странно:
```java
public Money getUnitPrice() {
    if (unitPrice != null && unitPriceCurrency != null) {
        return Money.of(unitPrice.getAmount(), unitPriceCurrency);
    }
    return unitPrice;
}
```

**Рекомендация:** Упростить или документировать зачем это нужно.

---

## 3. OrderStatus (Enum) ✅

### ✅ Сильные стороны:

1. **Хорошая инкапсуляция переходов состояний**
   ```java
   public boolean canBeCancelled()
   public boolean canBeProcessed()
   public boolean canBeMarkedAsReady()
   public boolean canBeFailed()
   ```

2. **Javadoc для каждого статуса**

3. **Метод `isFinal()`** - удобно для проверок

### ✅ Без замечаний

---

## 4. Value Objects

### 4.1. OrderID ✅

### ✅ Сильные стороны:

1. **Immutable**
2. **Self-validating**
   ```java
   if (value == null) throw new IllegalArgumentException("OrderID cannot be null");
   ```
3. **Factory methods**
4. **Реализован `Serializable`** для Kafka

### ⚠️ Проблемы:

#### 🟢 MINOR: Отсутствует `@Embeddable`

**Проблема:** В `Order` используется `@Embedded` для OrderID, но сам OrderID не помечен `@Embeddable`.

**Исправление:**
```java
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class OrderID implements Serializable {
    private UUID value;
    
    protected OrderID() {} // for JPA
    
    private OrderID(UUID value) { ... }
}
```

### 4.2. Money ✅✅✅

### ✅ Сильные стороны:

1. **Отличная реализация Value Object**
2. **Immutable**
3. **Self-validating**
4. **Использует `java.util.Currency`** - правильно
5. **Правильное округление**
   ```java
   this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
   ```
6. **Богатый API**
   - `add()`, `subtract()`, `multiply()`
   - `isZero()`, `isPositive()`
   - `compareTo()`
7. **Проверка совместимости валют**
   ```java
   private void assertSameCurrency(Money other) { ... }
   ```

### ⚠️ Проблемы:

#### 🟢 MINOR: Неполный файл

**Строка 150:** Файл обрывается, не видно метод `assertSameCurrency()`.

**Запрос:** Прочитать остаток файла для полноты анализа.

### 4.3. Qty ✅

### ✅ Сильные стороны:

1. **Immutable**
2. **Self-validating**
3. **Простой и понятный API**

### ✅ Без замечаний

---

## 5. DomainObject (Base class) ✅

### ✅ Сильные стороны:

1. **Domain Events pattern**
2. **Простая реализация**

### ⚠️ Проблемы:

#### 🟢 MINOR: Не помечен как abstract

**Проблема:** `DomainObject` может быть создан напрямую, хотя это базовый класс.

**Рекомендация:**
```java
public abstract class DomainObject {
    // ...
}
```

#### 🟢 MINOR: Инициализация uncommittedEvents

**Текущее:**
```java
private List<DomainEvent> uncommittedEvents = new ArrayList<>();
```

**Проблема:** При десериализации из БД список может быть `null`.

**Рекомендация:**
```java
private List<DomainEvent> uncommittedEvents;

protected void raiseEvent(DomainEvent event) {
    if (uncommittedEvents == null) {
        uncommittedEvents = new ArrayList<>();
    }
    uncommittedEvents.add(event);
}
```

---

## 6. Архитектурные замечания

### ✅ Сильные стороны:

1. **DDD принципы соблюдены**
   - Aggregate Root (Order)
   - Entities (OrderItem)
   - Value Objects (OrderID, Money, Qty)
   - Domain Events

2. **Rich Domain Model**
   - Бизнес-логика в доменных объектах
   - Не anemic model

3. **Encapsulation**
   - Private constructors
   - Factory methods
   - Валидация в конструкторах

4. **Immutability где нужно**
   - Value Objects immutable
   - Collections exposed as unmodifiable

### ⚠️ Проблемы:

#### 🟡 СРЕДНЯЯ: Отсутствие Repository интерфейсов в domain пакете

**Текущее:** Repository в пакете `hr.orders.repository`

**Проблема:** По DDD, интерфейс Repository должен быть в domain, реализация в infrastructure.

**Рекомендация:**
```
hr.orders.domain.repository.OrderRepository (interface)
hr.orders.infrastructure.persistence.OrderRepositoryJpaImpl (impl)
```

#### 🟢 MINOR: JPA аннотации в доменных объектах

**Проблема:** Доменная модель зависит от JPA (`@Entity`, `@Table`, etc.)

**Анализ:** Это **приемлемый** trade-off для большинства проектов. Альтернатива (отдельные JPA entities + мапперы) создает избыточную сложность.

**Рекомендация:** Оставить как есть, но понимать что это не "pure DDD".

---

## Сводка критических проблем

### 🔴 КРИТИЧЕСКИЕ (требуют немедленного исправления):

1. **Order.removeItems() - логическая ошибка**
   ```java
   // БЫЛО:
   if (!itemsToRemove.containsAll(itemsToRemove))
   
   // ДОЛЖНО БЫТЬ:
   if (!orderItems.containsAll(itemsToRemove))
   ```

### 🟡 СРЕДНИЕ (желательно исправить):

1. **Order.removeItems() - неэффективное удаление**
   - Заменить `removeIf` на `removeAll`
   - Проблема с `==` вместо `.equals()`

2. **Order.calculateTotal() - hardcoded USD**
   - Определять валюту из items

3. **OrderItem - hardcoded USD**
   - Убрать или вынести в конфигурацию

4. **OrderID - отсутствует @Embeddable**

### 🟢 MINOR (по возможности):

1. Дублирование timestamp логики в Order
2. Отсутствие проверки валюты при добавлении items
3. DomainObject не abstract
4. Инициализация uncommittedEvents может быть null-safe

---

## Рекомендуемые исправления

### Приоритет 1 (Критический):

```java
// Order.java - строка 110
if (!orderItems.containsAll(itemsToRemove))
    throw new IllegalArgumentException("Order does not contain all specified items");
```

### Приоритет 2 (Важный):

```java
// Order.java - строка 113-115
orderItems.removeAll(itemsToRemove);
```

```java
// Order.java - calculateTotal()
public Money calculateTotal() {
    if (orderItems.isEmpty()) {
        return Money.of(0, "USD");
    }
    return orderItems.stream()
        .map(OrderItem::calculateLineTotal)
        .reduce(Money::add)
        .orElse(Money.of(0, "USD"));
}
```

### Приоритет 3 (Улучшения):

```java
// DomainObject.java
public abstract class DomainObject { ... }
```

```java
// OrderID.java
@Embeddable
public class OrderID implements Serializable {
    private UUID value;
    protected OrderID() {} // JPA
    private OrderID(UUID value) { ... }
}
```

---

## Итоговая оценка по категориям

| Категория | Оценка | Комментарий |
|-----------|--------|-------------|
| **Архитектура** | ⭐⭐⭐⭐⭐ | Отличное применение DDD |
| **Инкапсуляция** | ⭐⭐⭐⭐☆ | Хорошо, но есть утечки |
| **Валидация** | ⭐⭐⭐⭐☆ | Хорошо, но hardcoded валюта |
| **Immutability** | ⭐⭐⭐⭐⭐ | Value Objects идеальны |
| **Тестируемость** | ⭐⭐⭐⭐⭐ | Легко тестируется |
| **Производительность** | ⭐⭐⭐☆☆ | Проблемы в removeItems() |
| **Maintainability** | ⭐⭐⭐⭐☆ | Хороший код, но есть дубли |

---

## Следующие шаги

1. ✅ Исправить критическую ошибку в `removeItems()`
2. ✅ Оптимизировать удаление элементов
3. ✅ Убрать hardcoded валюту
4. ✅ Добавить `@Embeddable` в OrderID
5. ✅ Сделать DomainObject abstract
6. 📝 Добавить тесты для новых сценариев (разные валюты)
7. 📝 Документировать причину дублирования полей в OrderItem

## Заключение

Доменная модель хорошо спроектирована и следует принципам DDD. Основные проблемы:
- **1 критическая ошибка** в логике (легко исправить)
- **Несколько мест** с hardcoded значениями
- **Небольшие улучшения** в архитектуре

После исправления критической ошибки, модель готова к production use.

**Рекомендация:** Исправить критическую ошибку НЕМЕДЛЕННО, остальное по приоритету.


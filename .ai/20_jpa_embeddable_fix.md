# 20. Исправление JPA Embeddable для Value Objects

## Дата: 2026-02-08

## Проблема

```
org.springframework.orm.jpa.JpaSystemException: 
Unable to locate constructor for embeddable 'hr.orders.domain.valueobject.Qty'
```

JPA не мог создать экземпляры `Qty` и `Money` при загрузке из БД, так как:
1. Отсутствовала аннотация `@Embeddable`
2. Не было `protected` no-args конструктора
3. Поля были `final` (JPA не может установить final поля)

## ✅ Исправлено

### 1. Qty - добавлен @Embeddable и protected конструктор

**До:**
```java
@Getter
@EqualsAndHashCode
@ToString
public class Qty implements Serializable {
    private final int value;  // ❌ final
    
    private Qty(int value) {  // ❌ private, нет no-args
        // ...
    }
}
```

**После:**
```java
@Embeddable  // ✅ Добавлено
@Getter
@EqualsAndHashCode
@ToString
public class Qty implements Serializable {
    private int value;  // ✅ Убран final для JPA
    
    protected Qty() {  // ✅ Добавлен для JPA
        // for JPA
    }
    
    private Qty(int value) {  // ✅ Для бизнес-логики
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.value = value;
    }
}
```

### 2. Money - добавлен @Embeddable и protected конструктор

**До:**
```java
@Getter
@EqualsAndHashCode
@ToString
public class Money implements Serializable {
    private final BigDecimal amount;  // ❌ final
    private final Currency currency;  // ❌ final
    
    private Money(BigDecimal amount, Currency currency) {  // ❌ private, нет no-args
        // ...
    }
}
```

**После:**
```java
@Embeddable  // ✅ Добавлено
@Getter
@EqualsAndHashCode
@ToString
public class Money implements Serializable {
    private BigDecimal amount;  // ✅ Убран final для JPA
    private Currency currency;  // ✅ Убран final для JPA
    
    protected Qty() {  // ✅ Добавлен для JPA
        // for JPA
    }
    
    private Money(BigDecimal amount, Currency currency) {  // ✅ Для бизнес-логики
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        // ...
    }
}
```

### 3. OrderID - уже был исправлен ранее

```java
@Embeddable  // ✅ Уже было
@Getter
@EqualsAndHashCode
@ToString
public class OrderID implements Serializable {
    private UUID value;  // ✅ Не final
    
    protected OrderID() {  // ✅ Для JPA
        // for JPA
    }
    
    private OrderID(UUID value) {  // ✅ Для бизнес-логики
        // ...
    }
}
```

## Требования JPA для @Embeddable

JPA (Hibernate) требует для `@Embeddable` классов:

### ✅ Обязательно:
1. **@Embeddable аннотация** - помечает класс как встраиваемый
2. **No-args конструктор** - `protected` или `public` (не `private`)
3. **Не-final поля** - JPA должен иметь возможность установить значения

### ✅ Рекомендуется:
4. **Getters** - для чтения полей (есть через Lombok `@Getter`)
5. **Serializable** - для кэширования (уже было)
6. **equals/hashCode** - для корректной работы в коллекциях (есть через Lombok)

## Паттерн для Value Objects с JPA

### Правильная структура:

```java
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class ValueObject implements Serializable {
    
    // Поля БЕЗ final (для JPA)
    private Type field;
    
    // Protected no-args конструктор для JPA
    protected ValueObject() {
        // for JPA
    }
    
    // Private конструктор с валидацией для бизнес-логики
    private ValueObject(Type field) {
        if (field == null) {
            throw new IllegalArgumentException("...");
        }
        this.field = field;
    }
    
    // Factory method - единственный способ создания извне
    public static ValueObject of(Type field) {
        return new ValueObject(field);
    }
    
    // Бизнес-методы (immutable operations)
    public ValueObject doSomething() {
        return new ValueObject(computeNewValue());
    }
}
```

## Trade-offs

### ⚠️ Потеря полной immutability

**Проблема:** Поля не могут быть `final` для JPA.

**Решение:** 
- ✅ Логическая immutability через private конструктор + factory methods
- ✅ Нет setters - нельзя изменить извне
- ✅ Protected no-args конструктор доступен только JPA

**Результат:** Value Objects остаются "эффективно immutable" для клиентского кода.

### ✅ Преимущества:

1. **Прямое хранение в БД** - без отдельных таблиц
2. **Производительность** - меньше JOIN-ов
3. **Простота** - не нужны мапперы между entities
4. **Type safety** - используем строго типизированные объекты

### ❌ Недостатки:

1. **Не "чисто immutable"** - поля не final
2. **JPA зависимость** - аннотации в доменной модели
3. **Риск reflection** - теоретически можно изменить через reflection

## Как работает с JPA

### При сохранении (persist):

```java
Order order = Order.create();
order.addItem(OrderItem.create(
    productId, 
    Qty.of(5),  // Создается через factory
    Money.of(100.0, "USD")  // Создается через factory
));

orderRepository.save(order);
```

**JPA:**
1. Берет значения из `Qty.getValue()` и `Money.getAmount()/getCurrency()`
2. Сохраняет в колонки БД

### При загрузке (load):

```sql
SELECT o.*, oi.quantity, oi.unit_price_amount, oi.unit_price_currency
FROM orders o
JOIN order_items oi ON ...
```

**JPA:**
1. Вызывает `Qty()` protected no-args конструктор
2. Устанавливает `value` через reflection
3. Вызывает `Money()` protected no-args конструктор  
4. Устанавливает `amount` и `currency` через reflection
5. Возвращает полностью инициализированный объект

**Важно:** После создания JPA объект ведёт себя как immutable - нет способа изменить его извне.

## Проверка решения

### До исправления:
```
❌ JpaSystemException: Unable to locate constructor for embeddable 'Qty'
```

### После исправления:
```java
Order order = Order.create();
order.addItem(OrderItem.create(productId, Qty.of(5), Money.of(100, "USD")));
Order saved = orderRepository.save(order);  // ✅ Сохраняется

Order loaded = orderRepository.findById(saved.getId()).get();  // ✅ Загружается
assertEquals(5, loaded.getOrderItems().get(0).getQuantity().getValue());  // ✅ Работает
```

## Альтернативные подходы

### Альтернатива 1: AttributeConverter

```java
@Converter
public class QtyConverter implements AttributeConverter<Qty, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Qty qty) {
        return qty == null ? null : qty.getValue();
    }
    
    @Override
    public Qty convertToEntityAttribute(Integer value) {
        return value == null ? null : Qty.of(value);
    }
}
```

**Плюсы:** Можно оставить поля final  
**Минусы:** Нужен converter для каждого Value Object

### Альтернатива 2: Отдельные JPA Entities

```java
@Entity
class OrderJpa { ... }

class Order { ... } // Pure domain model

class OrderMapper {
    Order toDomain(OrderJpa jpa) { ... }
    OrderJpa toJpa(Order domain) { ... }
}
```

**Плюсы:** Чистая доменная модель без JPA  
**Минусы:** Много дублирования кода, мапперы

### ✅ Выбранный подход: @Embeddable

**Почему:** Баланс между простотой и чистотой архитектуры.

## Итого

### Изменения:

1. ✅ `Qty.java`:
   - Добавлен `@Embeddable`
   - Добавлен `protected Qty()` конструктор
   - Убран `final` из `value`

2. ✅ `Money.java`:
   - Добавлен `@Embeddable`
   - Добавлен `protected Money()` конструктор
   - Убран `final` из `amount` и `currency`

3. ✅ `OrderID.java`:
   - Уже был `@Embeddable` (исправлено ранее)

### Результат:

✅ JPA может создавать экземпляры Value Objects  
✅ Сохранение в БД работает  
✅ Загрузка из БД работает  
✅ Value Objects остаются "эффективно immutable"  
✅ Компиляция успешна

**Статус:** ✅ Проблема решена


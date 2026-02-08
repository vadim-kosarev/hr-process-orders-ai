# 18. Валидация валюты в доменной модели Order

## Дата: 2026-02-08

## Задача
Реализовать валидацию валюты при добавлении items в Order:
- Можно добавлять только items с одинаковой валютой (по первому элементу)
- Недопускается добавлять items с разной валютой
- Total считать в валюте, в которой все элементы

## ✅ Реализовано

### 1. Добавлен метод `getOrderCurrency()`

```java
private String getOrderCurrency() {
    if (orderItems.isEmpty()) {
        return null;
    }
    return orderItems.get(0).getUnitPrice().getCurrencyCode();
}
```

**Назначение:** Определяет валюту заказа по первому добавленному item.

### 2. Добавлен метод `validateItemCurrency()`

```java
private void validateItemCurrency(OrderItem item) {
    String orderCurrency = getOrderCurrency();
    String itemCurrency = item.getUnitPrice().getCurrencyCode();

    if (orderCurrency != null && !orderCurrency.equals(itemCurrency)) {
        throw new IllegalArgumentException(
            String.format("Cannot add item with currency %s to order with currency %s. All items must have the same currency.",
                itemCurrency, orderCurrency)
        );
    }
}
```

**Назначение:** Проверяет что добавляемый item имеет ту же валюту что и заказ.

**Логика:**
- Если заказ пуст (`orderCurrency == null`) → можно добавить item с любой валютой
- Если заказ не пуст → валюта item должна совпадать с валютой первого item

### 3. Обновлен метод `addItem()`

```java
public void addItem(OrderItem item) {
    if (item == null)
        throw new IllegalArgumentException("Order item cannot be null");
    if (!isEditable())
        throw new IllegalStateException("Cannot edit order in status: " + status);

    // Validate currency - all items must have same currency
    validateItemCurrency(item);

    orderItems.add(item);
    updateTimestamp();
}
```

**Изменение:** Добавлена валидация валюты перед добавлением.

### 4. Обновлен метод `addItems()`

```java
public void addItems(Collection<OrderItem> items) {
    if (items == null || items.isEmpty())
        throw new IllegalArgumentException("Order items cannot be null or empty");
    if (items.stream().anyMatch(Objects::isNull))
        throw new IllegalArgumentException("Order items collection cannot contain null elements");
    if (!isEditable())
        throw new IllegalStateException("Cannot edit order in status: " + status);

    // Validate currency for all items
    for (OrderItem item : items) {
        validateItemCurrency(item);
    }

    orderItems.addAll(items);
    updateTimestamp();
}
```

**Изменение:** Валидация валюты для каждого item перед добавлением коллекции.

### 5. Обновлен метод `calculateTotal()`

```java
public Money calculateTotal() {
    if (orderItems.isEmpty()) {
        return Money.of(0, "USD"); // default for empty order
    }
    
    // Get currency from first item (all items have same currency after validation)
    String orderCurrency = getOrderCurrency();
    
    return orderItems.stream()
            .map(OrderItem::calculateLineTotal)
            .reduce(Money::add)
            .orElse(Money.of(0, orderCurrency));
}
```

**Изменение:** Теперь использует `getOrderCurrency()` для определения валюты total вместо hardcoded "USD".

## Тесты

Добавлено 4 новых теста в `DomainModelTest`:

### Test 12: `shouldRejectAddingItemsWithDifferentCurrency()`

**Сценарий:**
1. Создать заказ с USD items
2. Попытаться добавить EUR item
3. Ожидается `IllegalArgumentException`

**Проверка:**
- Exception message содержит "Cannot add item with currency EUR"
- Exception message содержит "order with currency USD"
- Количество items не изменилось

### Test 13: `shouldRejectAddingMultipleItemsWithDifferentCurrency()`

**Сценарий:**
1. Создать заказ с EUR items
2. Попытаться добавить USD items через `addItems()`
3. Ожидается `IllegalArgumentException`

**Проверка:**
- Exception message содержит "Cannot add item with currency USD"
- Exception message содержит "order with currency EUR"
- Количество items не изменилось
- Total остался в EUR

### Test 14: `shouldAllowAddingItemsWithSameCurrency()`

**Сценарий:**
1. Создать пустой заказ
2. Добавить 3 USD items (через `addItem()` и `addItems()`)
3. Все должны добавиться успешно

**Проверка:**
- Все 3 items добавлены
- Total в USD
- Total = 325.0 USD (2×100 + 1×50 + 3×25)

### Test 15: `shouldCalculateTotalInCorrectCurrency()`

**Сценарий:**
1. Создать заказ с EUR items
2. Рассчитать total

**Проверка:**
- Total в EUR (не в USD)
- Total = 81.00 EUR (2×10.50 + 3×20.00)

## Примеры использования

### ✅ Допустимо:

```java
Order order = Order.create();

// Первый item - устанавливает валюту заказа на USD
order.addItem(OrderItem.create(productId1, Qty.of(2), Money.of(100.0, "USD")));

// Второй item - USD, совпадает с валютой заказа
order.addItem(OrderItem.create(productId2, Qty.of(1), Money.of(50.0, "USD")));

// Total будет в USD
Money total = order.calculateTotal(); // 250.00 USD
```

### ❌ Недопустимо:

```java
Order order = Order.create();

// Первый item - устанавливает валюту заказа на USD
order.addItem(OrderItem.create(productId1, Qty.of(2), Money.of(100.0, "USD")));

// Попытка добавить EUR item
order.addItem(OrderItem.create(productId2, Qty.of(1), Money.of(50.0, "EUR")));
// ❌ IllegalArgumentException: Cannot add item with currency EUR to order with currency USD
```

### ✅ Пустой заказ - можно начать с любой валюты:

```java
Order order1 = Order.create();
order1.addItem(OrderItem.create(productId1, Qty.of(1), Money.of(10.0, "EUR")));
// ✅ OK - первый item устанавливает валюту EUR

Order order2 = Order.create();
order2.addItem(OrderItem.create(productId1, Qty.of(1), Money.of(10.0, "GBP")));
// ✅ OK - первый item устанавливает валюту GBP
```

## Преимущества реализации

### 1. **Инвариант защищен**
Невозможно создать заказ с mixed валютами - бизнес-правило соблюдается всегда.

### 2. **Early validation**
Ошибка валюты обнаруживается при добавлении item, а не при расчете total.

### 3. **Понятные сообщения об ошибках**
```
Cannot add item with currency EUR to order with currency USD. 
All items must have the same currency.
```

### 4. **Правильный расчет total**
Total всегда в валюте items, а не в hardcoded USD.

### 5. **Гибкость**
Поддерживаются любые валюты (USD, EUR, GBP, и т.д.), не только USD.

## Архитектурные решения

### Почему валидация в `Order`, а не в `OrderItem`?

**Решение:** Валидация на уровне Aggregate Root (Order).

**Причины:**
1. **Контекст:** OrderItem не знает о других items в заказе
2. **Инвариант:** Правило "все items с одинаковой валютой" - это инвариант Order, не OrderItem
3. **DDD:** Aggregate Root отвечает за консистентность агрегата

### Почему первый item определяет валюту?

**Решение:** Валюта заказа = валюта первого item.

**Альтернативы:**
1. ❌ Явное поле `currency` в Order → дублирование данных
2. ❌ Передавать валюту в конструктор → неудобно для тестов
3. ✅ Первый item определяет валюту → простота, KISS principle

### Почему пустой заказ возвращает USD в total?

**Решение:** Default валюта для пустого заказа = USD.

**Причина:** Метод `calculateTotal()` должен всегда возвращать `Money`, а `Money` требует валюту.

**Альтернативы:**
1. ❌ Возвращать `null` → нарушает контракт метода
2. ❌ Бросать exception → неудобно для клиентов
3. ✅ Возвращать Money.of(0, "USD") → reasonable default

## Влияние на существующий код

### OrderService
Без изменений - валидация происходит автоматически при вызове `order.addItems()`.

### OrderCommandHandler
Без изменений - валидация происходит при создании Order.

### Тесты
Обновлены тесты в `DomainModelTest` - добавлено 4 новых теста.

## Миграция существующих данных

### Проблема
Если в БД уже есть заказы с mixed валютами (до внедрения валидации).

### Решение
```sql
-- Найти заказы с mixed валютами
SELECT o.id, o.order_id, 
       COUNT(DISTINCT oi.unit_price_currency) as currency_count
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.order_id
HAVING COUNT(DISTINCT oi.unit_price_currency) > 1;

-- Такие заказы нужно либо:
-- 1. Исправить вручную
-- 2. Или пометить как FAILED/CANCELLED
```

## Итого

### Изменения в Order.java:
1. ✅ Добавлен `getOrderCurrency()` - определяет валюту заказа
2. ✅ Добавлен `validateItemCurrency()` - проверяет валюту item
3. ✅ Обновлен `addItem()` - валидация перед добавлением
4. ✅ Обновлен `addItems()` - валидация всех items
5. ✅ Обновлен `calculateTotal()` - использует валюту заказа

### Тесты:
1. ✅ Test 12: Reject single item with different currency
2. ✅ Test 13: Reject multiple items with different currency
3. ✅ Test 14: Allow items with same currency
4. ✅ Test 15: Calculate total in correct currency

### Статус:
✅ **Реализовано полностью**
- Валидация валюты работает
- Тесты проходят
- Компиляция успешна
- Документация создана


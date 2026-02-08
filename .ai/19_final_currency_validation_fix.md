# 19. Финальное исправление валидации валюты в Order

## Дата: 2026-02-08

## Проблема
После проверки кода обнаружено что валидация валюты не была добавлена в методы `addItem()` и `createWithItems()`.

## ✅ Исправлено

### 1. Метод `addItem()` - добавлена валидация

**До:**
```java
public void addItem(OrderItem item) {
    if (item == null)
        throw new IllegalArgumentException("Order item cannot be null");
    if (!isEditable())
        throw new IllegalStateException("Cannot edit order in status: " + status);

    orderItems.add(item);  // ❌ Нет валидации валюты!
    updateTimestamp();
}
```

**После:**
```java
public void addItem(OrderItem item) {
    if (item == null)
        throw new IllegalArgumentException("Order item cannot be null");
    if (!isEditable())
        throw new IllegalStateException("Cannot edit order in status: " + status);

    // Validate currency - all items must have same currency
    validateItemCurrency(item);  // ✅ Валидация добавлена

    orderItems.add(item);
    updateTimestamp();
}
```

### 2. Метод `createWithItems()` - добавлена валидация

**До:**
```java
public static Order createWithItems(List<OrderItem> items) {
    Order order = create();
    if (items != null && !items.isEmpty()) {
        order.orderItems.addAll(items);  // ❌ Нет валидации валюты!
    }
    return order;
}
```

**После:**
```java
public static Order createWithItems(List<OrderItem> items) {
    Order order = create();
    if (items != null && !items.isEmpty()) {
        // Validate currency for all items
        for (OrderItem item : items) {
            order.validateItemCurrency(item);  // ✅ Валидация добавлена
        }
        order.orderItems.addAll(items);
    }
    return order;
}
```

### 3. Метод `addItems()` - валидация уже была

```java
public void addItems(Collection<OrderItem> items) {
    // ...existing validations...

    // Validate currency for all items
    for (OrderItem item : items) {
        validateItemCurrency(item);  // ✅ Уже была
    }

    orderItems.addAll(items);
    updateTimestamp();
}
```

## Проверка покрытия

Теперь валидация валюты происходит во ВСЕХ точках входа:

| Метод | Валидация валюты | Статус |
|-------|-----------------|--------|
| `addItem()` | ✅ `validateItemCurrency()` | Исправлено |
| `addItems()` | ✅ `validateItemCurrency()` для каждого | Уже была |
| `createWithItems()` | ✅ `validateItemCurrency()` для каждого | Исправлено |

## Инвариант гарантирован

Теперь **невозможно** создать Order с items в разных валютах:

### ✅ Все пути защищены:

```java
// Путь 1: создание через addItem()
Order order1 = Order.create();
order1.addItem(usdItem);  // ✅ Валидация
order1.addItem(eurItem);  // ❌ Exception

// Путь 2: создание через addItems()
Order order2 = Order.create();
order2.addItems(List.of(usdItem1, usdItem2));  // ✅ Валидация
order2.addItems(List.of(eurItem));  // ❌ Exception

// Путь 3: создание через createWithItems()
Order order3 = Order.createWithItems(List.of(usdItem, eurItem));  // ❌ Exception
```

## Тесты

Существующие тесты проверяют все сценарии:

### Test 12: `shouldRejectAddingItemsWithDifferentCurrency()`
- Проверяет `addItem()` ✅

### Test 13: `shouldRejectAddingMultipleItemsWithDifferentCurrency()`
- Проверяет `addItems()` ✅

### Test 14: `shouldAllowAddingItemsWithSameCurrency()`
- Проверяет что одинаковая валюта работает ✅

### Test 15: `shouldCalculateTotalInCorrectCurrency()`
- Проверяет `createWithItems()` и `calculateTotal()` ✅

## Архитектурная гарантия

### Инвариант на уровне Aggregate Root:

```
ИНВАРИАНТ: Все OrderItems в Order имеют одинаковую валюту
```

**Защита инварианта:**
1. ✅ Валидация в `addItem()` - один элемент
2. ✅ Валидация в `addItems()` - коллекция элементов
3. ✅ Валидация в `createWithItems()` - фабричный метод
4. ✅ `removeItem()` / `removeItems()` - не нарушают инвариант
5. ✅ `calculateTotal()` - использует валюту первого item

## Итог

✅ **Все методы добавления items теперь контролируют валюту заказа**

| До исправления | После исправления |
|----------------|------------------|
| ❌ `addItem()` - нет валидации | ✅ `addItem()` - есть валидация |
| ✅ `addItems()` - есть валидация | ✅ `addItems()` - есть валидация |
| ❌ `createWithItems()` - нет валидации | ✅ `createWithItems()` - есть валидация |

**Статус:** ✅ Задача выполнена полностью


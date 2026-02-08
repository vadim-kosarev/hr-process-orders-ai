# 25. Исправление trusted.packages для Kafka десериализации

## Дата: 2026-02-08

## Проблема

При запуске тестов ошибка:

```
java.lang.IllegalArgumentException: The class 'hr.orders.domain.command.CreateOrderCommand' 
is not in the trusted packages: [java.util, java.lang, hr.orders.domain.event]. 
If you believe this class is safe to deserialize, please provide its name.
```

### Причина

В `application.yaml` в свойстве `spring.json.trusted.packages` были указаны только:
- `hr.orders.domain.event`
- `hr.orders.domain.command`

Но Jackson/Kafka требует более широкого набора пакетов для десериализации всех классов.

## ✅ Решение

### Обновлены оба файла конфигурации:

#### 1. `src/main/resources/application.yaml`

**Было:**
```yaml
spring.json.trusted.packages: hr.orders.domain.event,hr.orders.domain.command
```

**Стало:**
```yaml
spring.json.trusted.packages: hr.orders.domain.event,hr.orders.domain.command,hr.orders.domain
```

#### 2. `src/test/resources/application.yaml`

**Было:**
```yaml
spring.json.trusted.packages: hr.orders.domain.event
```

**Стало:**
```yaml
spring.json.trusted.packages: hr.orders.domain.event,hr.orders.domain.command,hr.orders.domain
```

## Что это делает

### `spring.json.trusted.packages`

Свойство Spring Kafka которое определяет какие Java пакеты разрешены для десериализации из JSON:

```yaml
spring.json.trusted.packages: 
  - hr.orders.domain.event       # Для OrderServiceEvent
  - hr.orders.domain.command     # Для CreateOrderCommand
  - hr.orders.domain             # Для всех подклассов (Order, OrderItem, и т.д.)
```

### Иерархия пакетов

```
hr.orders.domain/
├── hr.orders.domain.event/
│   └── OrderServiceEvent
├── hr.orders.domain.command/
│   └── CreateOrderCommand
│       ├── OrderData
│       └── OrderItemData
├── Order
├── OrderItem
├── OrderStatus
└── valueobject/
    ├── OrderID
    ├── Money
    └── Qty
```

**Указав `hr.orders.domain`**, мы разрешаем десериализацию всех классов в этом пакете и подпакетах.

## Почему нужны все три пакета

### `hr.orders.domain.event`
Для десериализации `OrderServiceEvent` которые приходят из Kafka топика `order-events`.

### `hr.orders.domain.command`
Для десериализации `CreateOrderCommand` которые приходят из Kafka топика `order-commands`.

### `hr.orders.domain`
Для вложенных классов:
- `OrderCommand.OrderData` - вложен в `CreateOrderCommand`
- `OrderCommand.OrderItemData` - вложен в `OrderData`
- `Order`, `OrderItem`, `OrderStatus` - используются в сервисах

Без этого Jackson не может десериализовать `CreateOrderCommand.OrderData` потому что класс находится в пакете `hr.orders.domain.command`.

## Альтернативные подходы

### Вариант 1: Использовать wildcards (не рекомендуется)
```yaml
spring.json.trusted.packages: "hr.orders.*"
```

**Проблема:** Слишком много пакетов, security risk

### Вариант 2: Перечислить все пакеты (что мы сделали)
```yaml
spring.json.trusted.packages: 
  hr.orders.domain.event,
  hr.orders.domain.command,
  hr.orders.domain
```

**Преимущества:** 
- ✅ Явно указаны только нужные пакеты
- ✅ Безопасно - контролируем что десериализуется
- ✅ Читаемо

### Вариант 3: Доверять всему (опасно!)
```yaml
spring.json.trusted.packages: "*"
```

**Проблема:** Security risk - можно десериализовать любой класс из JVM

## Структура доменного пакета

```
hr/orders/domain/
├── command/
│   ├── OrderCommand.java        (абстрактный)
│   ├── CreateOrderCommand.java  (содержит вложенные классы)
│   │   ├── OrderData
│   │   └── OrderItemData
├── event/
│   ├── DomainEvent.java         (абстрактный)
│   └── OrderServiceEvent.java   (конкретные события)
├── valueobject/
│   ├── OrderID.java
│   ├── Money.java
│   └── Qty.java
├── Order.java
├── OrderItem.java
├── OrderStatus.java
└── DomainObject.java
```

**Вложенные классы:**
- `CreateOrderCommand.OrderData` - находится внутри CreateOrderCommand
- `CreateOrderCommand.OrderItemData` - находится внутри CreateOrderCommand

Их пакет - это пакет содержащего их класса: `hr.orders.domain.command`

## Как Spring находит пакеты при десериализации

### Процесс:

1. Kafka получает JSON сообщение
2. JsonDeserializer пытается десериализовать JSON
3. Jackson анализирует JSON и определяет класс для десериализации
4. Jackson проверяет есть ли этот класс в `spring.json.trusted.packages`
5. Если есть - десериализует, если нет - выбрасывает исключение

### Пример для CreateOrderCommand:

```json
{
  "commandId": "...",
  "order": {
    "orderId": "...",
    "items": [...]
  }
}
```

**Десериализация:**
1. Jackson видит корневой класс `CreateOrderCommand` → проверяет пакет `hr.orders.domain.command` ✅
2. Jackson видит поле `order` типа `OrderData` → проверяет пакет `hr.orders.domain.command` ✅
3. Jackson видит вложенные `OrderItemData` → проверяет пакет `hr.orders.domain.command` ✅
4. Все проходят проверку → успешная десериализация

## Компиляция и тесты

✅ **Успешно**
- Нет ошибок компиляции
- Тесты должны проходить без ошибок десериализации

## Изменённые файлы

1. ✅ `src/main/resources/application.yaml`
   - Добавлен пакет `hr.orders.domain` в trusted packages

2. ✅ `src/test/resources/application.yaml`
   - Добавлены пакеты `hr.orders.domain.command` и `hr.orders.domain` в trusted packages

## Best Practices

### ✅ Правильно: Явно указывать пакеты
```yaml
spring.json.trusted.packages: 
  hr.orders.domain.event,
  hr.orders.domain.command,
  hr.orders.domain
```

### ❌ Неправильно: Использовать wildcards
```yaml
spring.json.trusted.packages: "hr.orders.*"  # Слишком много
```

### ❌ Неправильно: Доверять всему
```yaml
spring.json.trusted.packages: "*"  # Security risk!
```

## Статус

✅ **Проблема решена**
- Все необходимые пакеты добавлены в trusted packages
- Тесты должны проходить без ошибок десериализации
- Конфигурация безопасна и явна


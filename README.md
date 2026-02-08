# Обработка заказов
В данном проекте реализована система обработки заказов для интернет-магазина. Система позволяет пользователям создавать заказы, а администраторам - управлять ими.

## Контекст

```
Пользователь
    |
    |---> Создание заказа
    |---> Просмотр заказов
    |---> Отмена заказа
    |
    v
    ORDER SEVICE
```

## Контейнеры

```
ORDER SERVICE
    |
    |---> Веб-приложение (Spring Boot)
    |---> REDIS
    |---> KAFKA
    |---> База данных (PostgreSQL)
```

## Компоненты

```
ORDER SERVICE
    |
    |---> OrderController
    |---> OrdersDeduplicationRequesService
    |---> OrderService
    |---> OrdersQueueAdapter
    |---> OrderRepository
    |---> Order (Domain Model, JPA Entity)
```

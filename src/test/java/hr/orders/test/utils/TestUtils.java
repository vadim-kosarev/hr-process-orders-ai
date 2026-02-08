package hr.orders.test.utils;

import hr.orders.domain.Order;
import hr.orders.domain.OrderItem;
import hr.orders.domain.command.CreateOrderCommand;
import hr.orders.domain.valueobject.Money;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.domain.valueobject.Qty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for creating test data
 */
public class TestUtils {


    /**
     * Create a test CreateOrderCommand with default items
     * @return CreateOrderCommand with 2 items
     */
    public static CreateOrderCommand createTestOrderCommand() {
        return createTestOrderCommandWithItems(2);
    }

    /**
     * Create a test CreateOrderCommand with specified number of items
     * @param itemCount number of items to create
     * @return CreateOrderCommand
     */
    public static CreateOrderCommand createTestOrderCommandWithItems(int itemCount) {
        UUID orderId = UUID.randomUUID();
        List<CreateOrderCommand.OrderItemData> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            CreateOrderCommand.OrderItemData itemData = new CreateOrderCommand.OrderItemData(
                UUID.randomUUID(),
                2 + i,
                BigDecimal.valueOf(100.0 + (i * 50)),
                "USD"
            );
            items.add(itemData);
        }

        CreateOrderCommand.OrderData orderData = new CreateOrderCommand.OrderData(
            orderId,
            "NEW",
            items,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return new CreateOrderCommand(orderData);
    }

    /**
     * Create CreateOrderCommand from existing Order
     * @param order Order to convert
     * @return CreateOrderCommand
     */
    public static CreateOrderCommand createCommandFromOrder(Order order) {
        List<CreateOrderCommand.OrderItemData> items = order.getOrderItems().stream()
            .map(item -> new CreateOrderCommand.OrderItemData(
                item.getProductId(),
                item.getQuantity().getValue(),
                item.getUnitPrice().getAmount(),
                item.getUnitPrice().getCurrencyCode()
            ))
            .collect(Collectors.toList());

        CreateOrderCommand.OrderData orderData = new CreateOrderCommand.OrderData(
            order.getOrderId().getValue(),
            order.getStatus().name(),
            items,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );

        return new CreateOrderCommand(orderData);
    }

    /**
     * Create a test order with default items
     * @return Order with 3 items
     */
    public static Order createTestOrder() {
        return createTestOrderWithItems(3);
    }

    /**
     * Create a test order with specified number of items
     * @param itemCount number of items to create
     * @return Order with items
     */
    public static Order createTestOrderWithItems(int itemCount) {
        List<OrderItem> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            OrderItem item = createTestOrderItem(
                UUID.randomUUID(),
                2 + i,
                100.0 + (i * 50),
                "USD"
            );
            items.add(item);
        }

        return Order.createWithItems(OrderID.generate(), items);
    }

    /**
     * Create a test order item
     * @param productId product ID
     * @param quantity quantity
     * @param price unit price
     * @param currency currency code
     * @return OrderItem
     */
    public static OrderItem createTestOrderItem(UUID productId, int quantity, double price, String currency) {
        return OrderItem.create(
            productId,
            Qty.of(quantity),
            Money.of(price, currency)
        );
    }

    /**
     * Create a simple test order with predefined items
     * @return Order with 2 simple items
     */
    public static Order createSimpleTestOrder() {
        OrderItem item1 = createTestOrderItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            2,
            100.0,
            "USD"
        );

        OrderItem item2 = createTestOrderItem(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
            1,
            50.0,
            "USD"
        );

        List<OrderItem> items = List.of(item1, item2);
        return Order.createWithItems(OrderID.generate(),
                items);
    }

    /**
     * Create a test order with EUR currency
     * @return Order with EUR items
     */
    public static Order createEurTestOrder() {
        OrderItem item1 = createTestOrderItem(
            UUID.randomUUID(),
            1,
            10.50,
            "EUR"
        );

        OrderItem item2 = createTestOrderItem(
            UUID.randomUUID(),
            2,
            20.0,
            "EUR"
        );

        List<OrderItem> items = List.of(item1, item2);
        return Order.createWithItems(OrderID.generate(), items);
    }
}


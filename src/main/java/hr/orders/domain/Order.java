package hr.orders.domain;

import hr.orders.domain.event.*;
import hr.orders.domain.valueobject.Money;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.domain.valueobject.Qty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Order aggregate root
 * Represents an order in the system with its items
 * Encapsulates business logic and invariants
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "orderItems")
@EqualsAndHashCode(of = "orderId")
public class Order extends DomainObject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "order_id", nullable = false, unique = true))
    })
    private OrderID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Order create() {
        return create(OrderID.generate());
    }

    public static Order create(OrderID id) {
        Order order = new Order();
        order.orderId = id;
        order.status = OrderStatus.NEW;
        order.createdAt = LocalDateTime.now();
        order.updatedAt = LocalDateTime.now();
        order.orderItems = new ArrayList<>();
        order.raiseEvent(new OrderCreatedEvent(order.orderId));
        return order;
    }

    public static Order createWithItems(OrderID id, List<OrderItem> items) {
        Order order = create(id);
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                order.validateItemCurrency(item);
            }
            order.orderItems.addAll(items);
        }
        return order;
    }

    public boolean isEditable() {
        return status == OrderStatus.NEW;
    }

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

    public void removeItem(OrderItem item) {
        if (item == null)
            throw new IllegalArgumentException("Order item cannot be null");
        if (!isEditable())
            throw new IllegalStateException("Cannot edit order in status: " + status);
        orderItems.remove(item);
        updateTimestamp();
    }

    public void removeItems(Collection<OrderItem> itemsToRemove) {
        if (itemsToRemove == null || itemsToRemove.isEmpty())
            throw new IllegalArgumentException("Order items cannot be null or empty");
        if (itemsToRemove.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Order items collection cannot contain null elements");
        if (!isEditable())
            throw new IllegalStateException("Cannot edit order in status: " + status);
        if (!orderItems.containsAll(itemsToRemove))
            throw new IllegalArgumentException("Order does not contain all specified items");

        for (OrderItem itemToRemove : itemsToRemove) {
            orderItems.removeIf(existingItem -> itemToRemove == existingItem);
        }
        updateTimestamp();
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

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

    public Qty calculateTotalQuantity() {
        return orderItems.stream()
                .map(OrderItem::getQuantity)
                .reduce(Qty.of(0), Qty::add);
    }

    public void startProcessing() {
        if (!status.canBeProcessed()) {
            throw new IllegalStateException("Cannot process order in status: " + status);
        }
        if (orderItems.isEmpty()) {
            throw new IllegalStateException("Cannot start processing order without items");
        }
        this.status = OrderStatus.IN_PROGRESS;
        updateTimestamp();

        raiseEvent(new OrderProcessingStartedEvent(orderId));
    }

    public void complete() {
        if (!status.canBeMarkedAsReady()) {
            throw new IllegalStateException("Can only mark IN_PROGRESS orders as READY");
        }
        this.status = OrderStatus.READY;
        updateTimestamp();
        raiseEvent(new OrderReadyEvent(orderId));
    }

    public void markAsFailed() {
        if (!status.canBeFailed()) {
            throw new IllegalStateException("Cannot fail order in status: " + status);
        }
        this.status = OrderStatus.FAILED;
        updateTimestamp();
        raiseEvent(new OrderProcessingFailedEvent(orderId, "Somthing was WRONG"));
    }

    public void cancel() {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        updateTimestamp();
        raiseEvent(new OrderCancelledEvent(orderId));
    }

    public boolean hasItems() {
        return !orderItems.isEmpty();
    }

    public boolean containsProduct(UUID productId) {
        return orderItems.stream()
                .anyMatch(item -> item.getProductId().equals(productId));
    }

    public int getItemCount() {
        return orderItems.size();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get currency of the order (from first item)
     * @return currency code or null if no items
     */
    private String getOrderCurrency() {
        if (orderItems.isEmpty()) {
            return null;
        }
        return orderItems.get(0).getUnitPrice().getCurrencyCode();
    }

    /**
     * Validate that item currency matches order currency
     * @param item item to validate
     * @throws IllegalArgumentException if currencies don't match
     */
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

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}


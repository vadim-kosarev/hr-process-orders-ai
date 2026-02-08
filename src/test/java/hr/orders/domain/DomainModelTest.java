package hr.orders.domain;

import hr.orders.domain.valueobject.Money;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.domain.valueobject.Qty;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain Model Test
 * Tests for domain entities and value objects
 * Covers both valid and invalid cases
 */
class DomainModelTest {

    private static final Logger logger = LoggerFactory.getLogger(DomainModelTest.class);

    @Test
    void shouldCreateValidOrderWithItemsAndCalculateTotal() {
        logger.info("=== Test 1: Create valid order with items and calculate total ===");

        // Given: Valid order items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        Qty qty1 = Qty.of(2);
        Qty qty2 = Qty.of(3);
        Money price1 = Money.of(100.0, "USD");
        Money price2 = Money.of(50.0, "USD");

        logger.info("Creating order item 1: productId={}, qty={}, price={}", productId1, qty1, price1);
        OrderItem item1 = OrderItem.create(productId1, qty1, price1);

        logger.info("Creating order item 2: productId={}, qty={}, price={}", productId2, qty2, price2);
        OrderItem item2 = OrderItem.create(productId2, qty2, price2);

        // When: Create order and add items
        logger.info("Creating order and adding items");
        Order order = Order.create();
        order.addItem(item1);
        order.addItem(item2);

        // Then: Order should be created with correct state
        logger.info("Verifying order state: orderId={}, status={}, itemCount={}",
            order.getOrderId(), order.getStatus(), order.getItemCount());
        assertNotNull(order.getOrderId());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(2, order.getItemCount());
        assertTrue(order.hasItems());

        // And: Total should be calculated correctly
        Money expectedTotal = Money.of(350.0, "USD"); // (2*100) + (3*50) = 350
        Money actualTotal = order.calculateTotal();
        logger.info("Total calculation: expected={}, actual={}", expectedTotal, actualTotal);
        assertEquals(expectedTotal, actualTotal);

        // And: Total quantity should be calculated correctly
        Qty expectedQty = Qty.of(5); // 2 + 3 = 5
        Qty actualQty = order.calculateTotalQuantity();
        logger.info("Total quantity: expected={}, actual={}", expectedQty, actualQty);
        assertEquals(expectedQty, actualQty);

        logger.info("=== Test 1: PASSED ===");
    }

    @Test
    void shouldTransitionOrderThroughValidStatusFlow() {
        logger.info("=== Test 2: Transition order through valid status flow ===");

        // Given: Order with items
        Order order = Order.create();
        OrderItem item = OrderItem.create(
            UUID.randomUUID(),
            Qty.of(1),
            Money.of(100.0, "USD")
        );
        order.addItem(item);

        // When/Then: Follow valid status transitions
        logger.info("Initial status: {}", order.getStatus());
        assertEquals(OrderStatus.NEW, order.getStatus());

        // NEW -> IN_PROGRESS
        logger.info("Transitioning NEW -> IN_PROGRESS");
        order.startProcessing();
        logger.info("Status after startProcessing: {}", order.getStatus());
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

        // IN_PROGRESS -> READY
        logger.info("Transitioning IN_PROGRESS -> READY");
        order.complete();
        logger.info("Status after markAsReady: {}, isFinal: {}", order.getStatus(), order.getStatus().isFinal());
        assertEquals(OrderStatus.READY, order.getStatus());
        assertTrue(order.getStatus().isFinal());

        logger.info("=== Test 2: PASSED ===");
    }

    @Test
    void shouldRejectInvalidOrderItemCreation() {
        logger.info("=== Test 3: Reject invalid order item creation ===");

        // Test 1: Null product ID
        logger.info("Test 3.1: Attempting to create OrderItem with null productId");
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(null, Qty.of(1), Money.of(100.0, "USD"));
        });
        logger.info("Test 3.1: Correctly rejected null productId");

        // Test 2: Null quantity
        logger.info("Test 3.2: Attempting to create OrderItem with null quantity");
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(UUID.randomUUID(), null, Money.of(100.0, "USD"));
        });
        logger.info("Test 3.2: Correctly rejected null quantity");

        // Test 3: Null unit price
        logger.info("Test 3.3: Attempting to create OrderItem with null unitPrice");
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(UUID.randomUUID(), Qty.of(1), null);
        });
        logger.info("Test 3.3: Correctly rejected null unitPrice");

        // Test 4: Zero quantity
        logger.info("Test 3.4: Attempting to create OrderItem with zero quantity");
        assertThrows(IllegalArgumentException.class, () -> {
            OrderItem.create(UUID.randomUUID(), Qty.of(0), Money.of(100.0, "USD"));
        });
        logger.info("Test 3.4: Correctly rejected zero quantity");

        // Test 5: Negative quantity
        logger.info("Test 3.5: Attempting to create Qty with negative value");
        assertThrows(IllegalArgumentException.class, () -> {
            Qty.of(-5);
        });
        logger.info("Test 3.5: Correctly rejected negative quantity");

        logger.info("=== Test 3: PASSED ===");
    }

    @Test
    void shouldRejectInvalidOrderStatusTransitions() {
        logger.info("=== Test 4: Reject invalid order status transitions ===");

        // Given: Order in NEW status
        Order order = Order.create();
        OrderItem item = OrderItem.create(
            UUID.randomUUID(),
            Qty.of(1),
            Money.of(100.0, "USD")
        );
        order.addItem(item);
        logger.info("Order created with status: {}", order.getStatus());

        // Test 1: Cannot mark as READY from NEW (must be IN_PROGRESS)
        logger.info("Test 4.1: Attempting to markAsReady from NEW status");
        assertThrows(IllegalStateException.class, () -> {
            order.complete();
        });
        logger.info("Test 4.1: Correctly rejected markAsReady from NEW");

        // Test 2: Cannot start processing without items
        logger.info("Test 4.2: Testing startProcessing with items");
        order.startProcessing(); // Should work - we have items
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        logger.info("Test 4.2: Successfully transitioned to IN_PROGRESS");

        // Given: Order in READY status
        order.complete();
        logger.info("Order transitioned to READY status");

        // Test 3: Cannot cancel READY order
        logger.info("Test 4.3: Attempting to cancel READY order");
        assertThrows(IllegalStateException.class, () -> {
            order.cancel();
        });
        logger.info("Test 4.3: Correctly rejected cancel of READY order");

        // Test 4: Cannot add items to READY order
        logger.info("Test 4.4: Attempting to add item to READY order");
        OrderItem newItem = OrderItem.create(
            UUID.randomUUID(),
            Qty.of(1),
            Money.of(50.0, "USD")
        );
        assertThrows(IllegalStateException.class, () -> {
            order.addItem(newItem);
        });
        logger.info("Test 4.4: Correctly rejected adding item to READY order");

        logger.info("=== Test 4: PASSED ===");
    }

    @Test
    void shouldValidateValueObjectsCorrectly() {
        logger.info("=== Test 5: Validate value objects correctly ===");

        // Test Money value object
        logger.info("Test 5.1: Testing Money value object");
        Money money1 = Money.of(100.0, "USD");
        Money money2 = Money.of(50.0, "USD");
        Money money3 = Money.of(100.0, "EUR");
        logger.info("Created: money1={}, money2={}, money3={}", money1, money2, money3);

        // Valid money operations
        Money sum = money1.add(money2);
        logger.info("money1.add(money2) = {}", sum);
        assertEquals(Money.of(150.0, "USD"), sum);

        Money difference = money1.subtract(money2);
        logger.info("money1.subtract(money2) = {}", difference);
        assertEquals(Money.of(50.0, "USD"), difference);

        logger.info("money1.isPositive() = {}", money1.isPositive());
        assertTrue(money1.isPositive());

        Money zeroMoney = Money.of(0.0, "USD");
        logger.info("zeroMoney.isPositive() = {}", zeroMoney.isPositive());
        assertFalse(zeroMoney.isPositive());

        // Invalid money operations - different currencies
        logger.info("Test 5.2: Testing Money with different currencies (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            money1.add(money3);
        });
        logger.info("Correctly rejected adding different currencies");

        // Test Qty value object
        logger.info("Test 5.3: Testing Qty value object");
        Qty qty1 = Qty.of(5);
        Qty qty2 = Qty.of(3);
        logger.info("Created: qty1={}, qty2={}", qty1, qty2);

        // Valid quantity operations
        Qty qtySum = qty1.add(qty2);
        logger.info("qty1.add(qty2) = {}", qtySum);
        assertEquals(Qty.of(8), qtySum);

        Qty qtyDiff = qty1.subtract(qty2);
        logger.info("qty1.subtract(qty2) = {}", qtyDiff);
        assertEquals(Qty.of(2), qtyDiff);

        logger.info("qty1.isPositive() = {}", qty1.isPositive());
        assertTrue(qty1.isPositive());

        Qty zeroQty = Qty.of(0);
        logger.info("Testing Qty.of(0).isZero() = {}", zeroQty.isZero());
        // BUG FIX: Qty со значением 0 ДОЛЖНО быть zero, поэтому assertTrue!
        assertTrue(zeroQty.isZero(), "Qty with value 0 should return true for isZero()");

        // Invalid quantity - negative
        logger.info("Test 5.4: Testing negative Qty (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            Qty.of(-1);
        });
        logger.info("Correctly rejected negative quantity");

        // Test OrderID value object
        logger.info("Test 5.5: Testing OrderID value object");
        UUID uuid = UUID.randomUUID();
        OrderID orderId1 = OrderID.of(uuid);
        OrderID orderId2 = OrderID.of(uuid);
        OrderID orderId3 = OrderID.generate();
        logger.info("Created: orderId1={}, orderId2={}, orderId3={}", orderId1, orderId2, orderId3);

        // OrderID equality
        logger.info("orderId1.equals(orderId2) = {}", orderId1.equals(orderId2));
        assertEquals(orderId1, orderId2);

        logger.info("orderId1.equals(orderId3) = {}", orderId1.equals(orderId3));
        assertNotEquals(orderId1, orderId3);

        logger.info("orderId1.asString() = {}", orderId1.asString());
        assertEquals(uuid.toString(), orderId1.asString());

        // Invalid OrderID
        logger.info("Test 5.6: Testing null OrderID (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            OrderID.of((UUID) null);
        });
        logger.info("Correctly rejected null UUID");

        logger.info("=== Test 5: PASSED ===");
    }

    @Test
    void shouldAddMultipleItemsAtOnce() {
        logger.info("=== Test 6: Add multiple items at once using addItems() ===");

        // Given: Multiple order items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        logger.info("Creating 3 order items");
        OrderItem item1 = OrderItem.create(productId1, Qty.of(2), Money.of(100.0, "USD"));
        OrderItem item2 = OrderItem.create(productId2, Qty.of(3), Money.of(50.0, "USD"));
        OrderItem item3 = OrderItem.create(productId3, Qty.of(1), Money.of(200.0, "USD"));

        java.util.List<OrderItem> items = java.util.Arrays.asList(item1, item2, item3);

        // When: Create order and add all items at once
        logger.info("Creating order and adding all items using addItems()");
        Order order = Order.create();
        order.addItems(items);

        // Then: Order should contain all items
        logger.info("Verifying order contains all 3 items");
        assertEquals(3, order.getItemCount());
        assertTrue(order.containsProduct(productId1));
        assertTrue(order.containsProduct(productId2));
        assertTrue(order.containsProduct(productId3));

        // And: Total should be calculated correctly
        Money expectedTotal = Money.of(550.0, "USD"); // 200 + 150 + 200
        Money actualTotal = order.calculateTotal();
        logger.info("Total calculated: expected={}, actual={}", expectedTotal, actualTotal);
        assertEquals(expectedTotal.getAmount().doubleValue(), actualTotal.getAmount().doubleValue(), 0.01);

        logger.info("=== Test 6: PASSED ===");
    }

    @Test
    void shouldRejectAddItemsWithNullOrEmptyCollection() {
        logger.info("=== Test 7: Reject addItems with null or empty collection ===");

        Order order = Order.create();

        // Test null collection
        logger.info("Test 7.1: Testing addItems with null collection (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            order.addItems(null);
        });
        logger.info("Correctly rejected null collection");

        // Test empty collection
        logger.info("Test 7.2: Testing addItems with empty collection (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            order.addItems(java.util.Collections.emptyList());
        });
        logger.info("Correctly rejected empty collection");

        // Test collection with null element
        logger.info("Test 7.3: Testing addItems with null element (should throw)");
        OrderItem validItem = OrderItem.create(UUID.randomUUID(), Qty.of(1), Money.of(100.0, "USD"));
        java.util.List<OrderItem> itemsWithNull = java.util.Arrays.asList(validItem, null);
        assertThrows(IllegalArgumentException.class, () -> {
            order.addItems(itemsWithNull);
        });
        logger.info("Correctly rejected collection with null element");

        logger.info("=== Test 7: PASSED ===");
    }

    @Test
    void shouldRemoveMultipleItemsAtOnce() {
        logger.info("=== Test 8: Remove multiple items using removeItems() ===");

        // Given: Order with 5 items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();
        UUID productId4 = UUID.randomUUID();
        UUID productId5 = UUID.randomUUID();

        logger.info("Creating order with 5 items");
        OrderItem item1 = OrderItem.create(productId1, Qty.of(1), Money.of(100.0, "USD"));
        OrderItem item2 = OrderItem.create(productId2, Qty.of(2), Money.of(50.0, "USD"));
        OrderItem item3 = OrderItem.create(productId3, Qty.of(3), Money.of(75.0, "USD"));
        OrderItem item4 = OrderItem.create(productId4, Qty.of(1), Money.of(200.0, "USD"));
        OrderItem item5 = OrderItem.create(productId5, Qty.of(2), Money.of(30.0, "USD"));

        Order order = Order.create();
        order.addItems(java.util.Arrays.asList(item1, item2, item3, item4, item5));

        logger.info("Order created with {} items", order.getItemCount());
        assertEquals(5, order.getItemCount());

        logger.info("Removing items 2 and 4 from order");

        java.util.List<OrderItem> currentItems = new java.util.ArrayList<>(order.getOrderItems());
        OrderItem itemToRemove1 = currentItems.stream().filter(i -> i.getProductId().equals(productId2)).findFirst().orElseThrow(() -> new RuntimeException("Item with productId2 not found"));
        OrderItem itemToRemove2 = currentItems.stream().filter(i -> i.getProductId().equals(productId4)).findFirst().orElseThrow(() -> new RuntimeException("Item with productId4 not found"));
        java.util.List<OrderItem> itemsToRemove = java.util.Arrays.asList(itemToRemove1, itemToRemove2);
        order.removeItems(itemsToRemove);

        // Then: Order should have 3 remaining items
        logger.info("After removal: itemCount={}", order.getItemCount());
        assertEquals(3, order.getItemCount());

        // And: Removed products should not be in order
        logger.info("Verifying removed products are not in order");
        assertFalse(order.containsProduct(productId2));
        assertFalse(order.containsProduct(productId4));

        // And: Remaining products should still be in order
        logger.info("Verifying remaining products are still in order");
        assertTrue(order.containsProduct(productId1));
        assertTrue(order.containsProduct(productId3));
        assertTrue(order.containsProduct(productId5));

        // And: Total should be recalculated correctly
        Money expectedTotal = Money.of(385.0, "USD"); // 100 + 225 + 60 (removed: 100 + 200)
        Money actualTotal = order.calculateTotal();
        logger.info("Total after removal: expected={}, actual={}", expectedTotal, actualTotal);
        assertEquals(expectedTotal.getAmount().doubleValue(), actualTotal.getAmount().doubleValue(), 0.01);

        logger.info("=== Test 8: PASSED ===");
    }

    @Test
    void shouldRejectRemoveItemsWithNullOrEmptyCollection() {
        logger.info("=== Test 9: Reject removeItems with null or empty collection ===");

        Order order = Order.create();
        OrderItem item = OrderItem.create(UUID.randomUUID(), Qty.of(1), Money.of(100.0, "USD"));
        order.addItem(item);

        // Test null collection
        logger.info("Test 9.1: Testing removeItems with null collection (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            order.removeItems(null);
        });
        logger.info("Correctly rejected null collection");

        // Test empty collection
        logger.info("Test 9.2: Testing removeItems with empty collection (should throw)");
        assertThrows(IllegalArgumentException.class, () -> {
            order.removeItems(java.util.Collections.emptyList());
        });
        logger.info("Correctly rejected empty collection");

        // Test collection with null element
        logger.info("Test 9.3: Testing removeItems with null element (should throw)");
        java.util.List<OrderItem> itemsWithNull = java.util.Arrays.asList(item, null);
        assertThrows(IllegalArgumentException.class, () -> {
            order.removeItems(itemsWithNull);
        });
        logger.info("Correctly rejected collection with null element");

        logger.info("=== Test 9: PASSED ===");
    }

    @Test
    void shouldRejectAddOrRemoveItemsWhenOrderIsNotEditable() {
        logger.info("=== Test 10: Reject add/remove items when order is not editable ===");

        // Given: Order with items in IN_PROGRESS status
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        OrderItem item1 = OrderItem.create(productId1, Qty.of(1), Money.of(100.0, "USD"));
        OrderItem item2 = OrderItem.create(productId2, Qty.of(2), Money.of(50.0, "USD"));

        Order order = Order.create();
        order.addItem(item1);
        logger.info("Order created with 1 item, status={}", order.getStatus());

        // Move to IN_PROGRESS
        order.startProcessing();
        logger.info("Order status changed to {}", order.getStatus());
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());

        // When/Then: Try to add items (should fail)
        logger.info("Test 10.1: Attempting to add items to IN_PROGRESS order (should throw)");
        java.util.List<OrderItem> itemsToAdd = java.util.Arrays.asList(item2);
        assertThrows(IllegalStateException.class, () -> {
            order.addItems(itemsToAdd);
        });
        logger.info("Correctly rejected addItems on non-editable order");

        // When/Then: Try to remove items (should fail)
        logger.info("Test 10.2: Attempting to remove items from IN_PROGRESS order (should throw)");
        java.util.List<OrderItem> itemsToRemove = java.util.Arrays.asList(item1);
        assertThrows(IllegalStateException.class, () -> {
            order.removeItems(itemsToRemove);
        });
        logger.info("Correctly rejected removeItems on non-editable order");

        logger.info("=== Test 10: PASSED ===");
    }

    @Test
    void shouldHandleRemoveItemsByReference() {
        logger.info("=== Test 11: Remove items by reference - understanding collection equality ===");

        // Given: Order with items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        logger.info("Creating order with 3 items");
        OrderItem item1 = OrderItem.create(productId1, Qty.of(1), Money.of(100.0, "USD"));
        OrderItem item2 = OrderItem.create(productId2, Qty.of(2), Money.of(50.0, "USD"));
        OrderItem item3 = OrderItem.create(productId3, Qty.of(3), Money.of(75.0, "USD"));

        Order order = Order.create();
        order.addItems(java.util.Arrays.asList(item1, item2, item3));

        // Get items from order (these are the actual objects in the order)
        java.util.List<OrderItem> orderItems = new java.util.ArrayList<>(order.getOrderItems());
        logger.info("Retrieved {} items from order", orderItems.size());

        // When: Remove first item using reference from getOrderItems()
        logger.info("Removing first item using reference from order.getOrderItems()");
        OrderItem itemToRemove = orderItems.get(0);
        order.removeItem(itemToRemove);

        // Then: Item should be removed
        logger.info("After removal: itemCount={}", order.getItemCount());
        assertEquals(2, order.getItemCount());

        // Demonstrate: For removeItems() we need the exact same object references
        logger.info("=== Demonstrating removeItems() with references ===");

        // Get current items
        java.util.List<OrderItem> currentItems = new java.util.ArrayList<>(order.getOrderItems());
        logger.info("Current items count: {}", currentItems.size());

        // Remove last 2 items using their references
        java.util.List<OrderItem> toRemove = java.util.Arrays.asList(
            currentItems.get(0),
            currentItems.get(1)
        );

        logger.info("Removing {} items", toRemove.size());
        order.removeItems(toRemove);

        // Then: Order should be empty
        logger.info("After removal: itemCount={}", order.getItemCount());
        assertEquals(0, order.getItemCount());
        assertFalse(order.hasItems());

        logger.info("=== Test 11: PASSED ===");
        logger.info("KEY INSIGHT: To remove items, use the same object references from order.getOrderItems()");
    }

    @Test
    void shouldRejectAddingItemsWithDifferentCurrency() {
        logger.info("=== Test 12: Reject adding items with different currency ===");

        // Given: Order with USD items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        logger.info("Creating order with USD items");
        OrderItem usdItem1 = OrderItem.create(productId1, Qty.of(2), Money.of(100.0, "USD"));
        OrderItem usdItem2 = OrderItem.create(productId2, Qty.of(1), Money.of(50.0, "USD"));

        Order order = Order.create();
        order.addItem(usdItem1);
        order.addItem(usdItem2);

        logger.info("Order created with 2 USD items, total: {}", order.calculateTotal());
        assertEquals("USD", order.calculateTotal().getCurrencyCode());

        // When/Then: Try to add EUR item (should fail)
        UUID productId3 = UUID.randomUUID();
        OrderItem eurItem = OrderItem.create(productId3, Qty.of(1), Money.of(75.0, "EUR"));

        logger.info("Attempting to add EUR item to USD order (should throw)");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            order.addItem(eurItem);
        });

        logger.info("Exception message: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Cannot add item with currency EUR"));
        assertTrue(exception.getMessage().contains("order with currency USD"));

        // And: Order should still have only 2 items
        assertEquals(2, order.getItemCount());
        logger.info("Order still has {} items", order.getItemCount());

        logger.info("=== Test 12: PASSED ===");
    }

    @Test
    void shouldRejectAddingMultipleItemsWithDifferentCurrency() {
        logger.info("=== Test 13: Reject addItems() with different currency ===");

        // Given: Order with EUR items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        logger.info("Creating order with EUR items");
        OrderItem eurItem1 = OrderItem.create(productId1, Qty.of(1), Money.of(10.0, "EUR"));
        OrderItem eurItem2 = OrderItem.create(productId2, Qty.of(2), Money.of(20.0, "EUR"));

        Order order = Order.create();
        order.addItems(java.util.Arrays.asList(eurItem1, eurItem2));

        logger.info("Order created with 2 EUR items, total: {}", order.calculateTotal());
        assertEquals("EUR", order.calculateTotal().getCurrencyCode());

        // When/Then: Try to add USD items using addItems() (should fail)
        OrderItem usdItem = OrderItem.create(productId3, Qty.of(1), Money.of(50.0, "USD"));

        logger.info("Attempting to add USD items to EUR order using addItems() (should throw)");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            order.addItems(java.util.Arrays.asList(usdItem));
        });

        logger.info("Exception message: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Cannot add item with currency USD"));
        assertTrue(exception.getMessage().contains("order with currency EUR"));

        // And: Order should still have only 2 items
        assertEquals(2, order.getItemCount());
        assertEquals("EUR", order.calculateTotal().getCurrencyCode());

        logger.info("=== Test 13: PASSED ===");
    }

    @Test
    void shouldAllowAddingItemsWithSameCurrency() {
        logger.info("=== Test 14: Allow adding items with same currency ===");

        // Given: Empty order
        Order order = Order.create();

        // When: Add USD items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        logger.info("Adding first USD item");
        OrderItem usdItem1 = OrderItem.create(productId1, Qty.of(2), Money.of(100.0, "USD"));
        order.addItem(usdItem1);
        assertEquals("USD", order.calculateTotal().getCurrencyCode());

        logger.info("Adding second USD item");
        OrderItem usdItem2 = OrderItem.create(productId2, Qty.of(1), Money.of(50.0, "USD"));
        order.addItem(usdItem2);
        assertEquals("USD", order.calculateTotal().getCurrencyCode());

        logger.info("Adding third USD item using addItems()");
        OrderItem usdItem3 = OrderItem.create(productId3, Qty.of(3), Money.of(25.0, "USD"));
        order.addItems(java.util.Arrays.asList(usdItem3));
        assertEquals("USD", order.calculateTotal().getCurrencyCode());

        // Then: All items should be added successfully
        assertEquals(3, order.getItemCount());

        // And: Total should be in USD
        Money total = order.calculateTotal();
        logger.info("Total: {}", total);
        assertEquals("USD", total.getCurrencyCode());

        // And: Total should be correct (2*100 + 1*50 + 3*25 = 200 + 50 + 75 = 325)
        assertEquals(325.0, total.getAmount().doubleValue(), 0.01);

        logger.info("=== Test 14: PASSED ===");
    }

    @Test
    void shouldCalculateTotalInCorrectCurrency() {
        logger.info("=== Test 15: Calculate total in correct currency for EUR order ===");

        // Given: Order with EUR items
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        logger.info("Creating EUR order");
        OrderItem eurItem1 = OrderItem.create(productId1, Qty.of(2), Money.of(10.50, "EUR"));
        OrderItem eurItem2 = OrderItem.create(productId2, Qty.of(3), Money.of(20.00, "EUR"));

        Order order = Order.createWithItems(
                OrderID.generate(),
                java.util.Arrays.asList(eurItem1, eurItem2));

        // When: Calculate total
        Money total = order.calculateTotal();
        logger.info("Total: {}", total);

        // Then: Total should be in EUR
        assertEquals("EUR", total.getCurrencyCode());

        // And: Total should be correct (2*10.50 + 3*20.00 = 21.00 + 60.00 = 81.00)
        assertEquals(81.00, total.getAmount().doubleValue(), 0.01);

        logger.info("=== Test 15: PASSED ===");
    }
}


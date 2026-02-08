package hr.orders.repository;

import hr.orders.domain.Order;
import hr.orders.domain.OrderStatus;
import hr.orders.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        log.info("=== Cleaning up database before test ===");
        orderRepository.deleteAll();
    }

    @Test
    void testCreateOrder() {
        log.info("=== Test: Create order with multiple items ===");

        // Given: Create test order with 3 items using TestUtils
        log.info("Creating test order with 3 items");
        Order order = TestUtils.createTestOrderWithItems(3);

        log.info("Order created: orderId={}, status={}, itemCount={}",
            order.getOrderId(), order.getStatus(), order.getItemCount());

        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(3, order.getItemCount());
        assertTrue(order.hasItems());

        // When: Save order to repository
        log.info("Saving order to repository");
        Order savedOrder = orderRepository.save(order);

        assertNotNull(savedOrder);
        assertNotNull(savedOrder.getId());
        log.info("Order saved with database ID: {}", savedOrder.getId());

        // Then: Verify order exists in database
        log.info("Verifying order exists in database");
        assertTrue(orderRepository.existsById(savedOrder.getId()));

        // And: Retrieve order by orderId
        log.info("Retrieving order by orderId: {}", savedOrder.getOrderId());
        var foundOrder = orderRepository.findByOrderId(savedOrder.getOrderId().getValue());

        assertTrue(foundOrder.isPresent());
        Order retrievedOrder = foundOrder.get();

        // And: Verify all properties
        log.info("Verifying retrieved order properties");
        assertEquals(savedOrder.getId(), retrievedOrder.getId());
        assertEquals(savedOrder.getOrderId(), retrievedOrder.getOrderId());
        assertEquals(OrderStatus.NEW, retrievedOrder.getStatus());
        assertEquals(3, retrievedOrder.getItemCount());

        log.info("Retrieved order items: {}", retrievedOrder.getOrderItems().size());
        retrievedOrder.getOrderItems().forEach(item ->
            log.info("  - Item: productId={}, qty={}, price={}",
                item.getProductId(), item.getQuantity(), item.getUnitPrice())
        );

        // And: Verify total calculation
        var total = retrievedOrder.calculateTotal();
        log.info("Order total: {}", total);
        assertNotNull(total);
        assertTrue(total.isPositive());

        log.info("=== Test PASSED: Order created and retrieved successfully ===");
    }
}

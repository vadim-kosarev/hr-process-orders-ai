package hr.orders.service;

import hr.orders.domain.Order;
import hr.orders.domain.OrderItem;
import hr.orders.domain.OrderStatus;
import hr.orders.domain.event.*;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String KAFKA_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderServiceEvent> kafkaTemplate;


    /**
     * Create new order with items
     * @param items list of order items
     * @return created order
     */
    @Transactional
    public Order createNewOrder(List<OrderItem> items) {
        log.info("Creating new order with {} items", items.size());

        try {
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Cannot create order without items");
            }

            Order order = Order.createWithItems(items);
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: orderId={}, status={}",
                savedOrder.getOrderId(), savedOrder.getStatus());

            publishEvent(new OrderCreatedEvent(savedOrder.getOrderId()));

            return savedOrder;

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order", e);
        }
    }

    @Transactional
    public Order cancelOrder(UUID orderId) {
        log.info("Cancelling order: orderId={}", orderId);

        try {
            Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            log.info("Found order: orderId={}, currentStatus={}", order.getOrderId(), order.getStatus());

            order.cancel();

            Order cancelledOrder = orderRepository.save(order);
            log.info("Order cancelled successfully: orderId={}, status={}",
                cancelledOrder.getOrderId(), cancelledOrder.getStatus());

            publishEvent(new OrderCancelledEvent(cancelledOrder.getOrderId()));

            return cancelledOrder;

        } catch (IllegalStateException e) {
            log.error("Cannot cancel order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    /**
     * Get order status
     * @param orderId order identifier
     * @return order status
     */
    public OrderStatus getOrderStatus(UUID orderId) {
        log.info("Getting order status: orderId={}", orderId);

        try {
            Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            log.info("Order status retrieved: orderId={}, status={}", order.getOrderId(), order.getStatus());
            return order.getStatus();

        } catch (Exception e) {
            log.error("Error getting order status {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to get order status", e);
        }
    }

    @Transactional
    public void startProcessing(UUID orderId) {
        log.info("Starting processing for order: orderId={}", orderId);

        try {
            Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            log.info("Found order: orderId={}, currentStatus={}", order.getOrderId(), order.getStatus());

            order.startProcessing();

            Order updatedOrder = orderRepository.save(order);
            log.info("Order processing started: orderId={}, status={}",
                updatedOrder.getOrderId(), updatedOrder.getStatus());

            publishEvent(new OrderProcessingStartedEvent(updatedOrder.getOrderId()));

        } catch (IllegalStateException e) {
            log.error("Cannot start processing order {}: {}", orderId, e.getMessage());

            publishEvent(new OrderProcessingFailedEvent(OrderID.of(orderId), e.getMessage()));
            throw e;
        } catch (Exception e) {
            log.error("Error starting processing order {}: {}", orderId, e.getMessage(), e);

            publishEvent(new OrderProcessingFailedEvent(OrderID.of(orderId), e.getMessage()));
            throw new RuntimeException("Failed to start processing order", e);
        }
    }

    /**
     * Mark order as ready (called by event handler)
     * @param orderId order identifier
     */
    @Transactional
    public void markAsReady(UUID orderId) {
        log.info("Marking order as ready: orderId={}", orderId);

        try {
            Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            log.info("Found order: orderId={}, currentStatus={}", order.getOrderId(), order.getStatus());

            // Mark as ready
            order.markAsReady();

            // Save order
            Order updatedOrder = orderRepository.save(order);
            log.info("Order marked as ready: orderId={}, status={}",
                updatedOrder.getOrderId(), updatedOrder.getStatus());

            // Publish event
            publishEvent(new OrderReadyEvent(updatedOrder.getOrderId()));

        } catch (IllegalStateException e) {
            log.error("Cannot mark order {} as ready: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error marking order {} as ready: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark order as ready", e);
        }
    }

    /**
     * Mark order as failed (called by event handler)
     * @param orderId order identifier
     * @param reason failure reason
     */
    @Transactional
    public void markAsFailed(UUID orderId, String reason) {
        log.info("Marking order as failed: orderId={}, reason={}", orderId, reason);

        try {
            Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            log.info("Found order: orderId={}, currentStatus={}", order.getOrderId(), order.getStatus());

            // Mark as failed
            order.markAsFailed();

            // Save order
            Order updatedOrder = orderRepository.save(order);
            log.info("Order marked as failed: orderId={}, status={}",
                updatedOrder.getOrderId(), updatedOrder.getStatus());

            // Publish event
            publishEvent(new OrderProcessingFailedEvent(updatedOrder.getOrderId(), reason));

        } catch (Exception e) {
            log.error("Error marking order {} as failed: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark order as failed", e);
        }
    }

    private void publishEvent(OrderServiceEvent event) {
        try {
            log.info("Publishing event: type={}, orderId={}", event.getEventType(), event.getOrderId());
            kafkaTemplate.send(KAFKA_TOPIC, event.getOrderId().asString(), event);
            log.info("Event published successfully: type={}", event.getEventType());
        } catch (Exception e) {
            log.error("Error publishing event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }
}


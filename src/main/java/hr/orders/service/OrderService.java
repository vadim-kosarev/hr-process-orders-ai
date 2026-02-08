package hr.orders.service;

import hr.orders.domain.Order;
import hr.orders.domain.OrderItem;
import hr.orders.domain.OrderStatus;
import hr.orders.domain.event.OrderEvent;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String KAFKA_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    /**
     * Create new order with items
     *
     * @param orderID
     * @param items   list of order items
     * @return created order
     */
    @Transactional
    public Order createNewOrder(OrderID orderID, List<OrderItem> items) {
        log.info("Creating new order with {} items", items.size());

        try {

            Order order = Order.createWithItems(orderID, items);

            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: orderId={}, status={}",
                    savedOrder.getOrderId(), savedOrder.getStatus());

            publishEvents(order.pullEvents());

            return savedOrder;

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create order", e);
        }
    }

    private void publishEvents(Collection<OrderEvent> events) {
        for (OrderEvent event : events) {
            try {
                log.info("Publishing event: type={}, orderId={}", event.getEventType(), event.getOrderId());
                kafkaTemplate.send(KAFKA_TOPIC, event.getOrderId().asString(), event);
                log.info("Event published successfully: type={}", event.getEventType());
            } catch (Exception e) {
                log.error("Error publishing event {}: {}", event.getEventType(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void startProcessing(OrderID orderId) {
        try {
            Order order = orderRepository.findByOrderId(orderId).orElseThrow();
            order.startProcessing();
            orderRepository.save(order);
            publishEvents(order.pullEvents());
        } catch (Exception e) {
            log.error("Error starting processing for orderId {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to start processing order", e);
        }
    }

    @Transactional
    public void performProcessOrder(OrderID orderId) {
        try {
            Order order = orderRepository.findByOrderId(orderId).orElseThrow();
            double random = Math.random();
            if (random < 0.5) {
                log.info("Performing process order for orderId {}: SUCCESS", orderId);
                order.complete();
            } else if (random < 0.75) {
                log.info("Performing process order for orderId {}: CANCELLED", orderId);
                order.cancel();
            } else {
                log.info("Performing process order for orderId {}: FAILED", orderId);
                order.markAsFailed();
            }
            orderRepository.save(order);
            publishEvents(order.pullEvents());
        } catch (Exception e) {
            log.error("Error performing process order for orderId {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to perform process order", e);
        }
    }

    @Transactional
    public Optional<OrderStatus> getOrderStatus(OrderID orderId) {
        try {
            return orderRepository.findByOrderId(orderId).map(Order::getStatus);
        } catch (Exception e) {
            log.error("Error getting order status for orderId {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to get order status", e);
        }

    }
}


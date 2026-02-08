package hr.orders.service;

import hr.orders.domain.OrderStatus;
import hr.orders.domain.command.CreateOrderCommand;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")
public class OrderServiceTest {

    @Autowired
    private KafkaTemplate<String, Object> createOrderCommandKafkaTemplate;

    @Value("${app.kafka.topic.commands}")
    private String commandTopic;
    @Autowired
    private OrderService orderService;

    @Test
    void send10CreateOrderCommandConcurrently() {
        log.info("=== Test: Send 10 CreateOrderCommand to Kafka concurrently ===");
        int numCommands = 50;
        CountDownLatch latch = new CountDownLatch(numCommands);

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numCommands; i++) {
                exec.submit(() -> {
                    // countdown latch and start all threads at the same time
                    latch.countDown();
                    sendCreateOrderCommand();
                });
            }
        }

        log.info("=== Test completed ===");
    }

    @Test
    void sendCreateOrderCommand() {
        log.info("=== Test: Send CreateOrderCommand to Kafka ===");

        // Create test command
        CreateOrderCommand command = TestUtils.createTestOrderCommandWithItems(3);
        log.info("Created CreateOrderCommand: commandId={}, orderId={}, items={}",
                command.getCommandId(),
                command.getOrder().getOrderId(),
                command.getOrder().getItems().size());

        // Send to Kafka
        String key = command.getOrder().getOrderId().toString();
        log.info("Sending to topic: {}, key: {}", commandTopic, key);

        createOrderCommandKafkaTemplate.send(commandTopic, key, command)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send command to Kafka", ex);
                    } else {
                        log.info("Message sent to partition {} at offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .failFast(() -> log.error("Order did not reach final status within timeout"))
                .until(() -> {
                    OrderStatus status =
                            orderService.getOrderStatus(OrderID.of(command.getOrder().getOrderId()))
                                    .orElse(OrderStatus.NEW);
                    log.info("{}: Checking order status for orderId={}", status, command.getOrder().getOrderId());
                    return status.isFinal();
                });
        OrderStatus status =
                orderService.getOrderStatus(OrderID.of(command.getOrder().getOrderId()))
                        .orElse(OrderStatus.NEW);
        log.info("{} - FINAL STATUS for orderId={}", status, command.getOrder().getOrderId());


        log.info("=== Test completed ===");
    }

    @Test
    void sendMultipleCommandForCreateSameOrderAfterItIsProcessed() {
        log.info("=== Test: Send multiple CreateOrderCommand for same order after it is processed ===");

        // Create test command with specific order ID
        CreateOrderCommand command = TestUtils.createTestOrderCommandWithItems(3);
        String orderId = command.getOrder().getOrderId().toString();
        String key = orderId;

        log.info("Created CreateOrderCommand: commandId={}, orderId={}, items={}",
                command.getCommandId(),
                orderId,
                command.getOrder().getItems().size());

        // === Send first command ===
        log.info("Sending FIRST command to topic: {}, key: {}", commandTopic, key);
        createOrderCommandKafkaTemplate.send(commandTopic, key, command)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send first command to Kafka", ex);
                    } else {
                        log.info("FIRST message sent to partition {} at offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        // Wait for first command to be processed
        log.info("Waiting for order to reach final status after FIRST command...");
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .failFast(() -> log.error("Order did not reach final status within timeout after first command"))
                .until(() -> {
                    OrderStatus status =
                            orderService.getOrderStatus(OrderID.of(orderId))
                                    .orElse(OrderStatus.NEW);
                    log.info("{}: Checking order status after first command for orderId={}", status, orderId);
                    return status.isFinal();
                });

        OrderStatus statusAfterFirstCommand =
                orderService.getOrderStatus(OrderID.of(orderId))
                        .orElse(OrderStatus.NEW);
        log.info("{} - STATUS after FIRST command for orderId={}", statusAfterFirstCommand, orderId);

        // === Send second command for same order ===
        log.info("Sending SECOND command for same order to topic: {}, key: {}", commandTopic, key);
        createOrderCommandKafkaTemplate.send(commandTopic, key, command)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send second command to Kafka", ex);
                    } else {
                        log.info("SECOND message sent to partition {} at offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        // Wait a bit to allow processing
        log.info("Waiting for order to process SECOND command...");
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .failFast(() -> log.error("Order did not reach final status within timeout after second command"))
                .until(() -> {
                    OrderStatus status =
                            orderService.getOrderStatus(OrderID.of(orderId))
                                    .orElse(OrderStatus.NEW);
                    log.info("{}: Checking order status after second command for orderId={}", status, orderId);
                    return status.isFinal();
                });

        OrderStatus statusAfterSecondCommand =
                orderService.getOrderStatus(OrderID.of(orderId))
                        .orElse(OrderStatus.NEW);
        log.info("{} - FINAL STATUS after SECOND command for orderId={}", statusAfterSecondCommand, orderId);

        log.info("=== Test completed ===");
    }
}


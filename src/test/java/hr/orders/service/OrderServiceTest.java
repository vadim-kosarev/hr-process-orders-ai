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
        int numCommands = 10;
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
                .until(() -> {
                    OrderStatus status =
                            orderService.getOrderStatus(OrderID.of(command.getOrder().getOrderId()))
                                    .orElse(OrderStatus.NEW);
                    log.info("{}: Checking order status for orderId={}", status, command.getOrder().getOrderId());
                    return status.isFinal();
                });
        log.info("=== Test completed ===");
    }
}


package hr.orders.service;

import hr.orders.domain.command.CreateOrderCommand;
import hr.orders.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Slf4j
@ActiveProfiles("test")
public class OrderServiceTest {

    @Autowired
    private KafkaTemplate<String, CreateOrderCommand> createOrderCommandKafkaTemplate;

    @Value("${app.kafka.topic.commands}")
    private String commandTopic;

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

        log.info("=== Test completed ===");
    }
}


package hr.orders.service;

import hr.orders.domain.command.CreateOrderCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandHandler {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topic.commands}",
            groupId = "order-service-group",
            autoStartup = "${app.service.OrderCommandHandler.enabled:true}")
    public void handleOrderCommand(CreateOrderCommand command) {
        log.info("handleOrderCommand: Received command id={}", command.getCommandId());
    }
}


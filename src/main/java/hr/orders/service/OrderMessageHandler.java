package hr.orders.service;

import hr.orders.domain.Order;
import hr.orders.domain.OrderItem;
import hr.orders.domain.command.CreateOrderCommand;
import hr.orders.domain.command.OrderCommand;
import hr.orders.domain.event.OrderCreatedEvent;
import hr.orders.domain.event.OrderEvent;
import hr.orders.domain.event.OrderProcessingStartedEvent;
import hr.orders.domain.valueobject.Money;
import hr.orders.domain.valueobject.OrderID;
import hr.orders.domain.valueobject.Qty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMessageHandler {

    private final OrderService orderService;
    private final DeduplicationService deduplicationService;

    @KafkaListener(
            topics = "${app.kafka.topic.commands}",
            containerFactory = "orderCommandListenerFactory",
            autoStartup = "${app.service.OrderCommandHandler.enabled:true}",
            concurrency = "${app.service.OrderCommandHandler.concurrency:16}"
    )
    public void onCommand(OrderCommand command, Acknowledgment ack) {
        try {
            log.info("HANDLE COMMAND: Received command id={}, type={}",
                    command.getCommandId(), command.getClass().getSimpleName());

            if (!deduplicationService.checkAndMarkProcessed(
                    OrderMessageHandler.class.getSimpleName(),
                    command.getCommandId())) {
                log.info("Duplicate command detected, skipping processing: commandId={}", command.getCommandId());
                return;
            }

            if (command instanceof CreateOrderCommand createOrderCommand) {
                processCreateOrder(createOrderCommand);
            } else {
                log.warn("Unknown command type: {}", command.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to handle OrderCommand {}", command, e);
        } finally {
            ack.acknowledge();
        }
    }

    private void processCreateOrder(CreateOrderCommand createOrderCommand) {
        log.info("Processing CreateOrderCommand: orderId={}, items={}",
                createOrderCommand.getOrder().getOrderId(),
                createOrderCommand.getOrder().getItems().size());

        List<OrderItem> items = createOrderCommand.getOrder()
                .getItems().stream()
                .map(itemData -> OrderItem.create(
                        itemData.getProductId(),
                        Qty.of(itemData.getQuantity()),
                        Money.of(itemData.getPrice(), itemData.getCurrency())
                ))
                .toList();

        Order order = orderService.createNewOrder(
                OrderID.of(createOrderCommand.getOrder().getOrderId()),
                items);
        log.info("Order created successfully: orderId={}, status:{}", order.getOrderId(), order.getStatus());
    }

    @KafkaListener(
            topics = "${app.kafka.topic.events}",
            containerFactory = "orderEventListenerFactory",
            autoStartup = "${app.service.OrderCommandHandler.enabled:true}",
            concurrency = "${app.service.OrderCommandHandler.concurrency:16}"
    )
    public void onEvent(OrderEvent eventArg, Acknowledgment ack) {
        try {
            log.info("HANDLE EVENT: Received event ORDER={} id={}, type={}",
                    eventArg.getOrderId(),
                    eventArg.getEventId(), eventArg.getClass().getSimpleName());

            switch (eventArg) {
                case OrderCreatedEvent event -> {
                    processOrderCreatedEvent(event);
                }
                case OrderProcessingStartedEvent event -> {
                    processOrderProcessingStarted(event);
                }
                default -> log.warn("Unknown event type: {}", eventArg.getClass().getSimpleName());
            }

        } catch (Exception e) {
            log.error("Failed to handle OrderEvent {}", eventArg, e);
        } finally {
            ack.acknowledge();
        }
    }

    private void processOrderProcessingStarted(OrderProcessingStartedEvent event) {
        log.info("Handling OrderProcessingStartedEvent for orderId={}", event.getOrderId());
        try {
            TimeUnit.MILLISECONDS.sleep(new Random().nextInt(200, 3000));
        } catch (InterruptedException e) {
        }
        orderService.performProcessOrder(event.getOrderId());
    }

    private void processOrderCreatedEvent(OrderCreatedEvent createdEvent) {
        log.info("Handling OrderCreatedEvent for orderId={}", createdEvent.getOrderId());
        orderService.startProcessing(createdEvent.getOrderId());
    }
}


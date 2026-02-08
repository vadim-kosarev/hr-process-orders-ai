package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event published when a new order is created
 */
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends OrderServiceEvent {

    public OrderCreatedEvent(OrderID orderId) {
        super();
        initEvent(orderId);
    }

    @Override
    public String getEventType() {
        return "ORDER_CREATED";
    }
}


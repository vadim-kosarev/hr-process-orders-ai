package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event published when order is cancelled
 */
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends OrderServiceEvent {

    public OrderCancelledEvent(OrderID orderId) {
        super();
        initEvent(orderId);
    }

    @Override
    public String getEventType() {
        return "ORDER_CANCELLED";
    }
}


package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event published when order is ready for delivery/pickup
 */
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderReadyEvent extends OrderServiceEvent {

    public OrderReadyEvent(OrderID orderId) {
        super();
        initEvent(orderId);
    }

    @Override
    public String getEventType() {
        return "ORDER_READY";
    }
}


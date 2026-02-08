package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event published when order processing has started
 */
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderProcessingStartedEvent extends OrderServiceEvent {

    public OrderProcessingStartedEvent(OrderID orderId) {
        super();
        initEvent(orderId);
    }

    @Override
    public String getEventType() {
        return "ORDER_PROCESSING_STARTED";
    }
}


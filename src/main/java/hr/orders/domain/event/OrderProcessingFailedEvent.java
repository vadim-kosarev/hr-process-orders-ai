package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event published when order processing has failed
 */
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderProcessingFailedEvent extends OrderServiceEvent {

    private String reason;

    public OrderProcessingFailedEvent(OrderID orderId, String reason) {
        super();
        initEvent(orderId);
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "ORDER_PROCESSING_FAILED";
    }
}


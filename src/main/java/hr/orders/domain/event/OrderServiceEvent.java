package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Base class for all order service events
 * All events are published to Kafka topic 'order-events'
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public abstract class OrderServiceEvent extends OrderEvent implements Serializable {

    protected OrderID orderId;

    protected void initEvent(OrderID orderId) {
        this.orderId = orderId;
    }
}

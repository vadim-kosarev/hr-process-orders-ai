package hr.orders.domain.event;

import hr.orders.domain.valueobject.OrderID;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class OrderEvent implements Serializable {
    protected final UUID eventId = UUID.randomUUID();
    protected final LocalDateTime occurredAt = LocalDateTime.now();
    public abstract String getEventType();
    public abstract OrderID getOrderId();
}

package hr.orders.domain.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class DomainEvent implements Serializable {
    protected final LocalDateTime occurredAt = LocalDateTime.now();
    public abstract String getEventType();
}

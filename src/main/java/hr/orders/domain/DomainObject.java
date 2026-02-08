package hr.orders.domain;

import hr.orders.domain.event.OrderEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class DomainObject {

    private final List<OrderEvent> uncommittedEvents = new ArrayList<>();

    protected void raiseEvent(OrderEvent event) {
        uncommittedEvents.add(event);
    }

    public List<OrderEvent> pullEvents() {
        List<OrderEvent> events = List.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return events;
    }

}

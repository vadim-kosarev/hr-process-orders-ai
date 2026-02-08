package hr.orders.domain;

import hr.orders.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class DomainObject {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected void raiseEvent(DomainEvent event) {
        uncommittedEvents.add(event);
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> events = List.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return events;
    }

}

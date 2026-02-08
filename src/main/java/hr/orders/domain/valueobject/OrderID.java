package hr.orders.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class OrderID implements Serializable {

    private UUID value;

    protected OrderID() {
        // for JPA
    }

    private OrderID(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("OrderID cannot be null");
        }
        this.value = value;
    }

    public static OrderID of(UUID value) {
        return new OrderID(value);
    }

    public static OrderID of(String value) {
        return new OrderID(UUID.fromString(value));
    }

    public static OrderID generate() {
        return new OrderID(UUID.randomUUID());
    }

    public String asString() {
        return value.toString();
    }
}

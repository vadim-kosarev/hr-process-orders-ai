package hr.orders.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representing quantity
 * Immutable and self-validating
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class Qty implements Serializable {

    private int value;

    protected Qty() {
        // for JPA
    }

    private Qty(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.value = value;
    }

    /**
     * Factory method to create Qty instance
     * @param value quantity value
     * @return Qty instance
     */
    public static Qty of(int value) {
        return new Qty(value);
    }

    /**
     * Add quantity
     * @param other quantity to add
     * @return new Qty instance with sum
     */
    public Qty add(Qty other) {
        return new Qty(this.value + other.value);
    }

    /**
     * Subtract quantity
     * @param other quantity to subtract
     * @return new Qty instance with difference
     */
    public Qty subtract(Qty other) {
        return new Qty(this.value - other.value);
    }

    /**
     * Check if quantity is zero
     * @return true if zero
     */
    public boolean isZero() {
        return value == 0;
    }

    /**
     * Check if quantity is positive
     * @return true if positive
     */
    public boolean isPositive() {
        return value > 0;
    }
}


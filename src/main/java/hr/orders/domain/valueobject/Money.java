package hr.orders.domain.valueobject;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Value Object representing money with amount and currency
 * Immutable and self-validating
 */
@Embeddable
@Getter
@EqualsAndHashCode
@ToString
public class Money implements Serializable {

    private BigDecimal amount;
    private Currency currency;

    protected Money() {
        // for JPA
    }

    private Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * Factory method to create Money instance
     *
     * @param amount   monetary amount
     * @param currency currency code
     * @return Money instance
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Factory method to create Money instance from double
     *
     * @param amount       monetary amount
     * @param currencyCode currency code (e.g., "USD", "EUR")
     * @return Money instance
     */
    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }

    /**
     * Factory method to create Money instance
     *
     * @param amount       monetary amount
     * @param currencyCode currency code (e.g., "USD", "EUR")
     * @return Money instance
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * Add money (must be same currency)
     *
     * @param other money to add
     * @return new Money instance with sum
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtract money (must be same currency)
     *
     * @param other money to subtract
     * @return new Money instance with difference
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiply by quantity
     *
     * @param multiplier quantity
     * @return new Money instance
     */
    public Money multiply(Qty multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier.getValue())), this.currency);
    }

    /**
     * Multiply by scalar
     *
     * @param multiplier scalar value
     * @return new Money instance
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    /**
     * Check if money is zero
     *
     * @return true if zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Check if money is positive
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Compare with another money
     *
     * @param other money to compare
     * @return comparison result
     */
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    /**
     * Get currency code
     *
     * @return currency code (e.g., "USD")
     */
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(String.format("Cannot perform operation on different currencies: %s and %s", this.currency, other.currency));
        }
    }
}


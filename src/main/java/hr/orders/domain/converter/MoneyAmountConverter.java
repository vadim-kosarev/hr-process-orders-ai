package hr.orders.domain.converter;

import hr.orders.domain.valueobject.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * JPA Converter for Money value object
 * Note: This converter handles only the amount
 * Currency is stored separately via @AttributeOverride in entity
 */
@Converter
public class MoneyAmountConverter implements AttributeConverter<Money, BigDecimal> {

    private static final String DEFAULT_CURRENCY = "USD";

    @Override
    public BigDecimal convertToDatabaseColumn(Money money) {
        return money == null ? null : money.getAmount();
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        // Note: Currency should be loaded separately from currency column
        // This is a fallback converter
        return Money.of(amount, Currency.getInstance(DEFAULT_CURRENCY));
    }
}


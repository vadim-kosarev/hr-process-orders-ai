package hr.orders.domain.converter;

import hr.orders.domain.valueobject.Qty;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for Qty value object
 * Converts between Qty and Integer for database storage
 */
@Converter(autoApply = true)
public class QtyConverter implements AttributeConverter<Qty, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Qty qty) {
        return qty == null ? null : qty.getValue();
    }

    @Override
    public Qty convertToEntityAttribute(Integer value) {
        return value == null ? null : Qty.of(value);
    }
}


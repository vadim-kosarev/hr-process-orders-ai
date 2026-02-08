package hr.orders.domain.converter;

import hr.orders.domain.valueobject.OrderID;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA Converter for OrderID value object
 * Converts between OrderID and UUID for database storage
 */
@Converter(autoApply = true)
public class OrderIDConverter implements AttributeConverter<OrderID, UUID> {

    @Override
    public UUID convertToDatabaseColumn(OrderID orderId) {
        return orderId == null ? null : orderId.getValue();
    }

    @Override
    public OrderID convertToEntityAttribute(UUID uuid) {
        return uuid == null ? null : OrderID.of(uuid);
    }
}


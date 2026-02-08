package hr.orders.domain;

import hr.orders.domain.valueobject.Money;
import hr.orders.domain.valueobject.Qty;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode(of = {"id"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    })
    private Qty quantity;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount", nullable = false, precision = 19, scale = 4))
    })
    private Money unitPrice;

    @Column(name = "unit_price_currency", nullable = false, length = 3)
    private String unitPriceCurrency;

    public static OrderItem create(UUID productId, Qty quantity, Money unitPrice) {
        validateParameters(productId, quantity, unitPrice);
        OrderItem item = new OrderItem(null, productId, quantity, unitPrice, unitPrice.getCurrencyCode());
        return item;
    }

    private static void validateParameters(UUID productId, Qty quantity, Money unitPrice) {
        if (productId == null) throw new IllegalArgumentException("Product ID cannot be null");
        if (quantity == null) throw new IllegalArgumentException("Quantity cannot be null");
        if (unitPrice == null) throw new IllegalArgumentException("Unit price cannot be null");
        if (!quantity.isPositive()) throw new IllegalArgumentException("Quantity must be positive");
        if (!unitPrice.isPositive()) throw new IllegalArgumentException("Unit price must be positive");
    }

    public Money getUnitPrice() {
        if (unitPrice != null && unitPriceCurrency != null) {
            return Money.of(unitPrice.getAmount(), unitPriceCurrency);
        }
        return unitPrice;
    }

    public Money calculateLineTotal() {
        return getUnitPrice().multiply(quantity);
    }

    public void updateQuantity(Qty newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (!newQuantity.isPositive()) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
    }

    public void updateUnitPrice(Money newUnitPrice) {
        if (newUnitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null");
        }
        if (!newUnitPrice.isPositive()) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        this.unitPrice = newUnitPrice;
        this.unitPriceCurrency = newUnitPrice.getCurrencyCode();
    }
}


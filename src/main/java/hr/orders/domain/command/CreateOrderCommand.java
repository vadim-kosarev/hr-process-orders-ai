package hr.orders.domain.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CREATE_ORDER")
public class CreateOrderCommand extends OrderCommand {

    private OrderData order;

    public CreateOrderCommand(OrderData order) {
        super();
        initCommand();
        this.order = order;
    }

    @Override
    public String getCommandType() {
        return "CREATE_ORDER";
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class OrderData implements java.io.Serializable {
        private UUID orderId;
        private String status;
        private List<OrderItemData> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderData(UUID orderId, String status, List<OrderItemData> items,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.orderId = orderId;
            this.status = status;
            this.items = items;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    public static class OrderItemData implements java.io.Serializable {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
        private String currency;

        public OrderItemData(UUID productId, int quantity, BigDecimal price, String currency) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
            this.currency = currency;
        }
    }
}


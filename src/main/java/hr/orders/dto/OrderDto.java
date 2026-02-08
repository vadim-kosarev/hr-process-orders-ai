package hr.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private OrderData order;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderData {
        private UUID orderId;
        private String status;
        private List<OrderItemData> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
        private String currency;
    }
}


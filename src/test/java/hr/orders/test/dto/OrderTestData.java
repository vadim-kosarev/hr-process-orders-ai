package hr.orders.test.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class OrderTestData {
    private OrderData order;

    @Data
    @NoArgsConstructor
    public static class OrderData {
        private UUID orderId;
        private String status;
        private List<OrderItemTestData> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    public static class OrderItemTestData {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
        private String currency;
    }
}


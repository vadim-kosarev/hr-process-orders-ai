package hr.orders.repository;

import hr.orders.domain.Order;
import hr.orders.domain.OrderStatus;
import hr.orders.domain.valueobject.OrderID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order entity
 * Provides data access methods for orders
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by business identifier (OrderID)
     * @param orderId order business identifier
     * @return optional order
     */
    Optional<Order> findByOrderId(OrderID orderId);

    /**
     * Find order by UUID value
     * @param uuid order UUID value
     * @return optional order
     */
    default Optional<Order> findByOrderId(UUID uuid) {
        return findByOrderId(OrderID.of(uuid));
    }

    /**
     * Find orders by status
     * @param status order status
     * @return list of orders
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Check if order exists by OrderID
     * @param orderId order business identifier
     * @return true if exists
     */
    boolean existsByOrderId(OrderID orderId);
}


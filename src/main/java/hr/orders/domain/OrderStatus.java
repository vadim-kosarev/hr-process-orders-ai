package hr.orders.domain;

/**
 * Order status enum representing the lifecycle of an order
 */
public enum OrderStatus {

    /**
     * Order has been created and is new
     */
    NEW,

    /**
     * Order is being processed
     */
    IN_PROGRESS,

    /**
     * Order is ready for delivery/pickup
     */
    READY,

    /**
     * Order has been cancelled
     */
    CANCELLED,

    /**
     * Order processing failed
     */
    FAILED;

    /**
     * Check if order can be cancelled
     * @return true if order can be cancelled
     */
    public boolean canBeCancelled() {
        return this == NEW || this == IN_PROGRESS;
    }

    /**
     * Check if order is in final state
     * @return true if order is ready, cancelled or failed
     */
    public boolean isFinal() {
        return this == READY || this == CANCELLED || this == FAILED;
    }

    /**
     * Check if order can be processed
     * @return true if order can move to in_progress state
     */
    public boolean canBeProcessed() {
        return this == NEW;
    }

    /**
     * Check if order can be marked as ready
     * @return true if order can move to ready state
     */
    public boolean canBeMarkedAsReady() {
        return this == IN_PROGRESS;
    }

    /**
     * Check if order can be marked as failed
     * @return true if order can move to failed state
     */
    public boolean canBeFailed() {
        return this == NEW || this == IN_PROGRESS;
    }
}


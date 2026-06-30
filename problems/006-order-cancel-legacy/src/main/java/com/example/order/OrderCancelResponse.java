package com.example.order;

public class OrderCancelResponse {

    public final Long orderId;
    public final Long userId;
    public final String status;
    public final String cancelledAt;

    private OrderCancelResponse(Long orderId, Long userId, String status, String cancelledAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public static OrderCancelResponse from(Order order) {
        return new OrderCancelResponse(
                order.id,
                order.userId,
                order.status,
                order.cancelledAt == null ? null : order.cancelledAt.toString()
        );
    }
}

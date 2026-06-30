package com.example.order;

import java.time.LocalDateTime;

public class Order {

    public Long id;
    public Long userId;
    public int amount;
    public String status; // ACTIVE, CANCELLED
    public String type;   // NORMAL, PREORDER, DIGITAL
    public LocalDateTime createdAt;
    public LocalDateTime cancelledAt;
    public String cancelReason;

    public Order(Long id, Long userId, int amount, String status, String type, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.type = type;
        this.createdAt = createdAt;
    }

    public void requestCancel(String reason) {
        this.status = "CANCEL_REQUESTED";
        this.cancelReason = reason;
    }

    public void completeCancel(LocalDateTime cancelledAt) {
        this.status = "CANCELLED";
        this.cancelledAt = cancelledAt;
    }
}

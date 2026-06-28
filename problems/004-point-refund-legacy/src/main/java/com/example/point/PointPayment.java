package com.example.point;
//entity
public class PointPayment {

    public Long id;
    public Long userId;
    public int amount;
    public String status;
    public String requestedAt;
    public String refundReason;
    public String reason;

    public PointPayment(Long id, Long userId, int amount, String status, String requestedAt, String refundReason) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.requestedAt = requestedAt;
        this.refundReason = refundReason;
    }
}

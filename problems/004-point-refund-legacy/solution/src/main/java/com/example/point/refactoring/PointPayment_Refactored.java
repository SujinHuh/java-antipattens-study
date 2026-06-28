package com.example.point.refactoring;

public class PointPayment_Refactored {

    private final Long id;
    private final Long userId;
    private final int amount;
    private final PointRefundStatus_Refactored status;
    private final String paidAt;
    private final String refundReason;

    public PointPayment_Refactored(Long id, Long userId, int amount, PointRefundStatus_Refactored status, String paidAt, String refundReason) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
        this.refundReason = refundReason;
    }

    public PointPayment_Refactored refund(String reason) {
        return new PointPayment_Refactored(id, userId, amount, PointRefundStatus_Refactored.REFUNDED, paidAt, reason);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }

    public PointRefundStatus_Refactored getStatus() {
        return status;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public String getRefundReason() {
        return refundReason;
    }
}

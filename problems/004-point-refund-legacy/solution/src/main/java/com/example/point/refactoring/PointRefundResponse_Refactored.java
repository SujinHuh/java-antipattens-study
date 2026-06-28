package com.example.point.refactoring;

public record PointRefundResponse_Refactored(Long paymentId, Long userId, int amount, String status, String refundReason) {

    public static PointRefundResponse_Refactored from(PointPayment_Refactored payment) {
        return new PointRefundResponse_Refactored(
            payment.getId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getStatus().name(),
            payment.getRefundReason()
        );
    }
}

package com.example.point.refactoring;

public class PointRefundPolicy_Refactored {

    public void validateRefundable(PointPayment_Refactored payment, PointRefundRequest_Refactored request) {
        if (!payment.getUserId().equals(request.userId())) {
            throw new PointRefundException_Refactored(PointRefundErrorCode_Refactored.PAYMENT_NOT_OWNER, "payment does not belong to user");
        }

        if (payment.getStatus() == PointRefundStatus_Refactored.REFUNDED) {
            throw new PointRefundException_Refactored(PointRefundErrorCode_Refactored.ALREADY_REFUNDED, "payment already refunded");
        }

        if (isWeekendRefundBlocked(request.requestedAt())) {
            throw new PointRefundException_Refactored(PointRefundErrorCode_Refactored.REFUND_BLOCKED, "weekend refund blocked");
        }
    }

    private boolean isWeekendRefundBlocked(String requestedAt) {
        return requestedAt != null && (requestedAt.endsWith("-06") || requestedAt.endsWith("-07"));
    }
}

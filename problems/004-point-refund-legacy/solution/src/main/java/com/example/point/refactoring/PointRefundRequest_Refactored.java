package com.example.point.refactoring;

public record PointRefundRequest_Refactored(Long paymentId, Long userId, String reason, String requestedAt) {

    public void validate() {
        if (paymentId == null || userId == null) {
            throw new PointRefundException_Refactored(PointRefundErrorCode_Refactored.INVALID_REQUEST, "paymentId and userId are required");
        }
    }
}

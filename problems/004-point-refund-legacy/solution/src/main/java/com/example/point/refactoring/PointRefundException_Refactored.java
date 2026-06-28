package com.example.point.refactoring;

public class PointRefundException_Refactored extends RuntimeException {

    private final PointRefundErrorCode_Refactored errorCode;

    public PointRefundException_Refactored(PointRefundErrorCode_Refactored errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PointRefundErrorCode_Refactored getErrorCode() {
        return errorCode;
    }
}

package com.example.order;

public class OrderCancelException extends RuntimeException {

    private final OrderCancelErrorCode errorCode;

    public OrderCancelException(OrderCancelErrorCode errorCode) {
        super(errorCode.message);
        this.errorCode = errorCode;
    }

    public OrderCancelErrorCode getErrorCode() {
        return errorCode;
    }
}

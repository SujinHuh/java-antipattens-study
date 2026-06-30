package com.example.order;

public enum OrderCancelErrorCode {
    INVALID_REQUEST("invalid cancel request"),
    ORDER_NOT_FOUND("order not found"),
    ALREADY_CANCELLED("already cancelled"),
    BLOCKED_ORDER("blocked order cannot be cancelled"),
    CANCEL_WINDOW_CLOSED("cancel window closed"),
    DIGITAL_ITEM_NOT_CANCELABLE("digital items cannot be cancelled"),
    UNKNOWN_ORDER_TYPE("unknown order type");

    public final String message;

    OrderCancelErrorCode(String message) {
        this.message = message;
    }
}

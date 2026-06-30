package com.example.order;

public enum OrderType {
    NORMAL {
        @Override
        int calculateRefundAmount(int amount) {
            return amount;
        }
    },
    PREORDER {
        @Override
        int calculateRefundAmount(int amount) {
            return (int) (amount * 0.9);
        }
    },
    DIGITAL {
        @Override
        int calculateRefundAmount(int amount) {
            throw new OrderCancelException(OrderCancelErrorCode.DIGITAL_ITEM_NOT_CANCELABLE);
        }
    };

    abstract int calculateRefundAmount(int amount);

    static OrderType from(String value) {
        for (OrderType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        throw new OrderCancelException(OrderCancelErrorCode.UNKNOWN_ORDER_TYPE);
    }
}

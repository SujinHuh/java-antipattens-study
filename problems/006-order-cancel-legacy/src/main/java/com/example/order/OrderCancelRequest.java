package com.example.order;

public class OrderCancelRequest {

    public Long orderId;
    public Long userId;
    public String reason;
    public Integer requestedRefundAmount;
    public String requestedStatus;
}

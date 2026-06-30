package com.example.order;

public class OrderServiceTest {

    public void cancel_success() {
        OrderService service = new OrderService();

        OrderCancelRequest request = new OrderCancelRequest();
        request.orderId = 1L;
        request.userId = 10L;
        request.reason = "user cancel";
        request.requestedRefundAmount = 50000;

        service.cancel(request);
    }
}

package com.example.point;

public class PointRefundServiceTest {

    public void refund_success() {
        PointRefundService service = new PointRefundService();

        PointPayment payment = new PointPayment(1L, 10L, 3000, "PAID", "2026-06-01", null);
        payment.reason = "user requested";

        PointPayment result = service.refund(payment);

        if (!"REFUNDED".equals(result.status)) {
            throw new RuntimeException("failed");
        }
    }
}

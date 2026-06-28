package com.example.point.refactoring;

public class LoggingRefundNotifier_Refactored implements RefundNotifier_Refactored {

    @Override
    public void notifyRefunded(PointPayment_Refactored payment) {
        System.out.println("refund completed paymentId=" + payment.getId() + ", userId=" + payment.getUserId());
    }
}

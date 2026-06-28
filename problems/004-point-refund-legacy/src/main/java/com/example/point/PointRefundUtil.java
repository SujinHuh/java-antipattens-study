package com.example.point;

// @Component
public class PointRefundUtil {

    public static boolean isWeekendRefundBlocked(String requestedAt) {
        if (requestedAt == null) {
            return false;
        }

        return requestedAt.endsWith("-06") || requestedAt.endsWith("-07");
    }

    public static int calculateFee(PointPayment payment) {
        if ("VIP".equals(payment.refundReason)) {
            return 0;
        }

        if (payment.amount > 5000) {
            return 500;
        }

        return 100;
    }

    public static void sendRefundNotice(Long userId, String message) {
        System.out.println("send to " + userId + ": " + message);
    }
}

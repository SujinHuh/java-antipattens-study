package com.example.order;

import java.time.LocalDateTime;

// @Component
public class OrderCancelUtil {

    public static boolean isCancelWindowClosed(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        return LocalDateTime.now().minusMinutes(30).isAfter(createdAt);
    }

    public static void requestExternalPgRefund(Long orderId, int refundAmount) {
        System.out.println("PG Refund Request: orderId=" + orderId + ", amount=" + refundAmount);
    }

    public static void sendAuditLog(Long userId, String message) {
        System.out.println("audit log send to user " + userId + ": " + message);
    }
}

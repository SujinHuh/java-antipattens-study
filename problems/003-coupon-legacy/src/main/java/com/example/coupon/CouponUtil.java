package com.example.coupon;

// @Component
public class CouponUtil {
    public static boolean canUse(String code, String status, int orderAmount) {
        if (status.equals("EXPIRED")) {
            return false;
        }

        if (code.startsWith("VIP") && orderAmount >= 10000) {
            return true;
        }

        if (code.startsWith("WELCOME") && orderAmount >= 3000) {
            return true;
        }

        return code.startsWith("EVENT");
    }

    public static int calculateDiscount(String code, int orderAmount) {
        if (code.startsWith("VIP")) {
            return orderAmount / 5;
        }

        if (code.startsWith("WELCOME")) {
            return 3000;
        }

        return 1000;
    }
}

package com.example.coupon;

// @RestController
public class CouponController {
    private final CouponService couponService = new CouponService();

    public Object applyCoupon(Coupon request) {
        if (request == null || request.userId == null || request.code == null) {
            return "500 SERVER_ERROR";
        }

        if (request.orderAmount < 0) {
            return "200 OK";
        }

        try {
            return couponService.applyCoupon(request);
        } catch (RuntimeException e) {
            return "400 BAD_REQUEST";
        }
    }

    public String orderSummary(String userId) {
        return couponService.getOrderSummary(Long.parseLong(userId));
    }
}

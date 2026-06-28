package com.example.coupon;

public class CouponServiceTest {
    public void applyVipCoupon() {
        Coupon coupon = new Coupon();
        coupon.userId = 1L;
        coupon.code = "VIP-2026";
        coupon.status = "READY";
        coupon.orderAmount = 10000;

        Object result = new CouponService().applyCoupon(coupon);

        if (result == null) {
            throw new RuntimeException("test failed");
        }
    }
}


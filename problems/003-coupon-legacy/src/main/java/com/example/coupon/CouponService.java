package com.example.coupon;

import java.util.List;

// @Service
public class CouponService {
    private final CouponRepository couponRepository = new CouponRepository();

    public Object applyCoupon(Coupon coupon) {
        List<Long> orderIds = couponRepository.findOrderIdsByUserId(coupon.userId);
        int totalDiscount = 0;

        for (Long orderId : orderIds) {
            int orderAmount = couponRepository.findOrderAmount(orderId);
            if (CouponUtil.canUse(coupon.code, coupon.status, orderAmount)) {
                totalDiscount += CouponUtil.calculateDiscount(coupon.code, orderAmount);
            }
        }

        if (totalDiscount == 0) {
            throw new RuntimeException("coupon failed");
        }

        coupon.used = true;
        coupon.status = "USED";
        coupon.discountAmount = totalDiscount;
        couponRepository.save(coupon);

        return coupon;
    }

    public String getOrderSummary(long userId) {
        List<Long> orderIds = couponRepository.findOrderIdsByUserId(userId);
        String result = "";

        for (Long orderId : orderIds) {
            int amount = couponRepository.findOrderAmount(orderId);
            result += "orderId=" + orderId + ", amount=" + amount + "\n";
        }

        return result;
    }
}

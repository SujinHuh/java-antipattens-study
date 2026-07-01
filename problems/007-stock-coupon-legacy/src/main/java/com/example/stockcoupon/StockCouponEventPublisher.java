package com.example.stockcoupon;

// @Component
public class StockCouponEventPublisher {

    public void publishIssued(CouponIssueHistory history) {
        System.out.println("coupon issued userId=" + history.userId
            + ", couponId=" + history.couponId
            + ", key=" + history.idempotencyKey);
    }
}

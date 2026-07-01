package com.example.stockcoupon;

public class StockCouponResponse {

    public Long couponId;
    public Long userId;
    public String status;
    public int remainingQuantity;

    public StockCouponResponse(Long couponId, Long userId, String status, int remainingQuantity) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.remainingQuantity = remainingQuantity;
    }
}

package com.example.stockcoupon;

public class StockCouponRequest {

    public Long userId;
    public Long couponId;
    public int requestedQuantity;
    public Integer clientVersion;
    public String idempotencyKey;
}

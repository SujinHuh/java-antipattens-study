package com.example.stockcoupon;

import java.time.LocalDateTime;

public class CouponIssueHistory {

    public Long userId;
    public Long couponId;
    public String idempotencyKey;
    public String status;
    public LocalDateTime issuedAt;

    public CouponIssueHistory(Long userId, Long couponId, String idempotencyKey, String status, LocalDateTime issuedAt) {
        this.userId = userId;
        this.couponId = couponId;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.issuedAt = issuedAt;
    }
}

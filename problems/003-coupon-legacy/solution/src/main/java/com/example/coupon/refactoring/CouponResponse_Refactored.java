package com.example.coupon.refactoring;

public record CouponResponse_Refactored(
        String code,
        CouponStatus_Refactored status,
        int discountAmount
) {
}


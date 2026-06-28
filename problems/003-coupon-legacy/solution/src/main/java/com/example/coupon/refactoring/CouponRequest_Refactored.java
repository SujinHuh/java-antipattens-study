package com.example.coupon.refactoring;

public record CouponRequest_Refactored(
        long userId,
        String code
) {
    public CouponRequest_Refactored {
        // 기존 코드: Controller가 Coupon Entity를 받고, 클라이언트가 status까지 보낼 수 있었다.
        // 수정 코드: 요청 DTO에는 사용자가 보낼 수 있는 값만 남기고 기본 입력 조건을 검증한다.
        if (userId <= 0 || code == null || code.isBlank()) {
            throw new CouponException_Refactored(CouponErrorCode_Refactored.INVALID_REQUEST, "Invalid coupon request");
        }
    }
}

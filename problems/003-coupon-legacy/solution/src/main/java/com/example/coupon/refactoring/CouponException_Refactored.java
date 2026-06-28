package com.example.coupon.refactoring;

public class CouponException_Refactored extends RuntimeException {
    // 기존 코드: RuntimeException("coupon failed")처럼 원인을 구분하기 어려운 예외를 던졌다.
    // 수정 코드: 에러 코드를 함께 보관해 실패 원인을 분류할 수 있게 했다.
    private final CouponErrorCode_Refactored errorCode;

    public CouponException_Refactored(CouponErrorCode_Refactored errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CouponErrorCode_Refactored getErrorCode() {
        return errorCode;
    }
}

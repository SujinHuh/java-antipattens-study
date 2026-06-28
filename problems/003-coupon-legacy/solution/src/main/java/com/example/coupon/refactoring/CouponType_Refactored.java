package com.example.coupon.refactoring;

public enum CouponType_Refactored {
    VIP,
    WELCOME,
    EVENT;

    public static CouponType_Refactored from(String code) {
        // 기존 코드: VIP/WELCOME/EVENT 문자열 prefix를 여러 곳에서 직접 비교했다.
        // 수정 코드: 쿠폰 코드 해석을 enum 안으로 모아 알 수 없는 코드는 명시적으로 거절한다.
        if (code.startsWith("VIP")) {
            return VIP;
        }
        if (code.startsWith("WELCOME")) {
            return WELCOME;
        }
        if (code.startsWith("EVENT")) {
            return EVENT;
        }
        throw new CouponException_Refactored(CouponErrorCode_Refactored.INVALID_REQUEST, "Unknown coupon code");
    }
}

package com.example.coupon.refactoring;

// @Component
public class CouponPolicy_Refactored {
    public boolean canUse(String code, int orderAmount) {
        // 기존 코드: CouponUtil.canUse()가 status.equals("EXPIRED")처럼 null에 취약한 문자열 비교를 했다.
        // 수정 코드: 쿠폰 타입 판별과 최소 주문 금액 정책을 명확히 분리했다.
        CouponType_Refactored type = CouponType_Refactored.from(code);
        return switch (type) {
            case VIP -> orderAmount >= 10000;
            case WELCOME -> orderAmount >= 3000;
            case EVENT -> true;
        };
    }

    public int calculateDiscount(String code, int orderAmount) {
        // 기존 코드: CouponUtil static method에 쿠폰 정책이 들어 있었다.
        // 수정 코드: 정책 객체가 쿠폰 타입별 할인 계산을 담당한다.
        CouponType_Refactored type = CouponType_Refactored.from(code);
        return switch (type) {
            case VIP -> orderAmount / 5;
            case WELCOME -> 3000;
            case EVENT -> 1000;
        };
    }
}

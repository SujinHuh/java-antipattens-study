package com.example.coupon.refactoring;

// @RestController
public class CouponController_Refactored {
    // 기존 코드: Controller가 Service를 직접 new로 생성했다.
    // 수정 코드: 생성자 주입 형태로 바꿔 테스트와 교체가 가능하게 했다.
    private final CouponService_Refactored couponService;

    public CouponController_Refactored(CouponService_Refactored couponService) {
        this.couponService = couponService;
    }

    // 기존 코드: Coupon Entity를 요청으로 직접 받았다.
    // 수정 코드: 요청 DTO를 받아 API 모델과 저장 모델을 분리했다.
    public CouponResponse_Refactored applyCoupon(CouponRequest_Refactored request) {
        return couponService.applyCoupon(request);
    }

    // 기존 코드: String userId를 직접 parseLong 했다.
    // 수정 코드: 이미 변환된 long 값을 받아 Controller 파싱 책임을 줄였다.
    public OrderSummaryResponse_Refactored orderSummary(long userId) {
        return couponService.getOrderSummary(userId);
    }
}


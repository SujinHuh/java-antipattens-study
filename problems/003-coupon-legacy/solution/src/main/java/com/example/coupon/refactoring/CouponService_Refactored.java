package com.example.coupon.refactoring;

import java.util.List;

// @Service
public class CouponService_Refactored {
    private final CouponRepository_Refactored couponRepository;
    private final CouponPolicy_Refactored couponPolicy;

    public CouponService_Refactored(CouponRepository_Refactored couponRepository, CouponPolicy_Refactored couponPolicy) {
        this.couponRepository = couponRepository;
        this.couponPolicy = couponPolicy;
    }

    // 기존 코드: 상태 변경과 저장이 있지만 트랜잭션 경계가 보이지 않았다.
    // 수정 코드: 쿠폰 적용 유스케이스에 트랜잭션 경계를 둔다.
    // @Transactional
    public CouponResponse_Refactored applyCoupon(CouponRequest_Refactored request) {
        // 기존 코드: 클라이언트가 보낸 Coupon 객체의 code/status를 그대로 신뢰했다.
        // 수정 코드: 서버 저장소의 기존 쿠폰을 code로 조회한 뒤 소유자와 상태를 검증한다.
        Coupon_Refactored issuedCoupon = couponRepository.findByCode(request.code())
                .orElseThrow(() -> new CouponException_Refactored(
                        CouponErrorCode_Refactored.COUPON_NOT_FOUND,
                        "Coupon not found"
                ));

        if (issuedCoupon.getUserId() != request.userId()) {
            throw new CouponException_Refactored(
                    CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE,
                    "Coupon does not belong to user"
            );
        }

        if (issuedCoupon.getStatus() != CouponStatus_Refactored.READY) {
            throw new CouponException_Refactored(
                    CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE,
                    "Coupon is already used or expired"
            );
        }

        List<OrderAmount_Refactored> orders = couponRepository.findOrderAmountsByUserId(request.userId());

        int totalDiscount = orders.stream()
                .filter(order -> couponPolicy.canUse(request.code(), order.amount()))
                .mapToInt(order -> couponPolicy.calculateDiscount(request.code(), order.amount()))
                .sum();

        if (totalDiscount == 0) {
            throw new CouponException_Refactored(CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE, "Coupon is not available");
        }

        // 기존 코드: 요청 Entity를 직접 수정하고 그대로 반환했다.
        // 수정 코드: 서버가 관리하는 Entity를 생성하고 응답 DTO만 반환한다.
        Coupon_Refactored coupon = new Coupon_Refactored(
                issuedCoupon.getUserId(),
                issuedCoupon.getCode(),
                CouponStatus_Refactored.USED,
                totalDiscount
        );

        couponRepository.save(coupon);
        return new CouponResponse_Refactored(coupon.getCode(), coupon.getStatus(), coupon.getDiscountAmount());
    }

    public OrderSummaryResponse_Refactored getOrderSummary(long userId) {
        List<OrderAmount_Refactored> orders = couponRepository.findOrderAmountsByUserId(userId);
        return new OrderSummaryResponse_Refactored(userId, orders);
    }
}

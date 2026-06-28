package com.example.coupon.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// @Repository
public class InMemoryCouponRepository_Refactored implements CouponRepository_Refactored {
    // 기존 코드: static List라 테스트 간 상태가 공유될 수 있었다.
    // 수정 코드: 인스턴스 필드로 바꿔 테스트마다 독립 저장소를 만들 수 있게 했다.
    private final List<Coupon_Refactored> coupons = new ArrayList<>(List.of(
            new Coupon_Refactored(1L, "VIP-2026", CouponStatus_Refactored.READY, 0),
            new Coupon_Refactored(1L, "WELCOME-2026", CouponStatus_Refactored.READY, 0),
            new Coupon_Refactored(2L, "EVENT-2026", CouponStatus_Refactored.READY, 0)
    ));

    @Override
    public List<OrderAmount_Refactored> findOrderAmountsByUserId(long userId) {
        if (userId == 1L) {
            return List.of(
                    new OrderAmount_Refactored(10L, 10000),
                    new OrderAmount_Refactored(11L, 5000),
                    new OrderAmount_Refactored(12L, 30000)
            );
        }

        return List.of(
                new OrderAmount_Refactored(20L, 1000),
                new OrderAmount_Refactored(21L, 1000)
        );
    }

    @Override
    public Optional<Coupon_Refactored> findByCode(String code) {
        // 기존 코드: 못 찾으면 null을 반환했다.
        // 수정 코드: Optional로 없을 수 있음을 타입에 드러냈다.
        return coupons.stream()
                .filter(coupon -> coupon.getCode().equals(code))
                .findFirst();
    }

    @Override
    public Coupon_Refactored save(Coupon_Refactored coupon) {
        // 기존 코드: 같은 쿠폰 코드를 저장해도 데이터가 계속 add될 수 있었다.
        // 수정 코드: 같은 코드의 쿠폰은 교체해 현재 상태가 하나만 남도록 했다.
        coupons.removeIf(saved -> saved.getCode().equals(coupon.getCode()));
        coupons.add(coupon);
        return coupon;
    }
}

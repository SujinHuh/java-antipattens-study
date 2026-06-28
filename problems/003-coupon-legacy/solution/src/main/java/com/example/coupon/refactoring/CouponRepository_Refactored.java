package com.example.coupon.refactoring;

import java.util.List;
import java.util.Optional;

public interface CouponRepository_Refactored {
    // 기존 코드: 주문 id 목록을 조회한 뒤 반복문 안에서 금액을 다시 조회했다.
    // 수정 코드: 사용자 주문 금액을 한 번에 조회하는 계약으로 바꿨다.
    List<OrderAmount_Refactored> findOrderAmountsByUserId(long userId);

    Optional<Coupon_Refactored> findByCode(String code);

    Coupon_Refactored save(Coupon_Refactored coupon);
}


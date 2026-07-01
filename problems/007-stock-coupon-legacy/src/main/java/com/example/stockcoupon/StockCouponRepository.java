package com.example.stockcoupon;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// @Repository
public class StockCouponRepository {

    private static final Map<Long, StockCoupon> coupons = new HashMap<>();

    static {
        coupons.put(1L, new StockCoupon(
            1L,
            "first-come coupon",
            3,
            0,
            1,
            true,
            LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().plusHours(1)
        ));
    }

    public Optional<StockCoupon> findById(Long id) {
        return Optional.ofNullable(coupons.get(id));
    }

    public Optional<StockCoupon> findByIdForUpdate(Long id) {
        return Optional.ofNullable(coupons.get(id));
    }

    public void save(StockCoupon coupon) {
        coupons.put(coupon.id, coupon);
    }
}

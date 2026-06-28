package com.example.coupon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// @Repository
public class CouponRepository {
    private static final List<Coupon> COUPONS = new ArrayList<>();

    public List<Long> findOrderIdsByUserId(Long userId) {
        if (userId == 1L) {
            return Arrays.asList(10L, 11L, 12L);
        }

        return Arrays.asList(20L, 21L);
    }

    public int findOrderAmount(Long orderId) {
        if (orderId == 10L) {
            return 10000;
        }
        if (orderId == 11L) {
            return 5000;
        }
        if (orderId == 12L) {
            return 30000;
        }

        return 1000;
    }

    public Coupon findByCode(String code) {
        for (Coupon coupon : COUPONS) {
            if (coupon.code.equals(code)) {
                return coupon;
            }
        }

        return null;
    }

    public void save(Coupon coupon) {
        COUPONS.add(coupon);
        System.out.println("coupon saved. code=" + coupon.code);
    }
}

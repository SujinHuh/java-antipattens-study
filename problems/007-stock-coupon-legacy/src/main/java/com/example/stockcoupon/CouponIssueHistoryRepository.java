package com.example.stockcoupon;

import java.util.ArrayList;
import java.util.List;

// @Repository
public class CouponIssueHistoryRepository {

    private static final List<CouponIssueHistory> histories = new ArrayList<>();

    public boolean existsByUserIdAndCouponId(Long userId, Long couponId) {
        for (CouponIssueHistory history : histories) {
            if (history.userId.equals(userId) && history.couponId.equals(couponId)) {
                return true;
            }
        }
        return false;
    }

    public boolean existsByIdempotencyKey(String idempotencyKey) {
        for (CouponIssueHistory history : histories) {
            if (history.idempotencyKey != null && history.idempotencyKey.equals(idempotencyKey)) {
                return true;
            }
        }
        return false;
    }

    public void save(CouponIssueHistory history) {
        histories.add(history);
    }
}

package com.example.coupon.refactoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponService_RefactoredTest {
    @Test
    void applyVipCoupon() {
        CouponService_Refactored service = new CouponService_Refactored(
                new InMemoryCouponRepository_Refactored(),
                new CouponPolicy_Refactored()
        );

        CouponResponse_Refactored response = service.applyCoupon(
                new CouponRequest_Refactored(1L, "VIP-2026")
        );

        assertEquals(CouponStatus_Refactored.USED, response.status());
    }

    @Test
    void rejectExpiredCoupon() {
        InMemoryCouponRepository_Refactored repository = new InMemoryCouponRepository_Refactored();
        repository.save(new Coupon_Refactored(1L, "VIP-2026", CouponStatus_Refactored.EXPIRED, 0));

        CouponService_Refactored service = new CouponService_Refactored(
                repository,
                new CouponPolicy_Refactored()
        );

        CouponException_Refactored exception = assertThrows(
                CouponException_Refactored.class,
                () -> service.applyCoupon(new CouponRequest_Refactored(1L, "VIP-2026"))
        );

        assertEquals(CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void rejectInvalidRequest() {
        CouponException_Refactored exception = assertThrows(
                CouponException_Refactored.class,
                () -> new CouponRequest_Refactored(0L, "VIP-2026")
        );

        assertEquals(CouponErrorCode_Refactored.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void rejectUnknownCouponCode() {
        CouponService_Refactored service = new CouponService_Refactored(
                new InMemoryCouponRepository_Refactored(),
                new CouponPolicy_Refactored()
        );

        CouponException_Refactored exception = assertThrows(
                CouponException_Refactored.class,
                () -> service.applyCoupon(new CouponRequest_Refactored(1L, "UNKNOWN-2026"))
        );

        assertEquals(CouponErrorCode_Refactored.COUPON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectAlreadyUsedCoupon() {
        InMemoryCouponRepository_Refactored repository = new InMemoryCouponRepository_Refactored();
        CouponService_Refactored service = new CouponService_Refactored(
                repository,
                new CouponPolicy_Refactored()
        );

        service.applyCoupon(new CouponRequest_Refactored(1L, "VIP-2026"));

        CouponException_Refactored exception = assertThrows(
                CouponException_Refactored.class,
                () -> service.applyCoupon(new CouponRequest_Refactored(1L, "VIP-2026"))
        );

        assertEquals(CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void rejectCouponOwnedByOtherUser() {
        CouponService_Refactored service = new CouponService_Refactored(
                new InMemoryCouponRepository_Refactored(),
                new CouponPolicy_Refactored()
        );

        CouponException_Refactored exception = assertThrows(
                CouponException_Refactored.class,
                () -> service.applyCoupon(new CouponRequest_Refactored(1L, "EVENT-2026"))
        );

        assertEquals(CouponErrorCode_Refactored.COUPON_NOT_AVAILABLE, exception.getErrorCode());
    }

    @Test
    void returnOrderSummary() {
        CouponService_Refactored service = new CouponService_Refactored(
                new InMemoryCouponRepository_Refactored(),
                new CouponPolicy_Refactored()
        );

        OrderSummaryResponse_Refactored response = service.getOrderSummary(1L);

        assertEquals(1L, response.userId());
        assertEquals(3, response.orders().size());
    }
}

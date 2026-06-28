package com.example.coupon.refactoring;

public class Coupon_Refactored {
    // 기존 코드: Entity의 모든 필드가 public이라 외부에서 자유롭게 변경할 수 있었다.
    // 수정 코드: 필드를 private final로 감추고 생성 시점 이후 상태를 직접 바꾸지 못하게 했다.
    private final long userId;
    private final String code;
    private final CouponStatus_Refactored status;
    private final int discountAmount;

    public Coupon_Refactored(long userId, String code, CouponStatus_Refactored status, int discountAmount) {
        this.userId = userId;
        this.code = code;
        this.status = status;
        this.discountAmount = discountAmount;
    }

    public long getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public CouponStatus_Refactored getStatus() {
        return status;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }
}

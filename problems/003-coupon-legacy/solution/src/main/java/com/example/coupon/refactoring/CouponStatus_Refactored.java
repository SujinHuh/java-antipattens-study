package com.example.coupon.refactoring;

// 기존 코드: "READY", "USED", "EXPIRED" 같은 상태값을 문자열로 직접 다뤘다.
// 수정 코드: enum으로 허용 가능한 상태를 제한해 오타와 잘못된 상태값을 줄였다.
public enum CouponStatus_Refactored {
    READY,
    USED,
    EXPIRED
}

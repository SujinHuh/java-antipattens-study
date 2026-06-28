package com.example.coupon.refactoring;

import java.util.List;

public record OrderSummaryResponse_Refactored(
        long userId,
        List<OrderAmount_Refactored> orders
) {
}


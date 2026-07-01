package com.example.stockcoupon;

import java.time.LocalDateTime;

public class StockCoupon {

    public Long id;
    public String name;
    public int remainingQuantity;
    public int issuedCount;
    public int version;
    public boolean active;
    public LocalDateTime startsAt;
    public LocalDateTime endsAt;

    public StockCoupon(
        Long id,
        String name,
        int remainingQuantity,
        int issuedCount,
        int version,
        boolean active,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    ) {
        this.id = id;
        this.name = name;
        this.remainingQuantity = remainingQuantity;
        this.issuedCount = issuedCount;
        this.version = version;
        this.active = active;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}

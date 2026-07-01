package com.example.stockcoupon;

import java.time.LocalDateTime;

// @Service
public class StockCouponService {

    private final StockCouponRepository stockCouponRepository;
    private final CouponIssueHistoryRepository issueHistoryRepository;
    private final StockCouponEventPublisher eventPublisher;

    public StockCouponService(
        StockCouponRepository stockCouponRepository,
        CouponIssueHistoryRepository issueHistoryRepository,
        StockCouponEventPublisher eventPublisher
    ) {
        this.stockCouponRepository = stockCouponRepository;
        this.issueHistoryRepository = issueHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public StockCouponResponse issue(StockCouponRequest request) {
        if (request.userId == null || request.couponId == null) {
            throw new RuntimeException("invalid request");
        }

        if (issueHistoryRepository.existsByUserIdAndCouponId(request.userId, request.couponId)) {
            return new StockCouponResponse(request.couponId, request.userId, "ALREADY_ISSUED", 0);
        }

        return issueInternal(request);
    }

    // @Transactional
    public StockCouponResponse issueInternal(StockCouponRequest request) {
        StockCoupon coupon = stockCouponRepository.findById(request.couponId)
            .orElseThrow(() -> new StockCouponException("coupon not found"));

        if (!coupon.active) {
            return new StockCouponResponse(coupon.id, request.userId, "INACTIVE", coupon.remainingQuantity);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.startsAt) || now.isAfter(coupon.endsAt)) {
            return new StockCouponResponse(coupon.id, request.userId, "NOT_OPEN", coupon.remainingQuantity);
        }

        if (request.clientVersion != null && request.clientVersion < coupon.version) {
            return new StockCouponResponse(coupon.id, request.userId, "STALE_REQUEST", coupon.remainingQuantity);
        }

        if (coupon.remainingQuantity <= 0) {
            throw new StockCouponException("sold out");
        }

        int quantity = request.requestedQuantity <= 0 ? 1 : request.requestedQuantity;
        coupon.remainingQuantity = coupon.remainingQuantity - quantity;
        coupon.issuedCount = coupon.issuedCount + quantity;
        coupon.version = coupon.version + 1;

        CouponIssueHistory history = new CouponIssueHistory(
            request.userId,
            coupon.id,
            request.idempotencyKey,
            "ISSUED",
            now
        );

        try {
            stockCouponRepository.save(coupon);
            issueHistoryRepository.save(history);
            eventPublisher.publishIssued(history);
        } catch (Exception e) {
            return new StockCouponResponse(coupon.id, request.userId, "PENDING", coupon.remainingQuantity);
        }

        return new StockCouponResponse(coupon.id, request.userId, "ISSUED", coupon.remainingQuantity);
    }
}

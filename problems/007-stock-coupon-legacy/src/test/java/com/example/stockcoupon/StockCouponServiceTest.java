package com.example.stockcoupon;

public class StockCouponServiceTest {

    private final StockCouponRepository stockCouponRepository = new StockCouponRepository();
    private final CouponIssueHistoryRepository issueHistoryRepository = new CouponIssueHistoryRepository();
    private final StockCouponService service = new StockCouponService(
        stockCouponRepository,
        issueHistoryRepository,
        new StockCouponEventPublisher()
    );

    public void issue_decreases_remaining_quantity() {
        StockCoupon before = stockCouponRepository.findById(1L).orElseThrow();
        int beforeQuantity = before.remainingQuantity;

        StockCouponResponse response = service.issue(request(10L, "req-1", 1));
        StockCoupon after = stockCouponRepository.findById(1L).orElseThrow();

        assertEquals("ISSUED", response.status);
        assertEquals(beforeQuantity - 1, after.remainingQuantity);
        assertEquals(after.remainingQuantity, response.remainingQuantity);
    }

    public void same_user_second_request_is_not_issued_again() {
        StockCouponResponse first = service.issue(request(11L, "req-2", 1));
        StockCouponResponse second = service.issue(request(11L, "req-2-retry", 1));

        assertEquals("ISSUED", first.status);
        assertEquals("ALREADY_ISSUED", second.status);
    }

    public void sold_out_returns_exception() {
        service.issue(request(21L, "req-21", 1));
        service.issue(request(22L, "req-22", 1));
        service.issue(request(23L, "req-23", 1));

        assertThrows(StockCouponException.class, () -> service.issue(request(24L, "req-24", 1)));
    }

    private StockCouponRequest request(Long userId, String idempotencyKey, int quantity) {
        StockCouponRequest request = new StockCouponRequest();
        request.userId = userId;
        request.couponId = 1L;
        request.requestedQuantity = quantity;
        request.clientVersion = 1;
        request.idempotencyKey = idempotencyKey;
        return request;
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        throw new AssertionError("expected=" + expected + ", actual=" + actual);
    }

    private void assertThrows(Class<? extends RuntimeException> expectedType, Runnable executable) {
        try {
            executable.run();
        } catch (RuntimeException e) {
            if (expectedType.isInstance(e)) {
                return;
            }
            throw new AssertionError("unexpected exception type: " + e.getClass().getName(), e);
        }

        throw new AssertionError("expected exception: " + expectedType.getName());
    }
}

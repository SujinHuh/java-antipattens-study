package com.example.point.refactoring;

import java.util.List;

public class PointRefundService_RefactoredTest {

    public static void main(String[] args) {
        refundSuccess();
        paymentNotFound();
        otherUserPayment();
        alreadyRefunded();
        refundHistory();
    }

    private static void refundSuccess() {
        PointRefundService_Refactored service = newService();

        PointRefundResponse_Refactored response = service.refund(new PointRefundRequest_Refactored(1L, 10L, "user requested", "2026-06-01"));

        assertEquals("REFUNDED", response.status());
        assertEquals(3000, response.amount());
    }

    private static void paymentNotFound() {
        PointRefundService_Refactored service = newService();

        PointRefundException_Refactored exception = assertThrows(() ->
            service.refund(new PointRefundRequest_Refactored(99L, 10L, "missing", "2026-06-01"))
        );

        assertEquals(PointRefundErrorCode_Refactored.PAYMENT_NOT_FOUND, exception.getErrorCode());
    }

    private static void otherUserPayment() {
        PointRefundService_Refactored service = newService();

        PointRefundException_Refactored exception = assertThrows(() ->
            service.refund(new PointRefundRequest_Refactored(3L, 10L, "not mine", "2026-06-01"))
        );

        assertEquals(PointRefundErrorCode_Refactored.PAYMENT_NOT_OWNER, exception.getErrorCode());
    }

    private static void alreadyRefunded() {
        PointRefundService_Refactored service = newService();

        PointRefundException_Refactored exception = assertThrows(() ->
            service.refund(new PointRefundRequest_Refactored(2L, 10L, "again", "2026-06-01"))
        );

        assertEquals(PointRefundErrorCode_Refactored.ALREADY_REFUNDED, exception.getErrorCode());
    }

    private static void refundHistory() {
        PointRefundService_Refactored service = newService();

        List<PointRefundResponse_Refactored> history = service.getRefundHistory(10L);

        assertEquals(1, history.size());
        assertEquals(2L, history.get(0).paymentId());
    }

    private static PointRefundService_Refactored newService() {
        return new PointRefundService_Refactored(
            new InMemoryPointPaymentRepository_Refactored(),
            new PointRefundPolicy_Refactored(),
            payment -> {
            }
        );
    }

    private static PointRefundException_Refactored assertThrows(Runnable runnable) {
        try {
            runnable.run();
        } catch (PointRefundException_Refactored exception) {
            return exception;
        }
        throw new AssertionError("expected exception");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }
}

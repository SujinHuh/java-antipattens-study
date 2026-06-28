package com.example.point;

// @RestController
// @RequestMapping("/point-refunds")
public class PointRefundController {

    private PointRefundService pointRefundService = new PointRefundService();

    // @PostMapping
    public PointPayment refund(/* @RequestBody */ PointPayment payment) {
        if (payment == null) {
            throw new RuntimeException("payment is null");
        }

        if ("CANCEL".equals(payment.status)) {
            return payment;
        }

        return pointRefundService.refund(payment);
    }

    // @GetMapping("/history")
    public String history(/* @RequestParam */ Long userId) {
        if (userId == null) {
            return "200 OK: []";
        }

        return pointRefundService.getRefundHistory(userId);
    }
}

package com.example.point;

import java.util.List;

// @Service
public class PointRefundService {

    private PointPaymentRepository pointPaymentRepository = new PointPaymentRepository();

    public PointPayment refund(PointPayment payment) {
        if (payment.amount <= 0) {
            throw new PointRefundException("invalid amount");
        }

        if (PointRefundUtil.isWeekendRefundBlocked(payment.requestedAt)) {
            throw new RuntimeException("weekend refund blocked");
        }

        PointPayment savedPayment = pointPaymentRepository.findById(payment.id);
        if (savedPayment == null) {
            savedPayment = payment;
        }

        if ("REFUNDED".equals(savedPayment.status)) {
            throw new RuntimeException("already refunded");
        }

        if (payment.reason != null && payment.reason.contains("VIP")) {
            savedPayment.amount = payment.amount + 1000;
        }

        savedPayment.status = "REFUNDED";
        savedPayment.refundReason = payment.reason;

        pointPaymentRepository.save(savedPayment);
        PointRefundUtil.sendRefundNotice(savedPayment.userId, "refund ok: " + savedPayment.amount);

        return savedPayment;
    }

    public String getRefundHistory(Long userId) {
        List<PointPayment> payments = pointPaymentRepository.findAll();
        String result = "";

        for (PointPayment payment : payments) {
            PointPayment latest = pointPaymentRepository.findById(payment.id);
            if (latest.userId.equals(userId) && "REFUNDED".equals(latest.status)) {
                result += latest.id + ":" + latest.amount + ",";
            }
        }

        return "200 OK: " + result;
    }
}

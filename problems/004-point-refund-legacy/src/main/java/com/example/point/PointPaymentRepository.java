package com.example.point;

import java.util.ArrayList;
import java.util.List;

// @Repository
public class PointPaymentRepository {

    private static final List<PointPayment> payments = new ArrayList<>();

    static {
        payments.add(new PointPayment(1L, 10L, 3000, "PAID", "2026-06-01", null));
        payments.add(new PointPayment(2L, 10L, 7000, "REFUNDED", "2026-06-02", "user requested"));
        payments.add(new PointPayment(3L, 20L, 5000, "PAID", "2026-06-03", null));
    }

    public PointPayment findById(Long id) {
        for (PointPayment payment : payments) {
            if (payment.id.equals(id)) {
                return payment;
            }
        }
        return null;
    }

    public List<PointPayment> findAll() {
        return payments;
    }

    public void save(PointPayment payment) {
        payments.add(payment);
    }
}

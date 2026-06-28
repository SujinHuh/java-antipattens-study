package com.example.point.refactoring;

import java.util.List;
import java.util.Optional;

public interface PointPaymentRepository_Refactored {

    Optional<PointPayment_Refactored> findById(Long id);

    List<PointPayment_Refactored> findRefundedByUserId(Long userId);

    void save(PointPayment_Refactored payment);
}

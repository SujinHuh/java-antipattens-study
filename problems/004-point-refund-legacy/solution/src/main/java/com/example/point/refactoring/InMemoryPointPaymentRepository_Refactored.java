package com.example.point.refactoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPointPaymentRepository_Refactored implements PointPaymentRepository_Refactored {

    private final Map<Long, PointPayment_Refactored> payments = new LinkedHashMap<>();

    public InMemoryPointPaymentRepository_Refactored() {
        save(new PointPayment_Refactored(1L, 10L, 3000, PointRefundStatus_Refactored.PAID, "2026-06-01", null));
        save(new PointPayment_Refactored(2L, 10L, 7000, PointRefundStatus_Refactored.REFUNDED, "2026-06-02", "user requested"));
        save(new PointPayment_Refactored(3L, 20L, 5000, PointRefundStatus_Refactored.PAID, "2026-06-03", null));
    }

    @Override
    public Optional<PointPayment_Refactored> findById(Long id) {
        // 기존 코드: 조회 실패 시 null을 반환했다.
        // 수정 코드: 조회 실패를 Optional.empty()로 표현해 호출자가 명시적으로 처리하게 한다.
        return Optional.ofNullable(payments.get(id));
    }

    @Override
    public List<PointPayment_Refactored> findRefundedByUserId(Long userId) {
        // 기존 코드: findAll 후 반복문에서 findById를 다시 호출했다.
        // 수정 코드: 필요한 조건을 Repository 메서드로 표현해 반복 조회를 제거한다.
        List<PointPayment_Refactored> result = new ArrayList<>();
        for (PointPayment_Refactored payment : payments.values()) {
            if (payment.getUserId().equals(userId) && payment.getStatus() == PointRefundStatus_Refactored.REFUNDED) {
                result.add(payment);
            }
        }
        return result;
    }

    @Override
    public void save(PointPayment_Refactored payment) {
        // 기존 코드: save가 항상 add라서 같은 결제가 중복 저장될 수 있었다.
        // 수정 코드: id 기준으로 저장해 기존 결제 상태를 대체한다.
        payments.put(payment.getId(), payment);
    }
}

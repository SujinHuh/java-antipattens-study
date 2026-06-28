package com.example.point.refactoring;

import java.util.List;

public class PointRefundService_Refactored {

    private final PointPaymentRepository_Refactored pointPaymentRepository;
    private final PointRefundPolicy_Refactored pointRefundPolicy;
    private final RefundNotifier_Refactored refundNotifier;

    public PointRefundService_Refactored(
        PointPaymentRepository_Refactored pointPaymentRepository,
        PointRefundPolicy_Refactored pointRefundPolicy,
        RefundNotifier_Refactored refundNotifier
    ) {
        this.pointPaymentRepository = pointPaymentRepository;
        this.pointRefundPolicy = pointRefundPolicy;
        this.refundNotifier = refundNotifier;
    }

    // @Transactional
    public PointRefundResponse_Refactored refund(PointRefundRequest_Refactored request) {
        request.validate();

        // 기존 코드: 클라이언트가 보낸 PointPayment의 amount/status/userId를 신뢰했다.
        // 수정 코드: 서버 저장소에서 기존 결제를 조회하고, 그 상태를 기준으로 환불한다.
        PointPayment_Refactored payment = pointPaymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new PointRefundException_Refactored(PointRefundErrorCode_Refactored.PAYMENT_NOT_FOUND, "payment not found"));

        pointRefundPolicy.validateRefundable(payment, request);

        PointPayment_Refactored refunded = payment.refund(request.reason());
        pointPaymentRepository.save(refunded);

        // 기존 코드: static Util이 알림을 직접 처리했고 실패 정책이 보이지 않았다.
        // 수정 코드: 알림 책임을 Notifier로 분리한다. 실패 정책은 도메인 정책에 따라 별도 결정한다.
        refundNotifier.notifyRefunded(refunded);

        return PointRefundResponse_Refactored.from(refunded);
    }

    public List<PointRefundResponse_Refactored> getRefundHistory(Long userId) {
        if (userId == null) {
            throw new PointRefundException_Refactored(PointRefundErrorCode_Refactored.INVALID_REQUEST, "userId is required");
        }

        return pointPaymentRepository.findRefundedByUserId(userId)
            .stream()
            .map(PointRefundResponse_Refactored::from)
            .toList();
    }
}

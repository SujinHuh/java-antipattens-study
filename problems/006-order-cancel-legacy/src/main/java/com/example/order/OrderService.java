package com.example.order;

import java.time.LocalDateTime;

// @Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderCancelUtil orderCancelUtil;

    public OrderService() {
        this(new OrderRepository(), new OrderCancelUtil());
    }

    public OrderService(OrderRepository orderRepository) {
        this(orderRepository, new OrderCancelUtil());
    }

    public OrderService(OrderRepository orderRepository, OrderCancelUtil orderCancelUtil) {
        this.orderRepository = orderRepository;
        this.orderCancelUtil = orderCancelUtil;
    }

    // @Transactional
    public Order cancel(OrderCancelRequest request) {
        /*
         * 기존 코드:
         *
         * if (request == null || request.orderId == null) {
         *     throw new RuntimeException("invalid cancel request");
         * }
         *
         * Order order = orderRepository.findById(request.orderId)
         *         .orElse(createTemporaryOrder(request));
         *
         * if ("CANCELLED".equals(order.status)) {
         *     throw new OrderCancelException("already cancelled");
         * }
         *
         * if ("BLOCKED".equals(request.requestedStatus)) {
         *     throw new OrderCancelException("blocked order cannot be cancelled");
         * }
         *
         * int refundAmount = calculateRefundAmount(order, request);
         * OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);
         *
         * order.status = "CANCELLED";
         * order.cancelledAt = LocalDateTime.now();
         * order.cancelReason = request.reason;
         * orderRepository.save(order);
         *
         * 문제:
         * - RuntimeException과 도메인 예외가 섞여 있다.
         * - 조회 실패를 임시 주문 생성으로 숨긴다.
         * - 문자열 타입 if 분기가 Service에 몰려 있다.
         * - Entity public field를 직접 변경한다.
         * - 외부 PG API를 DB 상태 저장보다 먼저 호출한다.
         */

        /*
         * 리팩토링 코드:
         * - 실패 원인은 OrderCancelErrorCode로 통일한다.
         * - 주문이 없으면 임시 객체가 아니라 NOT_FOUND 예외를 던진다.
         * - 환불 정책은 OrderType enum으로 이동한다.
         * - 기존 Entity 상태 전이는 order.cancel(...)로 캡슐화한다.
         * - PG 호출 전에 DB에 취소 요청 상태를 먼저 저장한다.
         */
        if (request == null || request.orderId == null) {
            throw new OrderCancelException(OrderCancelErrorCode.INVALID_REQUEST);
        }

        Order order = orderRepository.findById(request.orderId)
                .orElseThrow(() -> new OrderCancelException(OrderCancelErrorCode.ORDER_NOT_FOUND));

        if ("CANCELLED".equals(order.status)) {
            throw new OrderCancelException(OrderCancelErrorCode.ALREADY_CANCELLED);
        }

        if ("BLOCKED".equals(request.requestedStatus)) {
            throw new OrderCancelException(OrderCancelErrorCode.BLOCKED_ORDER);
        }

        if (orderCancelUtil.isCancelWindowClosed(order.createdAt)) {
            throw new OrderCancelException(OrderCancelErrorCode.CANCEL_WINDOW_CLOSED);
        }

        int refundAmount = calculateRefundAmount(order, request);
        order.requestCancel(request.reason);
        orderRepository.save(order);

        orderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

        order.completeCancel(LocalDateTime.now());
        orderRepository.save(order);

        orderCancelUtil.sendAuditLog(order.userId, "order cancelled: " + order.id);
        return order;
    }

    private int calculateRefundAmount(Order order, OrderCancelRequest request) {
        /*
         * 기존 코드:
         *
         * if ("NORMAL".equals(order.type)) {
         *     return order.amount;
         * }
         * if ("PREORDER".equals(order.type)) {
         *     return (int) (order.amount * 0.9);
         * }
         * if ("DIGITAL".equals(order.type)) {
         *     throw new OrderCancelException("digital items cannot be cancelled");
         * }
         */
        if (request.requestedRefundAmount != null) {
            return request.requestedRefundAmount;
        }

        return OrderType.from(order.type).calculateRefundAmount(order.amount);
    }
}

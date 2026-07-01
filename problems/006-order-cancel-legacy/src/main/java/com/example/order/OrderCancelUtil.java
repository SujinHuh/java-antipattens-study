package com.example.order;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.logging.Logger;

// @Component
public class OrderCancelUtil {

    private static final Logger log = Logger.getLogger(OrderCancelUtil.class.getName());

    private final Clock clock;

    public OrderCancelUtil() {
        this(Clock.systemDefaultZone());
    }

    public OrderCancelUtil(Clock clock) {
        this.clock = clock;
    }

    /*
     * 기존 코드:
     *
     * public static boolean isCancelWindowClosed(LocalDateTime createdAt) {
     *     if (createdAt == null) {
     *         return true;
     *     }
     *     return LocalDateTime.now().minusMinutes(30).isAfter(createdAt);
     * }
     *
     * public static void requestExternalPgRefund(Long orderId, int refundAmount) {
     *     System.out.println("PG Refund Request: orderId=" + orderId + ", amount=" + refundAmount);
     * }
     *
     * public static void sendAuditLog(Long userId, String message) {
     *     System.out.println("audit log send to user " + userId + ": " + message);
     * }
     *
     * 문제:
     * - 취소 가능 시간 정책이 static Util에 숨어 있다.
     * - LocalDateTime.now()를 직접 호출해 테스트에서 현재 시간을 고정하기 어렵다.
     * - System.out.println으로 외부 PG 요청과 감사 로그를 흉내 내고 있어 운영 로그/추적이 어렵다.
     * - 취소 정책, 외부 환불 요청, 감사 로그라는 서로 다른 책임이 한 Util에 섞여 있다.
     */

    /*
     * 리팩토링 코드:
     * - Clock을 주입받아 테스트에서 시간을 고정할 수 있게 한다.
     * - println 대신 Logger를 사용한다.
     * - static 메서드 대신 인스턴스 메서드로 두어 테스트에서 fake/mock으로 대체할 수 있게 한다.
     * - 더 엄격히 나누면 isCancelWindowClosed는 OrderCancelPolicy,
     *   requestExternalPgRefund는 PgRefundClient,
     *   sendAuditLog는 AuditLogger/Notifier로 분리하는 것이 좋다.
     */
    public boolean isCancelWindowClosed(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        return LocalDateTime.now(clock).minusMinutes(30).isAfter(createdAt);
    }

    public void requestExternalPgRefund(Long orderId, int refundAmount) {
        log.info("PG Refund Request: orderId=" + orderId + ", amount=" + refundAmount);
    }

    public void sendAuditLog(Long userId, String message) {
        log.info("audit log send to user " + userId + ": " + message);
    }
}

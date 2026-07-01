# 트랜잭션과 외부 API 호출 순서

## 1. 핵심 원칙

`@Transactional`은 DB 작업을 하나의 트랜잭션으로 묶어 롤백할 수 있게 해줍니다.
하지만 이미 외부 시스템으로 나간 API 호출은 Spring DB 트랜잭션으로 되돌릴 수 없습니다.

> `@Transactional`은 DB만 롤백한다.
> 외부 PG/API 호출은 롤백하지 못한다.

---

## 2. 위험한 코드

```java
OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

order.cancel(request.reason, LocalDateTime.now());
orderRepository.save(order);
```

이 흐름에서는 외부 PG 환불 API를 먼저 호출하고, DB 저장을 나중에 수행합니다.

만약 PG 환불은 성공했는데 DB 저장이 실패하거나 트랜잭션이 롤백되면 아래처럼 상태가 갈라집니다.

```text
외부 PG: 환불 완료
우리 DB: 주문 ACTIVE
```

---

## 3. 더 안전한 흐름

```java
order.requestCancel(request.reason);
orderRepository.save(order);

OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

order.completeCancel(LocalDateTime.now());
orderRepository.save(order);
```

먼저 DB에 `CANCEL_REQUESTED` 같은 요청 상태를 저장하고, 그 다음 외부 PG를 호출합니다.
PG 호출이 성공하면 최종적으로 `CANCELLED` 상태를 저장합니다.

실무에서는 외부 호출을 트랜잭션 밖 이벤트, 큐, 아웃박스 패턴으로 분리하고 재시도/실패 상태를 함께 관리하는 방식이 더 안전합니다.

---

## 4. 007번 이벤트 발행과 트랜잭션 문제

007번 코드에서는 DB 저장과 이벤트 발행이 한 `try-catch` 안에 묶여 있습니다.

```java
try {
    stockCouponRepository.save(coupon);
    issueHistoryRepository.save(history);
    eventPublisher.publishIssued(history);
} catch (Exception e) {
    return new StockCouponResponse(coupon.id, request.userId, "PENDING", coupon.remainingQuantity);
}
```

여기서 핵심은 "세 메서드를 한 트랜잭션으로 묶으면 무조건 문제"라는 뜻이 아닙니다.

문제는 두 가지입니다.

1. **DB 저장과 외부 이벤트 발행은 같은 방식으로 롤백되지 않는다.**
   - `stockCouponRepository.save`, `issueHistoryRepository.save`는 DB 트랜잭션으로 롤백될 수 있습니다.
   - `eventPublisher.publishIssued`는 외부 메시지, 알림, 로그, 카프카, 웹훅 같은 외부 세계로 나갈 수 있습니다.
   - 외부로 이미 나간 이벤트는 DB 트랜잭션이 롤백되어도 자동으로 취소되지 않습니다.

2. **예외를 catch해서 정상 응답으로 바꾸면 트랜잭션 롤백을 방해할 수 있다.**
   - DB 저장 중 예외가 발생했는데 catch에서 `"PENDING"` 응답을 반환하면 메서드가 정상 종료된 것처럼 보입니다.
   - 그러면 Spring 트랜잭션은 롤백하지 않고 커밋을 시도할 수 있습니다.

### 시나리오

```text
case 1.
DB 저장 성공 -> 이벤트 발행 실패 -> catch 후 PENDING 반환
결과: DB에는 발급됐지만 외부 알림/후속 처리는 누락될 수 있음

case 2.
이벤트 발행 성공 -> 이후 DB 트랜잭션 롤백
결과: 외부에는 발급 이벤트가 나갔는데 DB에는 발급 이력이 없을 수 있음

case 3.
DB 저장 예외 -> catch 후 정상 반환
결과: 트랜잭션이 정상 종료로 오해되어 반쪽 커밋 또는 UnexpectedRollbackException 가능
```

### 개선 방향

- DB 저장은 하나의 트랜잭션으로 명확히 처리한다.
- 이벤트 발행은 트랜잭션 커밋 이후에 처리한다.
- 안정성이 중요하면 outbox 테이블에 이벤트를 저장하고 별도 퍼블리셔가 발행한다.
- DB 예외는 catch해서 성공/보류 응답으로 바꾸지 말고 롤백되도록 전파한다.

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    StockCoupon coupon = repository.findByIdForUpdate(request.couponId()).orElseThrow();
    coupon.issue(request.quantity());
    issueHistoryRepository.save(history);
    outboxRepository.save(EventMessage.couponIssued(history));
    return StockCouponResponse.issued(coupon);
}
```

면접용 문장:

```text
DB 저장과 이벤트 발행을 한 try-catch 안에 묶으면 DB 상태와 외부 이벤트 상태가 어긋날 수 있습니다.
DB 변경은 트랜잭션으로 확정하고, 이벤트는 커밋 이후 또는 outbox 패턴으로 분리하는 편이 안전합니다.
또한 DB 예외를 catch해서 정상 응답으로 바꾸면 롤백이 방해될 수 있으므로 주의해야 합니다.
```

---

## 5. 면접용 문장

> 외부 PG 호출은 DB 트랜잭션과 같은 원자성으로 묶을 수 없습니다.
> 따라서 트랜잭션 안에서 외부 API를 먼저 호출하면, API는 성공했지만 DB는 롤백되는 정합성 문제가 생길 수 있습니다.
> 먼저 DB에 취소 요청 상태를 저장한 뒤 외부 호출을 수행하고, 호출 결과에 따라 최종 상태를 다시 기록하는 구조가 더 안전합니다.

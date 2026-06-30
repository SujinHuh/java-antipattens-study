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

## 4. 면접용 문장

> 외부 PG 호출은 DB 트랜잭션과 같은 원자성으로 묶을 수 없습니다.
> 따라서 트랜잭션 안에서 외부 API를 먼저 호출하면, API는 성공했지만 DB는 롤백되는 정합성 문제가 생길 수 있습니다.
> 먼저 DB에 취소 요청 상태를 저장한 뒤 외부 호출을 수행하고, 호출 결과에 따라 최종 상태를 다시 기록하는 구조가 더 안전합니다.

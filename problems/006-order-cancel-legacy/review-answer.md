# 006. Order Cancel Legacy Review - 내 답변

## 내 답변

각 항목에는 가능하면 `문제점`, `왜 문제인지`, `실무 영향`, `개선 방향`을 같이 적어보세요.

## 먼저 볼 것

Service나 Repository를 분석할 때는 아래 흐름을 먼저 한 번 따라간다.

```text
입력값 -> 조회 -> 검증 -> 계산 -> 외부 호출 -> 저장 -> 결과 반환
```

- 입력값: Request DTO 값 중 서버가 믿으면 안 되는 값이 있는가?
- 조회: Repository 조회 실패가 예외로 중단되는가, 기본값/임시 객체로 계속 진행되는가?
- 검증: 저장된 Entity 상태 기준으로 판단하는가, 요청값 기준으로 판단하는가?
- 계산: 금액/상태/시간처럼 서버가 결정해야 하는 값을 요청에서 가져오는가?
- 외부 호출: PG/API/알림 호출이 DB 저장 전후 어디에 있는가?
- 저장: 생성/수정/상태 변경 의미가 분명한가?
- 결과 반환: Entity가 아니라 Response DTO로 필요한 값만 반환하는가?

---

### 1. Controller

#### 문제 1. Entity를 API 응답으로 그대로 반환한다

- 기존 코드:

```java
// @PostMapping("/cancel")
public Order cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
    return orderService.cancel(request);
}
```

- 문제점: Controller가 `Order` 엔티티를 그대로 응답으로 반환하고 있다.
- 왜 문제인지: `Order`는 DB 저장 구조와 도메인 상태에 가까운 객체인데, 이를 API 응답으로 그대로 노출하면 API 스펙이 엔티티 구조에 강하게 결합된다. 이후 엔티티 필드가 변경되거나 내부 관리용 필드가 추가되면 외부 응답에도 영향을 줄 수 있다.
- 실무 영향: `userId`, `status`, `type`, `cancelReason` 같은 내부 값이 의도치 않게 노출될 수 있고, DB/도메인 구조 변경이 API 호환성 문제로 이어질 수 있다.
- 개선 방향: 요청은 `OrderCancelRequest` 같은 요청 DTO로 받고, 응답은 `OrderCancelResponse` 같은 별도 응답 DTO 파일로 분리해서 필요한 값만 반환한다.
- 암기 포인트: `Request DTO`는 클라이언트가 보내는 요청 의도이고, `Response DTO`는 서버가 처리한 결과다. 요청 DTO를 그대로 응답으로 돌려주는 것이 아니라, 서버 처리 결과를 응답 DTO로 새로 구성한다.

- 리팩토링 코드:

```java
// @PostMapping("/cancel")
public OrderCancelResponse cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
    Order order = orderService.cancel(request);
    return OrderCancelResponse.from(order);
}
```

- 실제 코드 비교 위치: `src/main/java/com/example/order/OrderController.java`에도 기존 코드 주석과 리팩토링 코드를 함께 남긴다.
- 응답 DTO 위치: `src/main/java/com/example/order/OrderCancelResponse.java`로 분리한다.

#### 문제 2. 성공/실패 응답 기준과 HTTP 상태코드가 명확하지 않다

- 문제점: 현재 Controller 메서드는 객체만 반환하고 있어 취소 성공 시 `200 OK`로 응답할지, 응답 바디가 없다면 `204 No Content`로 응답할지 드러나지 않는다. 예외 발생 시에도 `400`, `404`, `409` 중 어떤 상태코드로 내려갈지 기준이 보이지 않는다.
- 왜 문제인지: HTTP 상태코드는 클라이언트가 성공/실패를 판단하는 1차 기준이다. 상태코드 기준이 없으면 프론트엔드, QA, 운영 로그에서 실패 원인을 일관되게 해석하기 어렵다.
- 실무 영향: 필수값 누락, 주문 없음, 이미 취소된 주문처럼 서로 다른 실패가 같은 응답으로 뭉개질 수 있다. 그러면 클라이언트가 사용자에게 정확한 메시지를 보여주기 어렵고, 장애 분석도 늦어진다.
- 개선 방향: 성공 시 응답 바디를 내려주면 `200 OK`, 바디가 필요 없으면 `204 No Content`를 사용한다. 실패는 요청값 오류는 `400 Bad Request`, 주문 없음은 `404 Not Found`, 이미 취소됨/취소 불가 상태는 `409 Conflict`처럼 구분한다. 실제 Spring 코드에서는 `ResponseEntity<OrderCancelResponse>`와 `@RestControllerAdvice` 기반 공통 예외 처리를 함께 사용한다.

---

### 2. Service

#### 문제 1. 예외 타입과 에러 응답 기준이 일관되지 않다

- 기존 코드:

```java
if (request == null || request.orderId == null) {
    throw new RuntimeException("invalid cancel request");
}

if ("CANCELLED".equals(order.status)) {
    throw new OrderCancelException("already cancelled");
}
```

- 문제점: 어떤 실패는 `RuntimeException`으로 던지고, 어떤 실패는 `OrderCancelException`으로 던진다. `OrderCancelException`도 문자열 메시지만 가지고 있어 에러 코드나 HTTP 상태 매핑 기준이 드러나지 않는다.
- 왜 문제인지: 예외 타입이 일관되지 않으면 ControllerAdvice에서 어떤 예외를 어떤 HTTP 상태코드로 내려야 하는지 정하기 어렵다. 문자열 메시지만 있으면 프론트엔드/QA/운영에서 안정적인 에러 분류를 하기 어렵다.
- 실무 영향: 필수값 누락, 이미 취소됨, 취소 기간 만료 같은 실패가 제각각 처리되거나 모두 같은 에러로 뭉개질 수 있다.
- 개선 방향: 주문 취소 도메인 예외로 통일하고, `INVALID_REQUEST`, `ALREADY_CANCELLED`, `CANCEL_WINDOW_CLOSED` 같은 에러 코드를 둔다. 요청 형식 오류는 `400`, 이미 취소됨/취소 불가 상태는 `409`처럼 전역 예외 처리에서 매핑한다.

- 리팩토링 코드:

```java
if (request == null || request.orderId == null) {
    throw new OrderCancelException(OrderCancelErrorCode.INVALID_REQUEST);
}

Order order = orderRepository.findById(request.orderId)
        .orElseThrow(() -> new OrderCancelException(OrderCancelErrorCode.ORDER_NOT_FOUND));
```

#### 문제 2. 주문 타입별 환불 정책이 if문으로 Service에 박혀 있다

- 기존 코드:

```java
if ("NORMAL".equals(order.type)) {
    return order.amount;
}
if ("PREORDER".equals(order.type)) {
    return (int) (order.amount * 0.9);
}
if ("DIGITAL".equals(order.type)) {
    throw new OrderCancelException("digital items cannot be cancelled");
}
```

- 문제점: `NORMAL`, `PREORDER`, `DIGITAL` 같은 주문 타입 문자열을 Service에서 직접 비교하고 있다.
- 왜 문제인지: 주문 타입이 추가될 때마다 Service의 if문을 수정해야 하므로 OCP에 약하다. 문자열 비교라 오타에도 취약하고, 타입별 정책이 어디에 모여 있는지 한눈에 보기도 어렵다.
- 실무 영향: 새 주문 타입이나 환불 정책이 추가될 때 기존 취소 로직을 계속 건드리게 되어 회귀 버그가 생길 수 있다.
- 개선 방향: 우선 문자열 상태값/타입을 enum으로 분리한다. 더 나아가 타입별 환불 정책이 커지면 `OrderCancelPolicy` 또는 전략 클래스로 분리한다.

- 리팩토링 코드:

```java
return OrderType.from(order.type).calculateRefundAmount(order.amount);
```

#### 문제 3. Service가 Entity의 public field를 직접 변경한다

- 기존 코드:

```java
order.status = "CANCELLED";
order.cancelledAt = LocalDateTime.now();
order.cancelReason = request.reason;
orderRepository.save(order);
```

- 문제점: Service가 `Order`의 내부 필드를 직접 바꾼다.
- 왜 문제인지: 주문이 취소될 때 어떤 값이 함께 바뀌어야 하는지, 어떤 상태에서만 취소 가능한지 같은 도메인 규칙이 Entity 밖에 흩어진다. public field라 다른 코드에서도 마음대로 상태를 바꿀 수 있다.
- 실무 영향: `status`만 바꾸고 `cancelledAt`은 누락하는 식의 불완전한 상태 변경이 생길 수 있다.
- 개선 방향: 단순히 빌더로 새 객체를 만드는 것보다, 기존 주문의 상태 변경을 표현하는 `order.cancel(reason, now)` 같은 도메인 메서드를 두는 것이 더 적절하다. 빌더는 객체 생성에는 도움이 되지만, 이미 존재하는 주문의 상태 전이 규칙을 보호하는 핵심 수단은 캡슐화된 상태 변경 메서드다.

- 리팩토링 코드:

```java
order.requestCancel(request.reason);
orderRepository.save(order);

OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

order.completeCancel(LocalDateTime.now());
orderRepository.save(order);
```

#### 문제 4. 외부 PG API 호출이 DB 저장보다 먼저 실행된다

- API 호출 위치:

```java
OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);
```

- 기존 코드:

```java
OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

order.cancel(request.reason, LocalDateTime.now());
orderRepository.save(order);
```

- 문제점: 외부 PG 환불 API를 먼저 호출하고, DB 주문 상태 저장을 나중에 한다.
- 왜 문제인지: `@Transactional`은 DB 작업만 롤백한다. PG 환불 API는 이미 외부 시스템에 나간 요청이라 DB 트랜잭션이 롤백되어도 함께 되돌릴 수 없다.
- 실무 영향: PG 환불은 성공했는데 DB 저장이 실패하면, 외부에서는 환불 완료이고 우리 DB에는 주문이 `ACTIVE`로 남는 결제 정합성 문제가 생길 수 있다.
- 개선 방향: DB에 먼저 `CANCEL_REQUESTED` 같은 요청 상태를 저장하고 커밋한 뒤, 외부 PG API를 호출한다. PG 성공 후 최종 `CANCELLED` 상태를 다시 저장한다. 실무에서는 이벤트/큐/아웃박스 패턴으로 더 안전하게 분리할 수 있다.

- 리팩토링 코드:

```java
order.requestCancel(request.reason);
orderRepository.save(order);

OrderCancelUtil.requestExternalPgRefund(order.id, refundAmount);

order.completeCancel(LocalDateTime.now());
orderRepository.save(order);
```

#### Service에서 아직 다시 봐야 할 개념

- `requestedRefundAmount`: 환불 금액은 클라이언트가 보낸 값을 신뢰하면 안 되고 서버 정책으로 계산해야 한다.
- `requestedStatus`: 주문 상태 판단은 요청 DTO가 아니라 저장된 `order.status` 기준으로 해야 한다.
- `request.userId`: 요청 userId와 저장된 주문의 `order.userId`가 같은지 소유자 검증이 필요하다.
- `LocalDateTime.now()`: 시간값을 직접 호출하면 테스트가 어려우므로 `Clock` 또는 시간 정책 분리를 고려한다.
- `OrderCancelUtil`: 외부 PG 호출과 감사 로그가 static Util이라 테스트 대체가 어렵다.

---

### 3. Repository

#### 문제 1. `save()`가 수정과 생성을 구분하지 않고 항상 `add()`만 한다

- 기존 코드:

```java
public void save(Order order) {
    orders.add(order);
}
```

- 문제점: 기존 주문을 수정하는 상황에서도 리스트에 계속 새 항목을 추가한다.
- 왜 문제인지: 같은 `id`를 가진 주문이 여러 개 쌓일 수 있고, `findById()`는 먼저 발견한 값을 반환하므로 최신 상태가 반영되지 않을 수 있다.
- 실무 영향: 주문 취소 후에도 같은 ID의 `ACTIVE` 주문과 `CANCELLED` 주문이 동시에 존재하는 데이터 정합성 문제가 생길 수 있다.
- 개선 방향: 인메모리 저장소라면 `List`보다 `Map<Long, Order>`로 관리하고, `save()`는 `put(order.id, order)`처럼 같은 ID를 덮어쓰게 한다. 실제 실무에서는 DB/JPA의 식별자 기반 update를 사용한다.

- 리팩토링 코드 예시:

```java
private static final Map<Long, Order> orders = new HashMap<>();

public Optional<Order> findById(Long id) {
    return Optional.ofNullable(orders.get(id));
}

public void save(Order order) {
    orders.put(order.id, order);
}
```

#### 문제 2. `Optional.empty()`는 Repository 문제가 아니라 Service 처리 지점이 중요하다

- 기존 코드:

```java
public Optional<Order> findById(Long id) {
    for (Order order : orders) {
        if (order.id.equals(id)) {
            return Optional.of(order);
        }
    }
    return Optional.empty();
}
```

- 판단: Repository가 조회 실패를 `Optional.empty()`로 표현하는 것은 괜찮다.
- 왜 괜찮은지: `null` 대신 `Optional.empty()`를 반환하면 호출하는 Service가 “값이 없을 수 있음”을 타입으로 인식하게 된다.
- 진짜 문제 위치: Service 코드에서 `orderRepository.findById(request.orderId)` 호출 결과를 어떻게 처리하는지가 핵심이다. `empty`를 임시 주문 생성으로 숨기면 안 된다.

```java
// 문제 있는 처리
orderRepository.findById(request.orderId)
        .orElse(createTemporaryOrder(request));
```

- 개선 방향: 조회 실패는 정상 주문처럼 보정하지 말고 도메인 예외로 명확히 처리한다.

```java
Order order = orderRepository.findById(request.orderId)
        .orElseThrow(() -> new OrderCancelException(OrderCancelErrorCode.ORDER_NOT_FOUND));
```

---

### 4. Util

- 

---

### 5. DTO/Entity

- 

---

### 6. Exception

- 

---

### 7. Test

- 

---

### 8. 면접에서 1분 답변으로 말한다면

*여기에는 이번 문제에서 발견한 가장 심각한 설계적 결함 2~3가지를 선정하여, 실제 면접장이라 생각하고 1분 내외로 답변할 스크립트를 작성해보세요.*

- 

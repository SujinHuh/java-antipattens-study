# 올바른 HTTP 응답 처리 (ResponseEntity와 응답 DTO)

Controller가 클라이언트에게 요청 처리 결과를 돌려줄 때(응답), 어떤 형태로 반환해야 하는지에 대한 설계 원칙입니다.

---

## 1. 안티패턴 1: Controller가 Entity를 직접 반환

```java
// ❌ 잘못된 예시 (004번 문제 Controller)
public PointPayment refund(PointPayment payment) {
    return pointRefundService.refund(payment); // Entity를 그대로 반환
}
```

### 왜 문제인가?
1. **데이터베이스 구조 노출**: Entity는 DB 테이블과 1:1로 매핑되어 있습니다. Entity를 직접 반환하면 DB 구조가 외부에 그대로 유출됩니다.
2. **민감한 정보 유출**: 사용자 비밀번호, 내부 상태값 등 클라이언트가 알 필요가 없거나 알아서는 안 되는 정보까지 함께 노출될 위험이 큽니다.
3. **유연성 부족**: 화면이나 클라이언트 요구사항에 맞춰 응답 데이터의 포맷을 조금만 바꾸려 해도 DB 테이블 구조(Entity)를 변경해야 하는 상황이 발생합니다.

### 해결책: 응답 전용 DTO (`ResponseDTO`) 반환
클라이언트가 딱 필요한 정보만 필드로 선언된 DTO를 별도로 만들어 반환합니다.
```java
// ✅ 올바른 예시
public PointRefundResponse refund(...) {
    PointPayment payment = pointRefundService.refund(...);
    return PointRefundResponse.from(payment); // 필요한 정보만 DTO에 담아 반환
}
```

---

## 2. 안티패턴 2: HTTP 상태를 흉내 낸 문자열 반환

```java
// ❌ 잘못된 예시 (004번 문제 Controller)
public String history(Long userId) {
    if (userId == null) {
        return "200 OK: []"; // 껍데기만 200 OK인 문자열 반환
    }
    // ...
}
```

### 왜 문제인가?
- `"200 OK: []"`는 실제 HTTP 응답 상태 코드(Status Code)가 아니라 **단순한 텍스트 문자열**입니다.
- 웹 브라우저나 클라이언트 앱은 HTTP Response Header의 **실제 상태 코드(200, 400, 500 등)**를 보고 성공/실패 여부를 판단합니다. 
- 문자열로 성공/실패를 내보내면 클라이언트는 요청이 성공했음에도 HTTP 레이어에서는 그냥 문자열 본문만 받게 되어, 매번 문자열을 파싱해서 성공 여부를 직접 판별해야 하는 비효율이 발생합니다.

---

## 3. 올바른 해결책: `ResponseEntity` 사용하기

Spring에서 제공하는 `ResponseEntity` 객체는 **실제 HTTP 응답 상태 코드, 헤더, 그리고 본문(Body)**을 온전히 커스텀하여 전송할 수 있도록 도와줍니다.

```java
// ✅ 올바른 예시
@PostMapping("/refund")
public ResponseEntity<PointRefundResponse> refund(@RequestBody PointRefundRequest request) {
    PointPayment result = pointRefundService.refund(request);
    
    // HTTP 실제 상태코드 200 OK와 함께 Body에 DTO를 실어 반환
    return ResponseEntity.ok(PointRefundResponse.from(result));
}

@GetMapping("/history")
public ResponseEntity<List<PointRefundHistoryResponse>> history(@RequestParam Long userId) {
    if (userId == null) {
        // 실제 HTTP 상태코드 200 OK와 함께 빈 리스트 반환
        return ResponseEntity.ok(Collections.emptyList());
    }
    
    List<PointRefundHistoryResponse> history = pointRefundService.getRefundHistory(userId);
    return ResponseEntity.ok(history);
}
```

### 핵심 요약
- Controller는 절대로 **Entity를 직접 반환하지 않고, 응답 전용 DTO를 반환**합니다.
- HTTP 응답을 제어할 때는 임의의 문자열 대신, **Spring의 `ResponseEntity`를 사용하여 표준 HTTP 상태 코드를 제공**해야 합니다.

---

## 4. `ResponseEntity<응답DTO>` 사용 기준

### 기본 원칙

Controller는 **`ResponseEntity<응답DTO>`** 형태로 반환하는 것이 가장 권장됩니다.

```java
// ✅ 가장 명확하고 권장되는 형태
@PostMapping("/refund")
public ResponseEntity<PointRefundResponse> refund(@RequestBody PointRefundRequest request) {
    PointRefundResponse response = service.refund(request);
    return ResponseEntity.ok(response);         // 200 OK + 응답DTO
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();  // 204 No Content (body 없음)
}
```

### 예외적으로 `ResponseEntity` 없이 써도 괜찮은 경우

```java
// ✅ 항상 200 OK이고 단순 조회라면 이렇게도 가능
@GetMapping("/{id}")
public PointRefundResponse getRefund(@PathVariable Long id) {
    return service.getRefund(id); // Spring이 자동으로 200 OK + JSON 변환
}
```

단, 이 방식은 HTTP 상태코드를 세밀하게 제어하기 어려워 실무에서는 **일관성을 위해 항상 `ResponseEntity`를 쓰는 팀도 많습니다.**

### 절대로 하면 안 되는 것들

| 잘못된 반환 타입 | 문제점 |
|----------------|--------|
| `Entity` 직접 반환 | DB 구조 노출, 민감 정보 유출 |
| `"200 OK: success"` 문자열 | 실제 HTTP 상태코드가 아님 |
| `Map<String, Object>` | 응답 구조가 타입으로 고정되지 않아 유지보수 어려움 |

### 상황별 정리

| 상황 | 권장 반환 타입 |
|------|-------------|
| 생성/수정/삭제 등 상태코드 제어 필요 | `ResponseEntity<응답DTO>` |
| 단순 조회, 항상 200 OK | `응답DTO`만 반환해도 OK |
| Entity, 문자열, Map 반환 | ❌ 절대 안 됨 |

---

## 5. 200, 204, 400, 404, 409 구분 기준

Controller는 단순히 객체를 반환하는 곳이 아니라, 요청 처리 결과를 HTTP 응답으로 매핑하는 계층입니다.
따라서 성공/실패 상황별로 상태코드 기준이 있어야 합니다.

| 상황 | 예시 | 권장 상태코드 |
|------|------|--------------|
| 처리 성공 + 응답 바디 있음 | 주문 취소 후 취소 결과 DTO 반환 | `200 OK` |
| 처리 성공 + 응답 바디 없음 | 삭제/취소 성공 후 내려줄 데이터 없음 | `204 No Content` |
| 요청 형식이나 입력값이 잘못됨 | 필수값 누락, 음수 금액, 형식 오류 | `400 Bad Request` |
| 대상 리소스를 찾을 수 없음 | 존재하지 않는 주문 ID | `404 Not Found` |
| 리소스의 현재 상태와 충돌 | 이미 취소된 주문, 취소 불가 상태 | `409 Conflict` |

### 주문 취소 API에 적용하면

```java
// 성공: 취소 결과를 내려주면 200
return ResponseEntity.ok(OrderCancelResponse.from(order));

// 성공: 내려줄 바디가 없다면 204
return ResponseEntity.noContent().build();

// 실패 예시
// request.orderId == null        -> 400 Bad Request
// 주문을 찾지 못함              -> 404 Not Found
// 이미 취소됨 / 취소 기간 지남   -> 409 Conflict
```

> 핵심은 "실패했다"를 모두 `400`으로 뭉개지 않는 것입니다.
> 요청 자체가 틀린 것인지, 대상이 없는 것인지, 현재 상태 때문에 처리할 수 없는 것인지 구분해야 API 계약이 선명해집니다.

---

## 6. 006번 Order Cancel Controller 예시

### 기존 코드

```java
// @PostMapping("/cancel")
public Order cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
    return orderService.cancel(request);
}
```

### 문제점

- `Order` 엔티티를 API 응답으로 그대로 반환한다.
- API 응답 스펙이 DB/도메인 구조와 강하게 결합된다.
- `userId`, `status`, `type`, `cancelReason` 같은 내부 필드가 의도치 않게 노출될 수 있다.
- 성공 응답이 `200 OK`인지 `204 No Content`인지, 실패 응답이 `400`, `404`, `409` 중 무엇인지 코드에서 드러나지 않는다.

### 수정된 코드

`OrderController.java`

```java
// @PostMapping("/cancel")
public OrderCancelResponse cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
    Order order = orderService.cancel(request);
    return OrderCancelResponse.from(order);
}
```

`OrderCancelResponse.java`

```java
public class OrderCancelResponse {
    public final Long orderId;
    public final Long userId;
    public final String status;
    public final String cancelledAt;

    private OrderCancelResponse(Long orderId, Long userId, String status, String cancelledAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public static OrderCancelResponse from(Order order) {
        return new OrderCancelResponse(
                order.id,
                order.userId,
                order.status,
                order.cancelledAt == null ? null : order.cancelledAt.toString()
        );
    }
}
```

> 빠른 암기: `OrderCancelRequest`는 요청 의도이고, `OrderCancelResponse`는 서버 처리 결과다.
> Response DTO는 Controller 내부 임시 클래스보다 별도 파일로 분리하는 것이 정석이다.

### 실무 코드라면

학습용 코드에는 Spring 의존성이 없으므로 `ResponseEntity`를 직접 사용하지 않았지만, 실제 Spring Controller라면 아래처럼 성공 상태코드까지 명확히 표현할 수 있습니다.

```java
// @PostMapping("/cancel")
public ResponseEntity<OrderCancelResponse> cancelOrder(@Valid @RequestBody OrderCancelRequest request) {
    Order order = orderService.cancel(request);
    return ResponseEntity.ok(OrderCancelResponse.from(order));
}
```

예외 상황은 Controller에서 직접 `try-catch`로 잡기보다 `@RestControllerAdvice`에서 공통 처리합니다.

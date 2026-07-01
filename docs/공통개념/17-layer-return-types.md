# 계층별 반환 타입 원칙

Controller → Service → Repository 각 계층이 무엇을 반환해야 하는지에 대한 원칙입니다.

---

## 1. 계층별 반환 타입 요약

```
Controller  →  ResponseEntity<ResponseDTO>  (HTTP 응답 포장)
Service     →  ResponseDTO 또는 도메인 객체  (비즈니스 결과)
Repository  →  Entity 또는 Optional<Entity> (DB 조회 결과)
```

---

## 2. Controller — `ResponseEntity<ResponseDTO>`

Controller는 HTTP 응답을 책임지는 계층입니다.
단, `ResponseEntity`가 항상 유일한 정답은 아닙니다.
항상 200 OK인 단순 조회라면 Response DTO만 직접 반환해도 됩니다.
다만 상태코드, 헤더, Location, 204 No Content, 실패 응답을 명확히 제어해야 하면 `ResponseEntity`나 `@RestControllerAdvice`가 필요합니다.

```java
// ✅ 올바른 반환
@PostMapping("/refund")
public ResponseEntity<PointRefundResponse> refund(@RequestBody PointRefundRequest request) {
    PointRefundResponse response = service.refund(request);
    return ResponseEntity.ok(response);           // 200 OK + ResponseDTO
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();    // 204 No Content
}

// ✅ 단순 조회는 ResponseDTO만 반환도 가능 (Spring이 자동으로 200 OK 처리)
@GetMapping("/{id}")
public PointRefundResponse get(@PathVariable Long id) {
    return service.get(id);
}
```

**절대 안 되는 것:**
```java
// ❌ Entity 직접 반환
public PointPayment refund(...) { ... }

// ❌ 문자열로 상태 흉내
public String refund(...) { return "200 OK: success"; }

// ❌ Map으로 응답 구조 직접 조립
public Map<String, Object> refund(...) { return Map.of("status", 200, ...); }

// ❌ Service가 ResponseEntity를 만들면 Controller에서 그대로 반환하는 것
// Service는 HTTP 응답을 알면 안 됨!
```

### 007번 Controller에서 헷갈린 포인트

```java
public StockCouponResponse issue(StockCouponRequest request) {
    if (request == null) {
        return new StockCouponResponse(null, null, "FAILED", 0);
    }
    return stockCouponService.issue(request);
}
```

좋은 점:

- Controller가 Service를 생성자 주입으로 받는다.
- Entity를 직접 반환하지 않고 Response DTO를 반환한다.
- 핵심 비즈니스 로직은 Service에 맡긴다.

문제점:

- `request == null`을 성공 응답처럼 보이는 실패 DTO로 직접 만든다.
- 실패가 HTTP 상태코드가 아니라 응답 바디의 문자열 `status`에만 들어간다.
- `@Valid` 같은 요청 형식 검증 경계가 보이지 않는다.

면접 답변:

```text
Controller가 얇고 Response DTO를 반환하는 점은 좋지만, null 요청을 FAILED 응답 DTO로 직접 만들어 반환하면 실제 HTTP 상태는 성공처럼 보일 수 있습니다.
요청 형식 검증은 @Valid와 Request DTO에 맡기고, 실패 응답은 공통 예외 처리에서 400/404/409 같은 HTTP 상태로 일관되게 매핑하는 편이 좋습니다.
```

---

## 3. Service — `ResponseDTO` 또는 도메인 객체

Service는 비즈니스 로직을 처리하는 계층입니다. HTTP 응답(ResponseEntity)을 직접 만들면 안 됩니다.

### 방식 ①: Service가 ResponseDTO를 직접 반환 (실무에서 많이 사용)
```java
@Service
public class PointRefundService {
    public PointRefundResponse refund(PointRefundRequest request) {
        PointPayment payment = repository.findById(request.getId())
            .orElseThrow(() -> new NotFoundException("결제 없음"));
        // 비즈니스 로직 처리...
        payment.markAsRefunded();
        repository.save(payment);
        return PointRefundResponse.from(payment); // Service가 DTO 변환까지
    }
}
```

### 방식 ②: Service가 도메인 객체 반환, Controller에서 DTO 변환
```java
@Service
public class PointRefundService {
    public PointPayment refund(PointRefundRequest request) {
        // 비즈니스 로직 처리...
        return payment; // 도메인 객체 반환
    }
}

// Controller에서 DTO로 변환
@RestController
public class PointRefundController {
    public ResponseEntity<PointRefundResponse> refund(...) {
        PointPayment payment = service.refund(request);
        return ResponseEntity.ok(PointRefundResponse.from(payment));
    }
}
```

> **두 방식 모두 맞습니다.** 팀 컨벤션에 따라 선택합니다.

**절대 안 되는 것:**
```java
// ❌ Service가 ResponseEntity를 반환 (HTTP는 Controller 책임!)
public ResponseEntity<PointRefundResponse> refund(...) { ... }

// ❌ Service가 HTTP 상태 문자열을 만들어 반환
public String refund(...) { return "200 OK: refunded"; }
```

---

## 4. Repository — `Entity` 또는 `Optional<Entity>`

Repository는 DB와 통신하는 계층입니다.

```java
// ✅ 단건 조회 → Optional로 반환 (null 반환 금지!)
public Optional<PointPayment> findById(Long id) {
    return Optional.ofNullable(store.get(id));
}

// ✅ 목록 조회 → List<Entity> 반환
public List<PointPayment> findAll() {
    return Collections.unmodifiableList(payments); // 외부 변경 방지
}

// ✅ 저장 → void 또는 저장된 Entity 반환
public void save(PointPayment payment) { ... }
```

**절대 안 되는 것:**
```java
// ❌ null 반환 (NPE 위험!)
public PointPayment findById(Long id) {
    return null; // 못 찾으면 null
}

// ❌ 내부 mutable List를 그대로 반환 (외부에서 수정 가능!)
public List<PointPayment> findAll() {
    return payments; // 원본 List 그대로 노출
}
```

---

## 5. 한눈에 보는 원칙 표

| 계층 | 받는 것 | 반환하는 것 | 절대 반환하면 안 되는 것 |
|------|---------|-----------|----------------------|
| **Controller** | `RequestDTO` | `ResponseEntity<ResponseDTO>` | Entity, 문자열, Map |
| **Service** | `RequestDTO` | `ResponseDTO` 또는 도메인 객체 | `ResponseEntity`, HTTP 문자열 |
| **Repository** | `Entity` | `Optional<Entity>`, `List<Entity>` | `null`, 원본 mutable List |

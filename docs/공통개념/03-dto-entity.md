# DTO vs Entity — 역할과 책임의 분리

## 1. Entity란?

**DB 테이블의 한 행(Row)을 자바 객체로 표현한 클래스**입니다.
Spring Data JPA 환경에서는 `@Entity` 어노테이션이 붙으며, DB와 직접 매핑됩니다.

```java
// Entity: DB 테이블과 1:1 대응
public class Reservation {
    public long id;          // DB PK — 서버/DB가 생성
    public long userId;
    public long roomId;
    public int startHour;
    public int endHour;
    public String status;    // 비즈니스 로직이 결정 — 서버 전용
    public String createdAt; // 서버가 현재 시각으로 채움 — 서버 전용
}
```

---

## 2. DTO(Data Transfer Object)란?

**계층 간에 데이터를 운반하기 위한 단순한 그릇(Carrier) 객체**입니다.
비즈니스 로직이 없고, 순수하게 데이터를 담아 이동시키는 역할만 합니다.

- **Request DTO**: 클라이언트 → 서버 방향 입력값
- **Response DTO**: 서버 → 클라이언트 방향 결과값

면접 직전 암기 문장:

> Request DTO는 "클라이언트가 무엇을 해달라고 요청하는지"를 담는다.
> Response DTO는 "서버가 처리한 결과를 어떻게 보여줄지"를 담는다.
> 요청 DTO를 그대로 응답으로 돌려주는 것이 아니라, 서버 처리 결과를 응답 DTO로 새로 구성한다.

```java
// Request DTO: 클라이언트가 보내는 '의도(Intent)'만 담아야 함
public class ReservationRequest {
    public long userId;
    public long roomId;
    public int startHour;
    public int endHour;
    // status   → ❌ 없어야 함! 서버 비즈니스 로직이 결정
    // createdAt → ❌ 없어야 함! 서버가 현재 시각으로 채움
    // id       → ❌ 없어야 함! DB가 생성
}
```

---

## 3. 어느 필드가 어디 있어야 하는가?

> **핵심 질문: "이 필드, 클라이언트가 결정하는가? 서버가 결정하는가?"**

| 필드 | 누가 결정? | 있어야 할 곳 |
|------|-----------|------------|
| `userId`, `roomId` | 클라이언트 (어떤 방을 예약할지) | Request DTO ✅ |
| `startHour`, `endHour` | 클라이언트 (몇 시에 할지) | Request DTO ✅ |
| `id` | 서버/DB (자동 생성) | Entity ✅, Request DTO ❌ |
| `status` | 서버 비즈니스 로직 ("CONFIRMED" 등) | Entity ✅, Request DTO ❌ |
| `createdAt` | 서버 (`LocalDateTime.now()`) | Entity ✅, Request DTO ❌ |

---

## 4. Entity를 직접 외부에 노출하면 안 되는 이유

1. **보안**: `password`, `internalScore` 등 노출되면 안 되는 필드가 포함될 수 있음
2. **결합도**: API 응답 포맷이 DB 스키마에 종속됨. DB 컬럼 하나 바꾸면 API 스펙도 바뀜
3. **조작 위험**: 클라이언트가 `status`, `createdAt` 같은 서버 전용 필드를 직접 채워서 보낼 수 있음

---

## 5. 클라이언트 신뢰 문제 (Client Trust Problem)

클라이언트가 보내는 값은 **언제든 위조 가능**합니다.

```
정상 요청:
  { itemCount: 10, pricePerItem: 1000 } → 서버가 9000원으로 계산

악의적 조작:
  { itemCount: 10, pricePerItem: 1000, finalPrice: 1 } → "나 1원에 샀어!" 💥
```

→ 금액, 상태, 등급처럼 **비즈니스에 영향을 주는 값은 반드시 서버에서 계산**해야 합니다.

---

## 6. 올바른 계층별 변환 흐름

```
[클라이언트]
    │  Request DTO (userId, roomId, startHour, endHour 만)
    ▼
[Controller]  → Request DTO를 Service로 전달
    ▼
[Service]     → DTO를 Entity로 변환
    │           reservation.status   = "CONFIRMED"          // 서버가 결정
    │           reservation.createdAt = LocalDateTime.now() // 서버가 채움
    │           reservation.id        = generateId()        // 서버가 생성
    ▼
[Repository]  → Entity를 DB에 저장
    ▼
[Service]     → 결과를 Response DTO로 변환
    ▼
[Controller]  → Response DTO를 클라이언트에 반환
    ▼
[클라이언트]  Response DTO (id, status, createdAt 포함)
```

---

## 7. 핵심 요약

> Request DTO에는 **클라이언트가 의도를 전달하는 값**만 담는다.
> 서버가 계산하거나 생성해야 하는 값(`id`, `status`, `createdAt`)은 절대 Request DTO에 넣지 않는다.
> Entity는 DB 저장용이므로 직접 외부에 노출하지 않는다.

---

## 8. Request DTO 검증 경계

Request DTO는 외부 입력 계약이므로 "형식 검증" 기준을 드러내야 합니다.

예를 들어 쿠폰 발급 요청에서는 아래처럼 나눠서 봅니다.

```java
public record StockCouponIssueRequest(
    /* @NotNull */ Long userId,
    /* @NotNull */ Long couponId,
    /* @Positive */ int requestedQuantity,
    /* @NotBlank */ String idempotencyKey
) {
}
```

Controller에서는 이 DTO에 `@Valid @RequestBody`를 붙여 요청 형식 검증을 트리거합니다.

```java
public ResponseEntity<StockCouponResponse> issue(
    /* @Valid @RequestBody */ StockCouponIssueRequest request
) {
    StockCouponResponse response = stockCouponService.issue(request);
    return ResponseEntity.ok(response);
}
```

### 형식 검증과 도메인 검증 구분

| 검증 대상 | 예시 | 위치 |
|----------|------|------|
| 형식 검증 | null, blank, 양수 여부, 길이, 포맷 | Request DTO + Controller `@Valid` |
| 도메인 검증 | 쿠폰 존재 여부, 재고 여부, 중복 발급, 발급 기간 | Service/Domain |
| 정합성 검증 | 동시 요청 초과 발급, 낙관적 락/비관적 락 충돌 | Transaction/RDBMS |

암기 문장:

> `@Valid`는 "요청값 모양이 올바른가"를 보는 경계이고,  
> Service/Domain은 "현재 저장된 상태와 정책상 처리 가능한가"를 보는 경계다.

---

## 9. Entity, Request DTO, Response DTO 어노테이션 구분

| 대상 | 주로 붙는 어노테이션 | 역할 |
|------|----------------------|------|
| Entity | `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Version` | DB 저장 모델 |
| Request DTO | `@NotNull`, `@NotBlank`, `@Positive` | 입력 형식 검증 |
| Controller 파라미터 | `@Valid`, `@RequestBody` | DTO 검증 실행 |
| Response DTO | 보통 검증 어노테이션 없음 | 응답 계약 |

### 007번 StockCoupon 기준

`StockCoupon`은 Entity 역할을 하는 객체입니다.
실제 JPA 프로젝트라면 아래처럼 DB 저장 모델로 선언합니다.

```java
@Entity
@Table(name = "stock_coupon")
public class StockCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int remainingQuantity;
    private int issuedCount;

    @Version
    private int version;

    private boolean active;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    protected StockCoupon() {
    }
}
```

Request DTO는 클라이언트가 보내는 입력값의 모양을 표현합니다.
Java 21에서는 `record`로 만들기 좋습니다.

```java
public record StockCouponIssueRequest(
    @NotNull Long userId,
    @NotNull Long couponId,
    @Positive int requestedQuantity,
    @NotBlank String idempotencyKey
) {
}
```

Controller에서는 `@Valid @RequestBody`로 Request DTO 검증을 실행합니다.

```java
@PostMapping("/issue")
public ResponseEntity<StockCouponResponse> issue(
    @Valid @RequestBody StockCouponIssueRequest request
) {
    StockCouponResponse response = stockCouponService.issue(request);
    return ResponseEntity.ok(response);
}
```

Response DTO는 서버가 처리한 결과를 클라이언트에게 보여주는 응답 계약입니다.
보통 validation 어노테이션을 붙이지 않습니다.

```java
public record StockCouponResponse(
    Long couponId,
    Long userId,
    String status,
    int remainingQuantity
) {
}
```

---

## 10. Java record가 DTO에 잘 맞는 이유

Java `record`는 "값을 담는 용도의 불변 데이터 객체"를 짧게 만들기 위한 문법입니다.

예를 들어 아래 record는:

```java
public record StockCouponIssueRequest(
    Long userId,
    Long couponId,
    int requestedQuantity,
    String idempotencyKey
) {
}
```

컴파일러가 자동으로 아래 성격을 만들어줍니다.

- 모든 필드는 `private final` 성격이다.
- 생성자가 자동으로 생긴다.
- getter 대신 `userId()`, `couponId()` 같은 접근자가 생긴다.
- `equals`, `hashCode`, `toString`이 자동으로 생긴다.
- 생성 후 필드를 바꾸는 setter가 없다.

이 특성이 DTO와 잘 맞습니다.

### Request DTO에 record가 잘 맞는 이유

Request DTO는 "클라이언트가 보낸 요청값"입니다.
요청값은 Controller로 들어온 뒤 임의로 바꾸기보다, 그대로 Service에 전달하고 서버 결정값은 별도로 계산하는 편이 안전합니다.

```java
public record StockCouponIssueRequest(
    @NotNull Long userId,
    @NotNull Long couponId,
    @Positive int requestedQuantity,
    @NotBlank String idempotencyKey
) {
}
```

이렇게 만들면 Controller나 Service에서 아래처럼 요청 객체를 바꾸기 어렵습니다.

```java
// record에는 setter가 없으므로 이런 식의 변경이 불가능하다.
request.requestedQuantity = 100;
```

그래서 Request DTO의 역할인 "입력값 전달"에 집중하기 좋습니다.

### Response DTO에 record가 잘 맞는 이유

Response DTO는 "서버가 처리한 결과를 외부에 보여주는 값"입니다.
응답 객체도 한 번 만들어지면 바뀔 이유가 거의 없습니다.

```java
public record StockCouponResponse(
    Long couponId,
    Long userId,
    String status,
    int remainingQuantity
) {
}
```

응답 필드가 고정되므로 API 계약을 읽기 쉽고, 불필요한 setter를 열어두지 않아도 됩니다.

### Entity에는 record를 보통 쓰지 않는 이유

JPA Entity는 DTO와 다르게 "DB에 저장되고, 상태가 변하는 객체"입니다.

JPA Entity에는 보통 아래 특성이 필요합니다.

- 기본 생성자
- 식별자(`@Id`)
- 변경 감지(dirty checking)
- 지연 로딩 프록시
- 상태 변경 메서드
- 트랜잭션 안에서 값 변경

`record`는 불변 데이터 객체에 가깝기 때문에 이런 JPA Entity 요구사항과 잘 맞지 않습니다.

따라서 면접에서는 이렇게 말하면 안전합니다.

```text
Java record는 불변 값 객체라 Request/Response DTO에 잘 맞습니다.
반면 JPA Entity는 식별자, 기본 생성자, 변경 감지, 프록시, 상태 변경이 필요하므로 일반 클래스로 두는 것이 보통입니다.
```

### 빠른 구분

| 객체 | record 적합성 | 이유 |
|------|---------------|------|
| Request DTO | 적합 | 외부 입력값 전달, 불변성, validation과 잘 맞음 |
| Response DTO | 적합 | 서버 처리 결과 전달, 응답 계약 고정 |
| JPA Entity | 보통 부적합 | DB 저장 모델, 변경 감지, 프록시, 상태 변경 필요 |

---

## 11. 006번 Order Cancel DTO/Entity 예시

### 기존 코드의 문제점 (`OrderController.java`)

```java
// @PostMapping("/cancel")
public Order cancelOrder(/* @RequestBody */ OrderCancelRequest request) {
    return orderService.cancel(request); // 👈 Entity를 직접 응답으로 반환!
}
```

- **Entity 직접 반환에 따른 결합도 증가**: `Order` 엔티티 구조가 바뀌면(필드 추가/삭제/이름 변경) API 스펙도 연쇄적으로 변경되어 클라이언트(프론트엔드 등)와의 계약이 깨집니다.
- **민감 데이터 노출**: DB 저장 구조에 맞닿아 있는 엔티티의 관리용 상태(`type`, `status`)나 내부 정보가 무분별하게 응답 스펙에 노출될 수 있습니다.

### 개선 방향

성공 여부 및 필요한 필수 정보만을 제한적으로 돌려주도록 전용 응답 DTO `OrderCancelResponse`를 정의하여 반환합니다.

```java
public record OrderCancelResponse(
    Long orderId,
    Long userId,
    String status,
    String cancelledAt
) {
    public static OrderCancelResponse from(Order order) {
        return new OrderCancelResponse(
            order.id,
            order.userId,
            order.status,
            order.cancelledAt != null ? order.cancelledAt.toString() : null
        );
    }
}
```

# 입력값 검증(Validation)과 유효성 경계 설계

Spring Boot 애플리케이션에서 클라이언트로부터 유입되는 데이터를 검증하는 올바른 위치와 방식, 그리고 경계 설계 원칙을 다룹니다.

---

## 1. DTO Bean Validation과 `@Valid` 동작 원리

클라이언트가 전송한 요청 데이터(JSON 등)를 Java 객체(Request DTO)로 매핑하고 검증할 때, **Bean Validation API**를 사용합니다.

### 💡 올바른 구현 구조

1. **Request DTO**: 각 필드에 검증 제약 조건 어노테이션 선언
   ```java
   public record StockCouponRequest(
       @NotNull(message = "사용자 ID는 필수입니다.")
       Long userId,
       
       @NotNull(message = "쿠폰 ID는 필수입니다.")
       Long couponId,
       
       @Min(value = 1, message = "발급 수량은 최소 1개 이상이어야 합니다.")
       int requestedQuantity
   ) {}
   ```

2. **Controller**: `@RequestBody`와 `@Valid`를 함께 작성
   ```java
   @PostMapping("/issue")
   public ResponseEntity<StockCouponResponse> issue(
       @Valid @RequestBody StockCouponRequest request // 👈 @Valid가 유효성 검증을 트리거함
   ) {
       StockCouponResponse response = stockCouponService.issue(request);
       return ResponseEntity.ok(response);
   }
   ```

### ⚙️ 동작 과정
* **`@RequestBody`**: HTTP Request Body의 JSON 문자열을 Jackson 라이브러리를 통해 Java 객체(DTO)로 변환(역직렬화)합니다.
* **`@Valid`**: Java Bean Validator를 트리거하여 DTO 필드에 선언된 어노테이션(`@NotNull`, `@Min` 등)을 검사합니다.
* **예외 발생**: 검증이 실패하면 컨트롤러 메서드가 호출되기도 전에 **`MethodArgumentNotValidException`**이 발생하며, 전역 예외 처리기가 이를 가로채 `400 Bad Request`로 처리합니다.

---

## 2. 코드 작성을 누락했을 때의 사이드 이펙트 (안티패턴)

### ❌ 안티패턴 (007번 예시)
DTO와 컨트롤러에 어떠한 Validation 지시자도 넣지 않은 상태입니다.

```java
// StockCouponRequest.java (어노테이션 없음)
public class StockCouponRequest {
    public Long userId;
    public Long couponId;
    public int requestedQuantity;
}

// StockCouponController.java (@Valid 누락)
public StockCouponResponse issue(StockCouponRequest request) {
    return stockCouponService.issue(request);
}
```

### 💥 발생하는 문제점
1. **형식 검증의 Service 침투**:
   * 입력값이 null이거나 형식이 잘못된 요청이 서비스 계층까지 침투합니다.
   * 서비스 계층은 비즈니스 로직에 집중해야 하지만, 입력값의 단순 null 여부 등을 확인하는 방어 코드(`if (request.userId == null)`)를 직접 작성해야 합니다.
2. **에러 코드의 비표준화**:
   * 단순 형식 오류인데도 서비스 계층에서 `RuntimeException("invalid request")`을 던지게 되며, 이는 실제 오류 원인 파악 및 HTTP 상태코드 매핑을 어렵게 만듭니다.

---

## 3. 유효성 검증의 경계: 형식 검증 vs 비즈니스 검증

모든 검증을 DTO에서 할 수도 없고, 모든 검증을 서비스에서 할 수도 없습니다. **어떤 검증을 어디서 해야 하는가**에 대한 명확한 책임 경계가 필요합니다.

| 구분 | 형식 검증 (Format Validation) | 비즈니스 검증 (Business/Domain Validation) |
| :--- | :--- | :--- |
| **정의** | 데이터 자체의 형식적 유효성 (단독으로 검증 가능) | 시스템의 상태, DB 정보, 비즈니스 규칙에 따른 유효성 |
| **예시** | `null` 여부, 문자열 길이, 이메일 형식, 최소/최대 수량 | 쿠폰 재고 부족 여부, 이미 발급받은 쿠폰 여부, 만료일 초과 여부 |
| **위치** | **Controller / DTO** (Bean Validation) | **Service / Domain Entity** |
| **검증 도구** | `@NotNull`, `@Min`, `@NotBlank` 등 | DB 조회 쿼리, 엔티티 내부 도메인 로직 |

---

## 4. 면접 답변 템플릿

### Q. Controller 단의 DTO 검증(@Valid)과 Service 단의 비즈니스 검증의 차이는 무엇인가요?

> "유효성 검증은 **형식 검증**과 **비즈니스 검증**으로 책임을 분리해야 합니다.
> 
> 첫째, **형식 검증**은 데이터 자체의 완성도를 검증하는 것으로, DTO 필드에 `@NotNull`, `@Min` 등을 선언하고 컨트롤러 매개변수에 `@Valid`를 붙여 처리합니다. 이를 통해 비정상적인 데이터가 비즈니스 계층으로 유입되는 것을 컨트롤러 진입점에서 차단합니다.
> 
> 둘째, **비즈니스 검증**은 DB 상태나 비즈니스 규칙을 확인해야 하는 것으로, 재고 유무나 권한 체크 등은 서비스 계층이나 도메인 엔티티 내부에서 검증합니다.
> 
> 이렇게 경계를 분리하면 서비스 계층은 단순 null 체크 같은 방어 코드 없이 비즈니스 로직에만 온전히 집중할 수 있어 가독성과 유지보수성이 높아집니다."

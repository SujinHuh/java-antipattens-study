# 007. Stock Coupon Legacy Review - 내 답변

## 내 답변

각 항목에는 가능하면 `문제점`, `왜 문제인지`, `실무 영향`, `개선 방향`, `면접 1분 답변`을 같이 적어보세요.

### 1. Controller

#### 잘된 점

- `StockCouponController`가 `StockCouponService`를 직접 `new` 하지 않고 생성자 주입으로 받는다.
- Controller가 재고 차감, 중복 발급 판단, 이벤트 발행 같은 핵심 비즈니스 로직을 직접 수행하지 않는다.
- 응답으로 Entity인 `StockCoupon`을 직접 반환하지 않고 `StockCouponResponse`를 반환한다.

#### 문제 1. null 요청을 실패 DTO로 직접 만들어 반환한다

- 기존 코드:

```java
// @PostMapping("/issue")
public StockCouponResponse issue(/* @RequestBody */ StockCouponRequest request) {
    if (request == null) {
        return new StockCouponResponse(null, null, "FAILED", 0);
    }

    return stockCouponService.issue(request);
}
```

- 판단: Controller가 입력 오류를 HTTP 실패로 드러내지 않고, 성공 응답처럼 보이는 실패 DTO를 직접 만든다.
- 왜 문제인지: `request == null`은 요청 형식 또는 입력값 오류에 가깝다. 그런데 Controller가 `FAILED` 문자열을 가진 응답 DTO를 직접 만들면 실제 HTTP 상태는 성공처럼 내려갈 수 있고, 실패 응답 계약도 공통화되지 않는다.
- 실무 영향: FE/QA는 HTTP 상태코드만 보고 성공으로 처리할 수 있고, 운영 모니터링에서도 실패율이 잡히지 않을 수 있다. 실패 응답 형식도 Controller마다 달라질 위험이 있다.
- 개선 방향: null 요청과 필수값 누락은 DTO validation 또는 공통 예외 처리로 넘긴다. 실패 응답은 `ErrorResponse`와 HTTP 상태코드로 일관되게 내려준다.

- 리팩토링 코드:

```java
// @PostMapping("/issue")
public ResponseEntity<StockCouponResponse> issue(
    /* @Valid @RequestBody */ StockCouponRequest request
) {
    StockCouponResponse response = stockCouponService.issue(request);
    return ResponseEntity.ok(response);
}

// @RestControllerAdvice
class ApiExceptionHandler {
    // @ExceptionHandler(InvalidStockCouponRequestException.class)
    ErrorResponse handleInvalidRequest(InvalidStockCouponRequestException e) {
        return new ErrorResponse("INVALID_REQUEST", "invalid stock coupon request");
    }
}
```

- 면접용 문장:

```text
Controller가 null 요청을 FAILED 응답 DTO로 직접 만들어 반환하면 실제 HTTP 상태는 성공처럼 보일 수 있습니다.
입력 오류는 @Valid나 공통 예외 처리로 분리하고, 실패는 ErrorResponse와 400 같은 HTTP 상태코드로 일관되게 매핑하는 편이 좋습니다.
```

#### 문제 2. 입력 검증 경계가 보이지 않는다

- 기존 코드:

```java
public StockCouponResponse issue(/* @RequestBody */ StockCouponRequest request) {
    if (request == null) {
        return new StockCouponResponse(null, null, "FAILED", 0);
    }

    return stockCouponService.issue(request);
}
```

- 판단: Controller 메서드에서 `@Valid`가 보이지 않고, Request DTO에도 필수값/범위 검증 기준이 드러나지 않는다.
- 왜 문제인지: 여기서 말한 "입력 검증 경계"는 클라이언트가 보낸 값이 Service로 들어가기 전에 최소 형식 검증을 통과하는 지점을 뜻한다. 예를 들어 `couponId`, `userId`, `idempotencyKey`는 필수값이고, `requestedQuantity`는 1 이상이어야 한다. 이런 형식 검증은 Service 내부 비즈니스 로직보다 Controller 요청 경계에서 먼저 걸러지는 편이 좋다.
- 실무 영향: 검증 기준이 없으면 `couponId == null`, `requestedQuantity = -100`, 빈 idempotency key 같은 값이 Service까지 들어가고, Service가 곳곳에서 방어 코드를 반복하게 된다. 실패 원인도 API 계약에 명확히 드러나지 않는다.
- 개선 방향: Request DTO에 `@NotNull`, `@Positive`, `@NotBlank` 같은 Bean Validation을 둔다. Controller에서는 `@Valid @RequestBody`로 형식 검증을 트리거한다. 단, 재고가 남았는지, 이미 발급받았는지, 발급 기간인지 같은 도메인 검증은 Service/Domain에서 처리한다.

- 리팩토링 코드:

```java
public record StockCouponIssueRequest(
    /* @NotNull */ Long userId,
    /* @NotNull */ Long couponId,
    /* @Positive */ int requestedQuantity,
    /* @NotBlank */ String idempotencyKey
) {
}

// @PostMapping("/issue")
public ResponseEntity<StockCouponResponse> issue(
    /* @Valid @RequestBody */ StockCouponIssueRequest request
) {
    StockCouponResponse response = stockCouponService.issue(request);
    return ResponseEntity.ok(response);
}
```

- 암기 포인트:

```text
@Valid는 형식 검증 경계입니다.
null, blank, size, positive 같은 입력 형식은 Controller 요청 경계에서 막고,
재고 존재 여부, 중복 발급 여부, 발급 기간 같은 도메인 규칙은 Service/Domain에서 판단합니다.
```

#### 문제 3. ResponseEntity는 선택 기준으로 말해야 한다

- 기존 코드:

```java
public StockCouponResponse issue(/* @RequestBody */ StockCouponRequest request) {
    return stockCouponService.issue(request);
}
```

- 판단: `StockCouponResponse` 직접 반환 자체가 항상 틀린 것은 아니다. 하지만 이 API는 실패 상태와 상태코드 구분이 중요하므로 HTTP 응답 표현을 더 명확히 할 필요가 있다.
- 왜 문제인지: 단순 조회처럼 항상 200 JSON이면 DTO 직접 반환도 충분할 수 있다. 반면 쿠폰 발급은 잘못된 요청, 쿠폰 없음, 이미 발급됨, 품절, 발급 기간 아님, 동시성 충돌 같은 실패가 많다. 이 실패들을 모두 응답 바디의 문자열 status로만 표현하면 API 계약이 약해진다.
- 실무 영향: FE/QA가 어떤 실패를 재시도해야 하는지, 사용자에게 어떤 메시지를 보여줘야 하는지, 운영에서 어떤 실패율을 봐야 하는지 판단하기 어려워진다.
- 개선 방향: 성공은 `200 OK` 또는 정책에 따라 `201 Created`로 두고, 입력 오류는 400, 쿠폰 없음은 404, 이미 발급/품절/충돌은 409처럼 매핑 기준을 세운다. `ResponseEntity`를 쓰거나 `@RestControllerAdvice`에서 예외를 상태코드로 변환한다.

- 면접용 문장:

```text
ResponseEntity가 항상 정답은 아니지만, 이 문제처럼 실패 종류가 많고 상태코드 구분이 중요한 API에서는 응답 DTO만 직접 반환하는 것보다 HTTP 상태와 ErrorResponse 계약을 명확히 하는 편이 안전합니다.
```

#### Controller 빠른 정리

##### 면접에서 말할 기준

```text
Controller는 Service를 생성자 주입으로 받아 비즈니스 로직을 직접 처리하지 않는 점은 좋습니다.
다만 null 요청이나 검증 실패를 수동으로 FAILED 응답 DTO로 만들어 반환하면 실제 HTTP 상태는 성공처럼 보일 수 있습니다.
따라서 요청 형식 검증은 @Valid와 공통 예외 처리로 넘기고, 성공/실패 상태코드는 ResponseEntity나 ControllerAdvice에서 일관되게 매핑하는 편이 좋습니다.
단순 200 응답이면 DTO 직접 반환도 가능하지만, 이 문제처럼 실패 상태 구분이 중요한 API에서는 HTTP 상태코드와 ErrorResponse 계약을 명확히 하는 것이 중요합니다.
```

##### 외워둘 문장

- `ResponseEntity`는 무조건 정답이 아니라 HTTP 상태, 헤더, Location, 빈 본문, 실패 응답을 명확히 제어해야 할 때 유용하다.
- Controller가 얇아 보여도 실패를 200 응답 바디로 숨기면 API 계약이 약하다.
- Request DTO는 외부 입력 계약이고, Response DTO는 서버 처리 결과 계약이다.
- null 요청, 필수값 누락, 수량 범위 오류는 Controller 내부 수동 응답보다 validation과 공통 예외 처리로 일관되게 다루는 편이 좋다.

### 2. Service

### 3. Repository

### 4. Event / External

### 5. DTO / Entity

### 6. Exception

### 7. Test

## 1분 답변

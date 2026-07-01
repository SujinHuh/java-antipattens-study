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
- 왜 문제인지:
  `request == null`은 요청 형식 또는 입력값 오류에 가깝습니다.
  그런데 Controller가 `FAILED` 문자열을 가진 응답 DTO를 직접 만들면
  실제 HTTP 상태는 성공처럼 내려갈 수 있고, 실패 응답 계약도 공통화되지 않습니다.
- 실무 영향:
  FE/QA는 HTTP 상태코드만 보고 성공으로 처리할 수 있고,
  운영 모니터링에서도 실패율이 잡히지 않을 수 있습니다.
  실패 응답 형식도 Controller마다 달라질 위험이 있습니다.
- 개선 방향:
  null 요청과 필수값 누락은 DTO validation 또는 공통 예외 처리로 넘깁니다.
  실패 응답은 `ErrorResponse`와 HTTP 상태코드로 일관되게 내려줍니다.

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
Controller가 null 요청을 FAILED 응답 DTO로 직접 만들어 반환하면
실제 HTTP 상태는 성공처럼 보일 수 있습니다.
입력 오류는 @Valid나 공통 예외 처리로 분리하고,
실패는 ErrorResponse와 400 같은 HTTP 상태코드로 일관되게 매핑하는 편이 좋습니다.
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
- 왜 문제인지:
  여기서 말한 "입력 검증 경계"는 클라이언트가 보낸 값이 Service로 들어가기 전에
  최소 형식 검증을 통과하는 지점을 뜻합니다.
  예를 들어 `couponId`, `userId`, `idempotencyKey`는 필수값이고,
  `requestedQuantity`는 1 이상이어야 합니다.
  이런 형식 검증은 Service 내부 비즈니스 로직보다
  Controller 요청 경계에서 먼저 걸러지는 편이 좋습니다.
- 실무 영향:
  검증 기준이 없으면 `couponId == null`, `requestedQuantity = -100`,
  빈 idempotency key 같은 값이 Service까지 들어가고,
  Service가 곳곳에서 방어 코드를 반복하게 됩니다.
  실패 원인도 API 계약에 명확히 드러나지 않습니다.
- 개선 방향:
  Request DTO에 `@NotNull`, `@Positive`, `@NotBlank` 같은 Bean Validation을 둡니다.
  Controller에서는 `@Valid @RequestBody`로 형식 검증을 트리거합니다.
  단, 재고가 남았는지, 이미 발급받았는지, 발급 기간인지 같은
  도메인 검증은 Service/Domain에서 처리합니다.

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
- 왜 문제인지:
  단순 조회처럼 항상 200 JSON이면 DTO 직접 반환도 충분할 수 있습니다.
  반면 쿠폰 발급은 잘못된 요청, 쿠폰 없음, 이미 발급됨, 품절,
  발급 기간 아님, 동시성 충돌 같은 실패가 많습니다.
  이 실패들을 모두 응답 바디의 문자열 status로만 표현하면 API 계약이 약해집니다.
- 실무 영향:
  FE/QA가 어떤 실패를 재시도해야 하는지, 사용자에게 어떤 메시지를 보여줘야 하는지,
  운영에서 어떤 실패율을 봐야 하는지 판단하기 어려워집니다.
- 개선 방향:
  성공은 `200 OK` 또는 정책에 따라 `201 Created`로 두고,
  입력 오류는 400, 쿠폰 없음은 404, 이미 발급/품절/충돌은 409처럼 매핑 기준을 세웁니다.
  `ResponseEntity`를 쓰거나 `@RestControllerAdvice`에서 예외를 상태코드로 변환합니다.

- 면접용 문장:

```text
ResponseEntity가 항상 정답은 아니지만, 이 문제처럼 실패 종류가 많고
상태코드 구분이 중요한 API에서는 응답 DTO만 직접 반환하는 것보다
HTTP 상태와 ErrorResponse 계약을 명확히 하는 편이 안전합니다.
```

#### Controller 빠른 정리

##### 면접에서 말할 기준

```text
Controller는 Service를 생성자 주입으로 받아 비즈니스 로직을 직접 처리하지 않는 점은 좋습니다.
다만 null 요청이나 검증 실패를 수동으로 FAILED 응답 DTO로 만들어 반환하면
실제 HTTP 상태는 성공처럼 보일 수 있습니다.
따라서 요청 형식 검증은 @Valid와 공통 예외 처리로 넘기고,
성공/실패 상태코드는 ResponseEntity나 ControllerAdvice에서 일관되게 매핑하는 편이 좋습니다.
단순 200 응답이면 DTO 직접 반환도 가능하지만,
이 문제처럼 실패 상태 구분이 중요한 API에서는 HTTP 상태코드와 ErrorResponse 계약을 명확히 하는 것이 중요합니다.
```

##### 외워둘 문장

- `ResponseEntity`는 무조건 정답이 아니라 HTTP 상태, 헤더, Location, 빈 본문, 실패 응답을 명확히 제어해야 할 때 유용하다.
- Controller가 얇아 보여도 실패를 200 응답 바디로 숨기면 API 계약이 약하다.
- Request DTO는 외부 입력 계약이고, Response DTO는 서버 처리 결과 계약이다.
- null 요청, 필수값 누락, 수량 범위 오류는 Controller 내부 수동 응답보다 validation과 공통 예외 처리로 일관되게 다루는 편이 좋다.

### 2. Service

#### 수진님의 분석 피드백 & 정리

##### 1. 잘된 점
* **의존성 주입(DI) 준수**:
  `StockCouponRepository`, `CouponIssueHistoryRepository`, `StockCouponEventPublisher` 등의
  외부 컴포넌트를 `new`로 직접 생성하지 않고 생성자 주입을 받아
  모듈 간의 결합도를 낮추고 테스트 대체를 용이하게 설계했습니다.

##### 2. 개선 및 의문 사항 답변
* **Q1. 예외 발생과 DTO 반환이 혼용되어 일관성이 없고, 예외 처리가 무분별한 문자열 위주입니다.**
  * **문제점**:
    `RuntimeException`을 던지는 곳, `StockCouponException`을 던지는 곳,
    그리고 성공/실패와 무관하게 `new StockCouponResponse` 객체를 리턴하여 처리를 뭉개는 곳이 섞여 있어
    서비스 코드 전체의 **예외/응답 설계의 일관성이 심각하게 결여**되어 있습니다.
    또한 예외 메시지가 단순 하드코딩 문자열(예: `"sold out"`, `"coupon not found"`)로 처리되어 있어
    예외를 체계적으로 분류하거나 에러 코드를 내려주기 어렵습니다.
  * **개선 방향**:
    비즈니스 예외 처리를 통일하기 위해 `StockCouponErrorCode` Enum을 정의하고,
    예외 발생 시 `StockCouponException(ErrorCode)`를 던지도록 수정해야 합니다.
    DTO 반환을 통한 오류 은폐를 중단하고 비즈니스 실패는 반드시 예외로 처리해야 합니다.

* **Q2. DTO(Request)가 그대로 서비스 내부 로직과 엔티티 상태 변경에 개입합니다. DTO를 엔티티로 바꿔서 처리해야 하는 것 아닌가요?**
  * **문제점**:
    서비스 계층의 파라미터로 Request DTO가 깊숙이 침투하여 직접 비즈니스 로직에 관여하고 있으며,
    엔티티(`coupon`)의 상태(수량 및 카운트, 버전)를 서비스 단에서 절차지향적으로 직접 할당하여 변경하고 있습니다.
  * **개선 방향**:
    요청은 컨트롤러 경계에서 DTO로 받고, 서비스 계층에서는 이 값을 검증하거나
    필요 시 조회된 도메인 모델(`StockCoupon`)에게 책임을 위임해야 합니다.
    또한 엔티티 내부 필드를 직접 열어 연산하기보다는 엔티티에 비즈니스 상태 전이 메서드
    (예: `coupon.issue(quantity)`)를 정의하여 비즈니스 정합성 검증과 가변 연산을 도메인 안으로 캡슐화해야 합니다.

* **Q3. if 조건 분기가 너무 많고 중첩되어 가독성이 떨어져 보입니다.**
  * **문제점**:
    수진님이 명확하게 짚어주신 것처럼, 현재 `issueInternal` 내부에 `active` 검증,
    발급 시간 검증, 클라이언트 버전 검증, 수량 검증 등 수많은 `if`문이 순차적으로 중첩/반복되고 있습니다.
    각 분기마다 `new StockCouponResponse`를 직접 조립하여 조기 리턴을 하고 있어 로직이 산만합니다.
  * **개선 방향**:
    **가드 클로즈(Guard Clause) 패턴**을 도입하여 실패/검증 조건을 최상단에 배치하고
    즉시 예외(`throw new StockCouponException(...)`)를 던져 탈출(early exit)하도록 리팩토링합니다.
    이렇게 하면 비즈니스 오류 상황이 상단에 깔끔하게 나열되고,
    하단에는 최종 성공 시나리오에 해당하는 비즈니스 연산만 깔끔하게 남길 수 있습니다.

* **Q4. 응답 DTO를 리턴할 때 수동 생성자를 통해 모든 값을 하나하나 인자로 넘기는데, 이 경우 순서 오입력 등의 오류가 생길 수 있지 않을까요?**
  * **문제점**:
    아주 훌륭한 통찰입니다! `new StockCouponResponse(coupon.id, request.userId, "ISSUED", ...)`와 같이
    동일한 타입(`Long`)의 파라미터가 인접해 있는 생성자를 서비스 여러 군데에서 직접 호출하고 있습니다.
    실수로 순서를 바꾸어 넣어도 컴파일 타임에는 에러가 발생하지 않으며,
    런타임에 엉뚱한 사용자와 쿠폰 정보가 매핑되는 치명적인 데이터 장애로 이어집니다.
  * **개선 방향**:
    생성자를 직접 열어 매개변수를 나열하는 방식 대신 **정적 팩토리 메서드**
    (`StockCouponResponse.of(coupon, userId, status)`)나 **Lombok의 `@Builder` 패턴**을 활용하여
    변수의 매핑 오류 가능성을 원천 차단해야 합니다.

##### 3. 추가적으로 분석해야 할 미처 발견하지 못한 3대 핵심 결함

* **추가 문제 1: Spring AOP Self-Invocation (자가 호출) 결함**
  * **기존 코드**: `issue` 메서드 내에서 `return issueInternal(request);` 호출.
  * **왜 문제인가**:
    `issue` 메서드에는 `@Transactional`이 없고 내부의 `issueInternal`에만 선언되어 있습니다.
    Spring의 트랜잭션 프록시 메커니즘 상, 동일 클래스 내에서 프록시 객체를 거치지 않고
    `this.issueInternal(...)`을 직접 호출(Self-Invocation)하면
    `@Transactional` AOP 가로채기가 발생하지 않아 **트랜잭션이 전혀 동작하지 않습니다.**
  * **실무 영향**:
    쿠폰 재고 감소는 성공하고 뒤이은 발급 이력 저장에서 DB 에러가 발생해도,
    트랜잭션이 적용되지 않아 **이전의 재고 감소가 롤백되지 않고 그대로 커밋**됩니다.
    결국 재고는 깎였는데 이력은 없는 심각한 데이터 불일치가 발생합니다.
  * **개선 방향**:
    두 메서드를 다른 클래스로 분리하거나, 컨트롤러 단에서 바로 트랜잭션이 걸려 있는
    서비스 메서드를 호출하게 하거나, `issue` 메서드 자체에 `@Transactional`을 부여해야 합니다.

* **추가 문제 2: 동시성 이슈에 따른 데이터 경쟁 (Race Condition)**
  * **기존 코드**: `coupon.remainingQuantity = coupon.remainingQuantity - quantity;` 연산 후 `save` 호출.
  * **왜 문제인가**:
    동시에 수백 명의 사용자가 같은 쿠폰을 발급받으려고 요청하면,
    여러 스레드가 동시에 DB에서 동일한 `remainingQuantity`(예: 100개)를 조회합니다.
    그 후 각 스레드가 개별적으로 `100 - 1 = 99`로 수량을 변경하고 업데이트하므로,
    실제 발급된 수보다 재고가 훨씬 덜 차감되는 **분실된 업데이트(Lost Update)** 현상이 발생합니다.
  * **실무 영향**:
    한정 수량 100개인 쿠폰이 실제로는 1,000명에게 발급되는 **초과 발급(Overselling) 대형 사고**가 발생합니다.
  * **개선 방향**:
    DB 레벨에서 **비관적 락(Pessimistic Lock, SELECT ... FOR UPDATE)**을 사용해 순차 처리를 강제하거나,
    `@Version`을 이용해 낙관적 락(Optimistic Lock)을 구현하여 충돌 시 재시도하게 하거나,
    Redis 분산 락(Distributed Lock) 등을 도입해야 합니다.

* **추가 문제 3: try-catch를 통한 예외 삼킴(Swallow)과 트랜잭션 롤백 방해**
  * **기존 코드**:
    ```java
    try {
        stockCouponRepository.save(coupon);
        issueHistoryRepository.save(history);
        eventPublisher.publishIssued(history);
    } catch (Exception e) {
        return new StockCouponResponse(..., "PENDING", ...);
    }
    ```
  * **왜 문제인가**:
    트랜잭션 범위 내에서 발생하는 DB 예외를 단순히 `try-catch`로 잡아서 에러를 삼키고
    정상 DTO를 반환하면, Spring 트랜잭션 관리자는 메서드가 정상 종료된 것으로 판단하여
    **롤백하지 않고 커밋을 시도**합니다.
  * **실무 영향**:
    `issueHistoryRepository.save`에서 DB 에러가 났음에도 롤백되지 않고,
    앞선 재고 차감 정보만 그대로 커밋되는 데이터 정합성 파탄이 발생합니다.
  * **개선 방향**:
    비즈니스 예외 상황이 아닌 인프라/DB 실패 예외는 삼키지 않고 그대로 상위로 던져서(rethrow)
    트랜잭션이 완벽히 롤백되도록 해야 합니다.

#### 추가로 더 봐야 할 Service 포인트

##### 문제 4. idempotencyKey가 있는데 실제 재시도 기준으로 쓰이지 않는다

- 기존 코드:

```java
if (issueHistoryRepository.existsByUserIdAndCouponId(request.userId, request.couponId)) {
    return new StockCouponResponse(request.couponId, request.userId, "ALREADY_ISSUED", 0);
}

CouponIssueHistory history = new CouponIssueHistory(
    request.userId,
    coupon.id,
    request.idempotencyKey,
    "ISSUED",
    now
);
```

- 판단: 요청에는 `idempotencyKey`가 있고 Repository에도 `existsByIdempotencyKey`가 있지만, Service 발급 흐름에서는 사용하지 않는다.
- 왜 문제인지:
  같은 요청이 네트워크 타임아웃 등으로 재시도될 때, 서버는 "같은 요청 재시도"인지 "새로운 발급 요청"인지 구분해야 합니다.
  현재 코드는 userId/couponId 중복만 보고 `ALREADY_ISSUED`를 반환하므로, 같은 idempotency key로 재시도했을 때 이전 성공 결과를 안정적으로 재현하지 못합니다.
- 실무 영향:
  클라이언트는 첫 요청이 성공했는지 실패했는지 모른 채 재시도할 수 있습니다.
  이때 서버가 이전 결과를 재사용하지 못하면 중복 발급, 잘못된 실패 응답, 재고 차감 불일치가 생길 수 있습니다.
- 개선 방향:
  발급 요청의 idempotency key를 먼저 확인하고, 이미 처리된 key라면 이전 발급 결과를 그대로 반환합니다.
  DB에서는 `idempotencyKey`에 unique constraint를 두어 동시 요청에서도 중복 처리를 막아야 합니다.

- 면접용 문장:

```text
idempotencyKey가 DTO와 Repository에는 있지만 실제 Service 흐름에서 사용되지 않습니다.
재시도 가능한 발급 API라면 같은 idempotency key에 대해 같은 결과를 반환하고,
DB unique constraint로 중복 처리를 막는 것이 안전합니다.
```

##### 문제 5. requestedQuantity가 재고보다 큰 경우를 막지 않는다

- 기존 코드:

```java
if (coupon.remainingQuantity <= 0) {
    throw new StockCouponException("sold out");
}

int quantity = request.requestedQuantity <= 0 ? 1 : request.requestedQuantity;
coupon.remainingQuantity = coupon.remainingQuantity - quantity;
coupon.issuedCount = coupon.issuedCount + quantity;
```

- 판단: 재고가 1개라도 남아 있으면 요청 수량이 10개여도 차감한다.
- 왜 문제인지:
  `remainingQuantity <= 0`만 확인하면 "남은 재고가 요청 수량보다 충분한가"를 검증하지 못합니다.
  `remainingQuantity = 3`이고 `requestedQuantity = 10`이면 재고가 `-7`이 됩니다.
- 실무 영향:
  한정 수량 쿠폰/재고 발급에서 음수 재고, 초과 발급, 정산 불일치가 생깁니다.
- 개선 방향:
  수량은 Request DTO에서 `@Positive`로 1차 검증하고, Service/Domain에서 `remainingQuantity >= quantity`인지 검증해야 합니다.
  차감 로직은 `coupon.issue(quantity)` 같은 도메인 메서드 안으로 넣어 재고 부족 검증과 상태 변경을 묶는 편이 좋습니다.

- 면접용 문장:

```text
재고가 0보다 큰지만 보면 부족합니다.
요청 수량보다 재고가 충분한지 검증한 뒤 차감해야 하고,
이 검증과 차감은 하나의 도메인 메서드나 DB 조건 업데이트로 원자적으로 처리하는 편이 안전합니다.
```

##### 문제 6. clientVersion을 낙관적 락처럼 보지만 실제 락으로 동작하지 않는다

- 기존 코드:

```java
if (request.clientVersion != null && request.clientVersion < coupon.version) {
    return new StockCouponResponse(coupon.id, request.userId, "STALE_REQUEST", coupon.remainingQuantity);
}

coupon.version = coupon.version + 1;
```

- 판단: `clientVersion`과 `coupon.version`을 비교하지만, DB update 시점의 버전 충돌을 보장하지 않는다.
- 왜 문제인지:
  낙관적 락은 클라이언트가 보낸 버전을 단순 비교하는 것이 아니라, DB update 시점에 현재 버전이 기대 버전과 같은지 확인하고 다르면 업데이트를 실패시켜야 합니다.
  JPA라면 `@Version`을 사용하고, SQL이라면 `where id = ? and version = ?` 조건으로 업데이트해야 합니다.
- 실무 영향:
  동시에 두 요청이 같은 version을 읽고 둘 다 통과하면 둘 다 차감할 수 있습니다.
  코드상 version을 1 올려도 실제 동시성 충돌을 막지 못합니다.
- 개선 방향:
  JPA Entity에 `@Version`을 두거나, Repository에서 version 조건 update를 사용합니다.
  충돌이 발생하면 재조회 후 재시도할지, 사용자에게 재시도 요청을 줄지 정책을 정합니다.

- 면접용 문장:

```text
clientVersion 비교만으로는 낙관적 락이 아닙니다.
낙관적 락은 DB update 시점에 version 조건이 맞는지 확인하고,
충돌 시 예외나 재시도 정책으로 처리해야 합니다.
```

- 추가 설명:

비관적 락과 낙관적 락은 둘 다 "동시에 같은 재고를 수정할 때 초과 발급을 막기 위한 방법"입니다.
차이는 **먼저 막느냐**, **나중에 충돌을 감지하느냐**입니다.

비관적 락은 이름 그대로 "충돌이 날 것 같다"고 보고 먼저 잠급니다.
쿠폰 row를 조회할 때 `select ... for update`나 JPA의 `PESSIMISTIC_WRITE`로 DB row에 락을 겁니다.
그러면 A 요청이 쿠폰 재고를 읽고 수정하는 동안 B 요청은 같은 row를 수정하지 못하고 기다립니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from StockCoupon c where c.id = :id")
Optional<StockCoupon> findByIdForUpdate(Long id);
```

면접에서는 이렇게 보면 됩니다.

```text
선착순 쿠폰처럼 동시에 같은 재고를 많이 건드리고 초과 발급이 절대 안 되면 비관적 락을 고려합니다.
대신 요청이 몰리면 DB 커넥션이 오래 대기하고 처리량이 떨어질 수 있습니다.
```

낙관적 락은 "대부분은 충돌이 안 날 것"이라고 보고 먼저 잠그지 않습니다.
대신 저장할 때 `version`이 내가 처음 읽은 값과 같은지 DB가 확인합니다.
처음 읽을 때 version이 1이었는데, 저장할 때도 DB의 version이 1이면 성공하고 version을 2로 올립니다.
그 사이 다른 요청이 먼저 저장해서 DB version이 2가 되어 있으면 내 업데이트는 실패해야 합니다.

```sql
update stock_coupon
   set remaining_quantity = remaining_quantity - 1,
       version = version + 1
 where id = ?
   and version = ?;
```

여기서 중요한 점은 **비교가 Java if문에서 끝나면 안 되고 DB update 조건에 들어가야 한다는 것**입니다.

현재 코드의 문제는 아래 흐름입니다.

```text
초기 상태: version = 1, remainingQuantity = 1

A 요청이 쿠폰 조회: version 1
B 요청이 쿠폰 조회: version 1

A: clientVersion 1 < coupon.version 1 ? false -> 통과
B: clientVersion 1 < coupon.version 1 ? false -> 통과

A: 재고 차감, version = 2
B: 재고 차감, version = 2
```

둘 다 같은 시점의 version을 보고 통과할 수 있습니다.
`coupon.version = coupon.version + 1`도 그냥 메모리 객체의 값을 올리는 코드일 뿐, DB가 충돌을 감지하도록 강제하지 않습니다.

JPA에서 진짜 낙관적 락으로 만들려면 Entity에 `@Version`이 있어야 합니다.

```java
@Entity
public class StockCoupon {
    @Id
    private Long id;

    @Version
    private Long version;

    private int remainingQuantity;
}
```

그리고 동시에 수정이 발생하면 늦게 커밋하는 쪽은 `ObjectOptimisticLockingFailureException` 같은 예외가 발생해야 합니다.
그 예외를 보고 재시도할지, 사용자에게 "다시 시도해주세요"를 내려줄지 정책을 정합니다.

이 코드에서 `clientVersion`은 완전히 쓸모없는 값은 아닙니다.
클라이언트가 오래된 화면에서 요청했는지 확인하는 참고값으로는 사용할 수 있습니다.
하지만 그것만으로 동시성 제어가 되지는 않습니다.

정리하면:

```text
clientVersion = 클라이언트가 알고 있던 버전이라 오래된 요청인지 참고하는 값
@Version / where version = ? = DB update 시점에 충돌을 실제로 감지하는 장치
```

##### 문제 7. LocalDateTime.now() 직접 호출로 시간 정책 테스트가 어렵다

- 기존 코드:

```java
LocalDateTime now = LocalDateTime.now();
if (now.isBefore(coupon.startsAt) || now.isAfter(coupon.endsAt)) {
    return new StockCouponResponse(coupon.id, request.userId, "NOT_OPEN", coupon.remainingQuantity);
}
```

- 판단: 현재 시간을 Service에서 직접 호출한다.
- 왜 문제인지:
  발급 시작 직전, 종료 직후, 타임존 차이 같은 경계값 테스트를 안정적으로 만들기 어렵습니다.
  테스트 실행 시점에 따라 결과가 달라질 수 있습니다.
- 실무 영향:
  이벤트 오픈/마감 시간 관련 장애는 실제 서비스에서 자주 발생합니다.
  시간이 고정되지 않으면 회귀 테스트가 불안정해집니다.
- 개선 방향:
  `Clock`을 주입받아 `LocalDateTime.now(clock)`으로 계산하거나, 발급 가능 여부를 `StockCouponIssuePolicy` 같은 정책 객체로 분리합니다.

- 면접용 문장:

```text
시간 기준 정책은 LocalDateTime.now()를 직접 호출하면 테스트가 흔들립니다.
Clock을 주입하거나 정책 객체로 분리해 발급 시작/종료 경계값을 고정해서 테스트할 수 있게 하겠습니다.
```

##### 문제 8. 이벤트 발행이 저장 트랜잭션과 한 try-catch에 묶여 있다

- 기존 코드:

```java
try {
    stockCouponRepository.save(coupon);
    issueHistoryRepository.save(history);
    eventPublisher.publishIssued(history);
} catch (Exception e) {
    return new StockCouponResponse(coupon.id, request.userId, "PENDING", coupon.remainingQuantity);
}
```

- 판단: DB 저장과 이벤트 발행이 같은 try-catch에 섞여 있다.
- 왜 문제인지:
  이벤트 발행은 외부 시스템 또는 비동기 처리와 연결될 수 있습니다.
  DB 저장은 성공했는데 이벤트 발행이 실패하거나, 이벤트는 발행됐는데 이후 트랜잭션이 실패하면 외부 세계와 DB 상태가 달라집니다.
- 실무 영향:
  사용자는 쿠폰 발급 알림을 받았는데 DB에는 발급 이력이 없거나,
  DB에는 발급됐지만 알림/후속 처리가 누락되는 문제가 생길 수 있습니다.
- 개선 방향:
  이벤트 발행은 트랜잭션 커밋 이후 처리하거나, outbox 테이블에 이벤트를 저장한 뒤 별도 퍼블리셔가 안정적으로 발행하도록 분리합니다.
  면접에서는 "외부 호출은 DB 트랜잭션으로 롤백되지 않는다"는 기준을 함께 말하면 좋습니다.

- 면접용 문장:

```text
DB 저장과 이벤트 발행을 한 try-catch에 묶으면 외부 이벤트와 DB 상태가 어긋날 수 있습니다.
발급 이력을 먼저 트랜잭션으로 확정하고, 이벤트는 커밋 이후나 outbox 방식으로 분리하는 편이 안전합니다.
```

#### Service 빠른 정리

```text
Service에서는 요청값 검증, 중복 발급, 쿠폰 조회, 발급 가능 검증, 수량 차감, 이력 저장, 이벤트 발행 순서로 흐름을 봐야 합니다.
이 코드의 핵심 위험은 @Transactional self-invocation, read-modify-write 경쟁, idempotencyKey 미사용, requestedQuantity 검증 부족, 예외 삼킴, 이벤트 발행 시점입니다.
단순히 if문이 많다는 수준보다 동시 요청에서 재고와 발급 이력이 어떻게 깨지는지를 설명해야 면접 답변으로 강해집니다.
```

### 3. Repository

### 4. Event / External

### 5. DTO / Entity

### 6. Exception

### 7. Test

## 1분 답변

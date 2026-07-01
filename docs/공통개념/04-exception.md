# 예외(Exception) 처리 설계 원칙

## 1. 예외 삼킴 (Exception Swallowing) 방지

예외를 `catch`로 잡은 뒤 아무런 처리 없이 묻어버리는 행위입니다.

```java
// ❌ 안티패턴 — 예외 삼킴
try {
    doSomething();
} catch (Exception e) {
    return "ERROR"; // e가 완전히 유실됨. 로그에도 안 남음!
}
```

**실제 운영에서 생기는 문제**:
```
클라이언트 → "500 에러가 났어요"
개발자     → "뭐가 문제지?" (로그에 아무것도 없음 😱)
```

**해결책**:
```java
// ✅ 예외 객체 자체를 로거에 넘겨야 스택트레이스가 남음
catch (InvalidOrderException e) {
    log.error("주문 처리 실패: {}", e.getMessage(), e); // e를 마지막 인자로 전달
    return "400 BAD_REQUEST";
}
catch (Exception e) {
    log.error("예상치 못한 오류", e); // 스택트레이스 전체 기록
    return "500 INTERNAL_SERVER_ERROR";
}
```

> `e.toString()`이나 `e.getMessage()`만 남기면 파일명/라인 정보가 누락됩니다.
> 반드시 **예외 객체 자체**를 로거의 마지막 인자로 넘겨야 합니다.

---

## 2. 디버깅이 가능한 예외 메시지 작성

단순 고정 문자열 예외는 원인 추적이 불가능합니다.

```java
// ❌ 나쁜 예 — 컨텍스트가 없음
throw new OrderException("invalid order");

// 로그에 보이는 것:
//   ERROR: invalid order
//   ← 어떤 주문? 왜 invalid? 어떤 값이?  → 알 수 없음
```

```java
// ✅ 좋은 예 — 컨텍스트 포함
throw new OrderException(
    "주문 저장 실패: finalPrice가 0입니다. " +
    "orderId=" + order.id + ", customerType=" + order.customerType
);

// 로그에 보이는 것:
//   ERROR: 주문 저장 실패: finalPrice가 0입니다. orderId=12345, customerType=VIP
//   ← 즉시 원인 파악 가능 ✅
```

**좋은 예외 메시지의 요소**:
- **누가**: 어떤 ID의 데이터인가? (`orderId=12345`)
- **무엇이**: 어떤 필드가 문제인가? (`finalPrice`)
- **왜**: 어떤 값이어서 통과하지 못했는가? (`값이 0`)

---

## 3. 예외 계층화 및 세분화

하나의 예외로 모든 상황을 처리하면 호출부에서 구분이 불가능합니다.

```java
// ❌ 모든 상황을 하나로 처리
throw new OrderException("invalid"); // 가격 문제인지 재고 문제인지 알 수 없음

// ✅ 목적에 맞게 분리
class OrderException extends RuntimeException { }          // 최상위 도메인 예외
class InvalidOrderPriceException extends OrderException { } // 가격 문제
class OrderNotFoundException extends OrderException { }     // 주문 없음
class DuplicateOrderException extends OrderException { }    // 중복 주문
```

**세분화의 장점**:
```java
// 예외 종류별로 다른 HTTP 응답 코드 매핑 가능
catch (InvalidOrderPriceException e) → 400 BAD_REQUEST
catch (OrderNotFoundException e)     → 404 NOT_FOUND
catch (DuplicateOrderException e)    → 409 CONFLICT
```

---

## 4. 핵심 요약

> 예외는 **삼키지 말고**, **컨텍스트를 담아서**, **세분화**해서 던져야 한다.
> 좋은 예외 메시지는 "누가, 무엇을, 왜"를 담아 로그만 봐도 원인을 파악할 수 있어야 한다.

---

## 5. 006번 Order Cancel Exception 예시

### 기존 코드의 문제점 (`OrderService.java`)

```java
if (request == null || request.orderId == null) {
    throw new RuntimeException("invalid cancel request"); // 표준 예외 직접 사용
}

if ("CANCELLED".equals(order.status)) {
    throw new OrderCancelException("already cancelled"); // 커스텀 예외지만 메시지만 전달
}
```

1. **표준 예외와 비즈니스 예외의 혼용**: 입력값 유효성 실패 시 `RuntimeException`을 던지고, 상태값 오류 시 `OrderCancelException`을 던지는 등 일관성이 없습니다.
2. **에러 코드의 누락**: `OrderCancelException`에 에러를 식별할 수 있는 상태 코드나 내부 에러 코드(Enum)가 없고 단순 문자열 메시지만 넘겨받고 있습니다. 이 경우 전역 예외 처리기에서 예외마다 적절한 HTTP 상태코드(400, 404, 409 등)를 매핑해주기 어렵습니다.

### 개선 방향

에러 구분이 용이하도록 `OrderCancelErrorCode` 에러 코드 Enum을 정의하고, 도메인 예외인 `OrderCancelException`이 이를 포함하여 던지도록 수정합니다.

```java
public enum OrderCancelErrorCode {
    INVALID_REQUEST,
    ORDER_NOT_FOUND,
    ALREADY_CANCELLED,
    CANCEL_WINDOW_CLOSED
}

public class OrderCancelException extends RuntimeException {
    private final OrderCancelErrorCode errorCode;

    public OrderCancelException(OrderCancelErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public OrderCancelErrorCode getErrorCode() {
        return errorCode;
    }
}
```

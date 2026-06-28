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

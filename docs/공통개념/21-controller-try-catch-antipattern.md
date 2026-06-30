# Controller try-catch 예외 처리 안티패턴과 @RestControllerAdvice

Controller 내부에서 비즈니스 예외를 직접 try-catch로 잡는 설계의 문제점과, `@RestControllerAdvice`를 활용한 공통 예외 처리 모범 사례를 다룹니다.

---

## 1. Controller 내 try-catch 처리의 문제점 (안티패턴)

많은 초보 개발자가 Controller 단에서 예외가 발생해 서버가 터지는 것을 막기 위해 모든 코드를 try-catch로 감싸는 실수를 범합니다.

```java
// ❌ 전형적인 Controller try-catch 안티패턴
@PostMapping("/cancel")
public Map<String, Object> cancelOrder(@RequestBody OrderCancelRequest request) {
    try {
        orderService.cancel(request);
        return Map.of("status", 200, "message", "success");
    } catch (OrderCancelException e) {
        return Map.of("status", 400, "message", e.getMessage());
    } catch (Exception e) {
        return Map.of("status", 500, "message", "Internal Server Error");
    }
}
```

### ① 중복 코드 발생과 핵심 책임 분산
- 모든 API 메서드마다 try-catch 구조가 복사-붙여넣기되어 코드가 매우 지저분해집니다.
- Controller의 본래 책임인 **"요청을 받고 응답을 매핑하는 역할"**보다 예외 처리 코드가 더 비대해져 SRP(단일 책임 원칙)를 위반합니다.

### ② 실제 HTTP 상태코드 불일치 (200 OK 응답 버그)
- 위 코드처럼 예외를 catch해서 `Map.of("status", 400, ...)`를 그냥 리턴해버리면, WAS(톰캣 등)는 메서드가 정상적으로 리턴값을 반환했다고 인지하여 실제 HTTP 네트워크 헤더에는 **`200 OK`**를 실어 클라이언트에 보냅니다.
- 브라우저나 클라이언트는 API 호출이 성공(200)한 줄 알고 파싱하려다 JSON 바디 안의 `status: 400`을 보고 오작동하는 정합성 문제가 생깁니다.

---

## 2. 올바른 해결책: 예외 전파와 Global Exception Handler

가장 좋은 방법은 **Controller는 예외를 잡지 않고 밖으로 던지게(throw) 두고**, Spring이 제공하는 **`@RestControllerAdvice`를 통해 전역에서 예외를 가로채어 일관되게 처리**하는 구조입니다.

```java
// ✅ Controller는 예외 처리를 하지 않고 깔끔하게 유지
@PostMapping("/cancel")
public ResponseEntity<OrderCancelResponse> cancelOrder(@Valid @RequestBody OrderCancelRequest request) {
    orderService.cancel(request);
    return ResponseEntity.ok(new OrderCancelResponse("success"));
}
```

```java
// ✅ 전역에서 특정 예외들을 가로채 일관된 HTTP 상태코드와 DTO를 반환
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 예외 처리 (400 Bad Request)
    @ExceptionHandler(OrderCancelException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancelException(OrderCancelException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("ORDER_CANCEL_ERROR", e.getMessage()));
    }

    // 그 외 예상치 못한 모든 예외 처리 (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
```

### 전역 예외 처리의 장점
1. **Controller 코드의 극대화된 단순화**: 예외 처리 로직이 없어 비즈니스 흐름만 깔끔하게 보입니다.
2. **정확한 HTTP 상태코드 제공**: `ResponseEntity`를 활용해 네트워크 통신 레벨에서도 400, 500 등 올바른 HTTP 상태코드가 보장됩니다.
3. **일관된 에러 응답 포맷**: FE(프론트엔드)에서 일관된 `ErrorResponse` DTO를 활용해 에러 처리가 매우 쉬워집니다.

---

## 3. 면접 답변 템플릿

### Q. Controller 내에서 try-catch로 예외를 처리하는 방식의 문제점과 해결 방안은 무엇인가요?

> "Controller 내부에서 직접 try-catch로 예외를 잡아서 Map 등으로 반환하면 두 가지 문제가 생깁니다. 
> 
> 첫째, 모든 API 메서드마다 중복된 try-catch 블록이 생겨 유지보수가 어렵고 코드가 지저분해집니다.
> 둘째, 예외를 직접 잡아서 객체로 반환할 경우 실제 HTTP 응답 상태코드는 `200 OK`로 나가게 되어 API 스펙의 일관성이 깨집니다.
> 
> 이를 해결하기 위해 Controller는 예외를 잡지 않고 상위로 던지도록 설계하고, **`@RestControllerAdvice`와 `@ExceptionHandler`를 사용한 전역 예외 처리기(Global Exception Handler)**를 두어 일관된 HTTP 상태코드와 에러 DTO를 내려주는 방식을 취해야 합니다."

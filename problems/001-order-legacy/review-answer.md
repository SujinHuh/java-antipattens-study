# Review Answer

## 내 답변 원문

### Controller

- 여기에 responseBody로 받지 않은거? 그리고 if라는 로직으로 400에 badRequest를 남긴거? 로직은 service단에서 하는게 아닌가 싶어

### Service

-

### Repository

- db에 저장하는 로직이라서 크게 문제는 없는것 같아

### DTO / Entity

-

### Exception

-

### Test

-

## 피드백 요약

- Controller에서 DTO/validation/예외 처리 문제를 의심한 것은 좋은 출발이다.
- Repository는 단순 저장소처럼 보이지만, 비즈니스 판단과 테스트 격리 문제가 숨어 있다.
- Service, DTO/Entity, Exception, Test는 다음 문제에서 다시 집중해서 연습해야 한다.

## 보완 답변

### Controller

- 요청을 DTO로 받지 않고 `String customerType`, `String itemCount`, `String pricePerItem`처럼 개별 파라미터로 받고 있다.
- `Integer.parseInt()`를 Controller에서 직접 수행하고 있어 잘못된 숫자 문자열이 들어오면 예외가 발생한다.
- `count <= 0`, `price <= 0` 같은 입력 검증을 if문으로 직접 처리하고 있다. Spring에서는 요청 DTO에 validation을 두고 Controller에서는 `@Valid`로 검증하는 방식이 더 적절하다.
- 모든 예외를 `catch (Exception e)`로 잡아서 `"500 INTERNAL_SERVER_ERROR"`를 반환한다. 예외 원인을 잃고, 클라이언트가 어떤 문제가 발생했는지 알기 어렵다.
- `OrderService`를 직접 `new`로 생성하고 있어 의존성 주입을 사용하지 않는다.

### Service

- 할인 계산, 대량 구매 할인, 음수 보정, Entity 생성, 저장, 응답 문자열 생성까지 한 메서드가 모두 처리한다.
- `customerType.equals("VIP")`처럼 문자열로 고객 타입을 비교한다. 오타에 취약하고, `customerType`이 null이면 NPE가 발생할 수 있다.
- 할인 정책이 if/else로 하드코딩되어 있어 고객 타입이 늘어날수록 메서드가 계속 커진다.
- `System.currentTimeMillis()`로 id를 만들고 `LocalDateTime.now()`를 직접 호출해 테스트가 어려워진다.
- Service가 응답 문자열까지 직접 만들어서 비즈니스 로직과 표현 형식이 섞여 있다.

### Repository

- 저장소 계층인데 `finalPrice == 0`이면 예외를 던지는 비즈니스 판단을 하고 있다.
- `static List`를 DB처럼 사용하고 있어 테스트 간 데이터가 공유될 수 있고, 동시성에도 취약하다.
- `System.out.println()`으로 로그를 직접 출력한다.
- `findById()`에서 못 찾으면 null을 반환한다. 호출하는 쪽에서 NPE가 나기 쉬우므로 `Optional`이나 명확한 예외 처리가 낫다.

### DTO / Entity

- `OrderRequest`에 `finalPrice`가 들어 있다. 요청자가 최종 가격을 보내는 구조가 되면 서버 계산값과 충돌할 수 있다.
- DTO와 Entity가 모두 public field로 열려 있어 캡슐화가 없다.
- `customerType`이 문자열이라 허용 가능한 값이 코드로 제한되지 않는다. enum으로 바꾸는 편이 안전하다.
- `createdAt`이 `String`이다. 날짜/시간은 `LocalDateTime` 같은 타입으로 들고 있는 편이 낫다.

### Exception

- `OrderException("invalid order")`만으로는 어떤 주문이 왜 invalid인지 알기 어렵다.
- Controller가 모든 예외를 `Exception`으로 잡아서 도메인 예외와 시스템 예외를 구분하지 못한다.
- 실제 Spring 구조라면 `GlobalExceptionHandler`에서 예외별 HTTP status와 응답 body를 통일하는 것이 좋다.

### Test

- JUnit 어노테이션이 없어 실제 테스트로 실행되지 않는다.
- VIP 할인 하나만 검증하고 EMPLOYEE, 대량 구매 할인, 음수 보정, 잘못된 입력, 저장 실패 케이스를 검증하지 않는다.
- 결과 문자열에 `"final price: 18000"`이 포함되는지만 확인해서 비즈니스 값과 응답 형식이 강하게 묶여 있다.
- Repository가 static List를 사용하므로 테스트 간 상태가 섞일 수 있다.

## 다음 문제에서 다시 볼 약점

- Repository가 저장만 하는지, 비즈니스 판단을 섞고 있는지 확인하기
- DTO와 Entity의 역할이 섞여 있는지 확인하기
- Test가 실제로 실행되는 테스트인지, 핵심 규칙을 충분히 검증하는지 확인하기

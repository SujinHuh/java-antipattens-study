# Review Answer

이 파일에 답변을 작성하세요.

## Controller

- dto로 받지않고 각각의 spring UserId, String roomId... 이런걸로 받음
-             ReservationRequest request = new ReservationRequest();
            request.userId = Long.parseLong(userId);
            request.roomId = Long.parseLong(roomId);
            request.startHour = Integer.parseInt(startHour);
            request.endHour = Integer.parseInt(endHour);
            request.status = status; -> 이부부이 객체를 여기서 생성하는 게 맞아? 이건 dto나 entity에서 선언해서 호출해야하는게 아닌가 라는 생각을 하는데, 그리고 Long.parseLong -> 이부분도 parseLong을 여기서해야하나? 의문이 드네... 
- if(requet.starHour-> doeh ) 이것도 뢰직인데, if해서 비교해서 하는게 여기 들어가느게 맞나 ? 라는 생각이 들고 
- excetion이걸 예외처리를 너무 뭉뜽그려놨어 500에러로 말이야. 자세하게 나온게 아닌것같아서 

## Service

- 여기서 Reservation new 를 생서해줘야해 ? Reservation @setter, getter사용해서 할당해주는거 있지않아? 
- @Transactional 이있는데... 이거 여기에 있어야하는건지... 잘 모르겠다...? 트랜젝션은 컨트롤러에 잇어야하는건지 서비스에 있어야하ㅡㄴ건지마리야.. 
- cancel 에서  CANCELLED -> 글자 그대로 넣으면 오타가 생겨서 오류가 날 수 도 잇으니까.. 이넘을 사용해서 넣어야하는거아니야? 

## Repository

- 여기도 static List를 사용해서 데이터 충돌이 날 수 도있어 /데이터 정학성이 안맞을 수 도있고말이야.
- 로그를 찍을때 println을 사용하지말고 log.를 사용하느 방법이 있고
- Reservation -> 에서 return null;을 넣으면 NPE가 생길 수 잇는 우려가 있어 
- existsOverlapping 에서 (            if (reservation.roomId == roomId
  && !"CANCELLED".equals(reservation.status)
  && reservation.startHour < endHour
  && startHour < reservation.endHour) {) -> 이런 조건들이 다른 방법이 없을까?"CANCELLED" 이건 스펠링이 틀리면 ... 완전 안되는건데 이넘이라던지말이야. 

## DTO / Entity

- Reservation -> 각각의 필드들이 public으로 되어잇어서 캡슐화가 안되어있고 private로 수정을해야할 것 같아. 
- 나 여기서 궁금한점이 DTO/Entity의 개념이 조금 헷갈리고 여기서 db와 직접 주고 받는걸 dto가 하는건가? entity가 하는건가 사용자한테 요청을 받은아서 들어오는게 entity가 받는거 맞지? 

## Exception

- 여기부분은 아직 ...잘 이해가 안가고 모르겠어 

## Test

- CONFIRMED -> 여기에 대한것만 테스트가 아니라 다른 것도 테스트를 해야하는거 아니야? 
- Junit없이 테스트를 하틑것 같은데? 

## Transaction / AOP

- 그리고 @transcation에 대해서 횡단관심사이잖아? 그럼 공통되기 한 부분만 해서 @Transcation을 한번만 걸어줘도 좋을 것 같단말이지?

## 채점

점수: 72점

### 잘한 점

- Controller에서 DTO로 받지 않고 문자열 파라미터를 직접 파싱하는 문제를 잡았다.
- Controller의 수동 검증과 광범위 예외 처리를 문제로 봤다.
- 문자열 상태값인 `CANCELLED`, `CONFIRMED`가 오타에 취약하다는 점을 잘 잡았다.
- Repository의 `static List`, `println`, `return null` 문제를 001보다 훨씬 잘 찾았다.
- 테스트가 한 케이스만 보고, JUnit 구조가 아니라는 점을 잡았다.

### 보완할 점

- DTO와 Entity 역할이 반대로 이해된 부분이 있다. 사용자의 요청을 받는 것은 DTO이고, DB와 매핑되는 것은 Entity다.
- Service에서 Entity를 생성하는 것 자체가 항상 문제는 아니다. 문제는 생성, 검증, 저장, 응답 문자열 생성이 한 메서드에 과하게 몰려 있다는 점이다.
- `@Transactional`은 보통 Controller가 아니라 Service의 유스케이스 경계에 둔다.
- self-invocation 문제를 놓쳤다. 같은 클래스 내부에서 `reserve()`가 `saveReservation()`을 직접 호출하면 Spring AOP 프록시를 거치지 않아 `saveReservation()`의 `@Transactional`은 따로 적용되지 않는다.
- Repository의 `save()`가 `CANCELLED` 상태와 시간 유효성을 판단하는 것은 저장소 책임이 아니라 도메인/Service 책임이다.

## 보완 답변

### Controller

- 요청을 DTO로 받지 않고 `String userId`, `String roomId`, `String startHour`처럼 개별 문자열 파라미터로 받고 있다.
- `Long.parseLong()`, `Integer.parseInt()`를 Controller에서 직접 수행해 잘못된 입력이 들어오면 예외가 발생한다.
- `ReservationRequest`를 Controller에서 직접 만들고 public field에 값을 채우고 있다. Spring MVC라면 요청 DTO를 `@RequestBody`로 받고 `@Valid`로 검증하는 구조가 더 자연스럽다.
- 시간 범위 검증을 if문으로 직접 처리하고 문자열 `"400 BAD_REQUEST"`를 반환한다. 검증 정책과 응답 형식이 Controller에 흩어질 수 있다.
- 모든 예외를 `catch (Exception e)`로 잡아 `"500 INTERNAL_SERVER_ERROR"`만 반환한다. 잘못된 요청, 예약 중복, 시스템 오류를 구분할 수 없다.
- `ReservationService`를 직접 `new`로 생성해 의존성 주입을 사용하지 않는다.

### Service

- 예약 가능 여부 확인, 상태 검증, Entity 생성, 저장, 응답 문자열 생성까지 한 메서드가 처리한다.
- `"BLOCKED"`, `"CANCELLED"` 같은 문자열 상태값을 직접 비교한다. 오타에 취약하므로 `ReservationStatus` enum을 사용하는 편이 안전하다.
- `System.currentTimeMillis()`로 id를 만들고 `LocalDateTime.now()`를 직접 호출해 테스트가 어렵다.
- `reserve()`가 응답 문자열을 직접 만든다. Service는 결과 객체를 반환하고 Controller가 응답 형식으로 변환하는 편이 책임이 분리된다.
- `cancel()`에서 404, 409, 200 같은 HTTP 의미를 문자열로 반환한다. Service가 HTTP 응답 표현에 묶인다.

### Repository

- `static List`를 저장소처럼 사용해 테스트 간 데이터가 공유되고 동시성 문제가 생길 수 있다.
- `save()`에서 `"CANCELLED"` 상태와 시간 조건을 보고 예외를 던진다. Repository가 비즈니스 정책을 판단하고 있다.
- `System.out.println()`으로 로그를 직접 출력한다.
- `findById()`가 못 찾으면 `null`을 반환한다. 호출부에서 NPE가 나기 쉬우므로 `Optional<Reservation>` 또는 명확한 예외 처리가 낫다.
- `existsOverlapping()` 안에도 상태 문자열 비교와 예약 겹침 정책이 들어 있다. 조회 조건 자체는 Repository에 있을 수 있지만, 정책 의미가 커지면 도메인 메서드나 명확한 쿼리 메서드로 분리하는 것이 좋다.

### DTO / Entity

- 요청 DTO는 외부 요청을 받는 객체이고, Entity는 DB에 저장되는 도메인/영속 객체다. 사용자가 보내는 값을 Entity가 직접 받는 구조는 피하는 편이 좋다.
- `ReservationRequest`에 `status`, `createdAt`이 들어 있다. 상태와 생성 시간은 보통 서버가 결정해야 하므로 클라이언트 요청값으로 받으면 조작 가능성이 생긴다.
- DTO와 Entity가 모두 public field로 열려 있어 캡슐화가 없다.
- DTO와 Entity가 거의 같은 필드 구조라 역할이 흐려져 있다. 요청 DTO, 응답 DTO, Entity를 목적에 맞게 분리하는 것이 좋다.

### Exception

- `ReservationException("invalid")`, `"already reserved"`처럼 메시지가 모호하고 구조화된 에러 코드가 없다.
- Controller가 모든 예외를 `Exception`으로 잡아 예약 중복, 잘못된 입력, 시스템 오류를 구분하지 못한다.
- 실제 Spring 구조라면 `GlobalExceptionHandler`에서 예외별 HTTP status와 응답 body를 일관되게 내려주는 것이 좋다.

### Test

- JUnit의 `@Test`가 없어 실제 테스트로 실행되지 않는다.
- 성공 케이스 하나만 검증한다.
- 중복 예약, 잘못된 시간, 취소 성공, 이미 취소된 예약, 존재하지 않는 예약, 상태값 오류를 검증하지 않는다.
- Repository가 static List를 사용하므로 테스트 간 상태가 섞일 수 있다.
- 결과 문자열 포함 여부만 검증해 비즈니스 결과와 응답 포맷이 강하게 묶여 있다.

### Transaction / AOP

- `@Transactional`은 보통 Controller가 아니라 Service의 유스케이스 경계에 둔다.
- `reserve()`에서 같은 클래스의 `saveReservation()`을 직접 호출한다. Spring AOP는 프록시를 통해 외부에서 호출될 때 적용되므로, self-invocation에서는 `saveReservation()`의 `@Transactional`이 별도로 적용되지 않는다.
- 이 코드에서는 `reserve()` 자체에 `@Transactional`이 있으므로 전체 예약 흐름의 트랜잭션 경계는 `reserve()`에 두는 것이 더 자연스럽다.
- `saveReservation()`처럼 단순 저장만 감싼 내부 메서드에 별도 `@Transactional`을 붙이는 것은 의미가 약하고 오해를 만들 수 있다.

## DTO / Entity 정리

- DTO: Controller 경계에서 요청/응답 데이터를 운반하는 객체다. 예: `ReservationRequest`, `ReservationResponse`.
- Entity: DB에 저장되는 도메인 상태를 표현하는 객체다. 예: `Reservation`.
- 사용자의 요청은 DTO로 받고, Service에서 검증한 뒤 Entity를 생성하거나 변경한다.
- Entity를 API 요청에 직접 노출하면 DB 구조와 API 구조가 강하게 묶이고, 클라이언트가 수정하면 안 되는 필드까지 받을 위험이 생긴다.

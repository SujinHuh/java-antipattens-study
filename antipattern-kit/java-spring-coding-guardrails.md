# Java/Spring Coding Guardrails

이 문서는 Java/Spring Boot 코드 생성 시 피해야 할 안티패턴과 지켜야 할 코딩 지침을 누적한다.

## 작성 형식

```text
[안티패턴 이름]
[나쁜 코드 예시]
[왜 문제인가]
[실무에서 생길 수 있는 문제]
[면접에서 설명할 문장]
[좋은 코드 방향]
[AI 에이전트에게 줄 코딩 지침]
[관련 키워드]
```

## 누적 대상

- Controller에 비즈니스 로직 작성
- DTO validation 누락
- `ResponseEntity`가 필요한 상황에서 객체만 반환
- Service 책임 과다
- Repository 직접 노출
- `@Transactional` self-invocation
- Transaction boundary 과다/부족
- Entity에 모든 로직 몰아넣기
- 예외 정책 없음
- null 처리 불명확
- `Optional` 오남용
- `StringBuilder` / `StringBuffer` 용어 혼동
- 동시성 고려 누락
- 테스트 없는 리팩토링
- SOLID 위반
- Transaction Script 남용

## 규칙 초안

### Controller에 비즈니스 로직을 작성하지 않는다

[안티패턴 이름]
Controller 비즈니스 로직 집중

[나쁜 코드 예시]
Controller에서 상태 변경, 외부 API 호출, 결제/예약 정책 판단을 직접 수행한다.

[왜 문제인가]
요청 처리 계층과 비즈니스 정책 계층의 책임이 섞여 테스트와 변경이 어려워진다.

[실무에서 생길 수 있는 문제]
검증, 트랜잭션, 예외 처리, 로깅 정책이 컨트롤러마다 흩어져 장애 추적과 유지보수가 어려워진다.

[면접에서 설명할 문장]
Controller는 HTTP 요청/응답과 입력 검증에 집중하고, 유스케이스와 도메인 정책은 Service 계층으로 분리하는 편이 좋다고 봅니다.

[좋은 코드 방향]
Controller는 DTO 검증과 Service 호출만 담당하고, 비즈니스 분기와 상태 변경은 Service에서 처리한다.

[AI 에이전트에게 줄 코딩 지침]
Spring MVC Controller에는 핵심 비즈니스 정책, DB 상태 변경 조합, 외부 API 호출 순서 제어를 직접 작성하지 말고 Service 계층으로 위임하라.

[관련 키워드]
Controller, Service, responsibility separation, testability, Spring MVC

### Repository에 비즈니스 정책 판단을 넣지 않는다

[안티패턴 이름]
Repository 비즈니스 판단 혼입

[나쁜 코드 예시]
Repository의 `save()` 메서드에서 `finalPrice == 0` 같은 주문 정책을 판단하고 도메인 예외를 던진다.

[왜 문제인가]
저장소 접근 계층과 도메인 정책 계층의 책임이 섞여 변경 위치가 불명확해진다.

[실무에서 생길 수 있는 문제]
같은 정책이 Service, Repository, Entity에 흩어지고 테스트가 어려워진다. 저장 방식 변경 시 비즈니스 규칙까지 함께 건드리게 된다.

[면접에서 설명할 문장]
Repository는 데이터 접근을 담당하고, 주문 유효성이나 가격 정책 같은 비즈니스 판단은 Service나 도메인 객체 쪽에 두는 것이 책임 분리에 맞다고 봅니다.

[좋은 코드 방향]
Repository는 저장/조회에 집중시키고, 저장 전에 필요한 유효성 검사는 Service나 도메인 메서드에서 수행한다.

[AI 에이전트에게 줄 코딩 지침]
Spring Repository에는 도메인 정책 분기, 가격/상태 유효성 판단, HTTP 응답 판단을 넣지 말고 데이터 접근 책임으로 제한하라.

[관련 키워드]
Repository, domain policy, responsibility separation, persistence, testability

### Request DTO에 서버 계산값을 받지 않는다

[안티패턴 이름]
클라이언트 신뢰 기반 DTO 설계

[나쁜 코드 예시]
주문 생성 요청 DTO에 `finalPrice`처럼 서버가 계산해야 하는 값을 포함한다.

[왜 문제인가]
클라이언트가 조작 가능한 값을 서버의 비즈니스 결과처럼 다룰 위험이 생긴다.

[실무에서 생길 수 있는 문제]
가격, 할인액, 권한, 상태값처럼 서버가 결정해야 하는 값이 조작되어 금전 손실이나 권한 우회가 발생할 수 있다.

[면접에서 설명할 문장]
Request DTO에는 사용자가 입력해야 하는 값만 받고, 최종 가격이나 상태처럼 서버 정책으로 결정되는 값은 서버에서 계산해야 합니다.

[좋은 코드 방향]
요청 DTO는 `customerType`, `itemCount`, `pricePerItem`처럼 입력값만 포함하고, `finalPrice`는 Service나 도메인 로직에서 계산한다.

[AI 에이전트에게 줄 코딩 지침]
Request DTO를 만들 때 서버가 계산하거나 결정해야 하는 필드인 가격, 할인액, 상태, 권한, 생성일시는 클라이언트 입력으로 받지 말라.

[관련 키워드]
DTO, validation, trust boundary, server-side calculation, security

### 문자열 상태값을 하드코딩하지 않는다

[안티패턴 이름]
문자열 상태 코드 하드코딩

[나쁜 코드 예시]
`"CONFIRMED"`, `"CANCELLED"`, `"BLOCKED"` 같은 상태값을 여러 계층에서 문자열로 직접 비교한다.

[왜 문제인가]
오타에 취약하고 허용 가능한 상태 목록이 코드로 제한되지 않는다.

[실무에서 생길 수 있는 문제]
잘못된 상태값이 저장되거나 분기 조건이 누락되어 예약, 주문, 결제 같은 상태 전이가 깨질 수 있다.

[면접에서 설명할 문장]
상태값이 정해져 있다면 문자열보다 enum으로 표현해 허용 가능한 값을 제한하고 컴파일 타임에 오타를 잡는 편이 안전합니다.

[좋은 코드 방향]
`ReservationStatus` 같은 enum을 만들고 Entity와 DTO에서 명확히 변환한다.

[AI 에이전트에게 줄 코딩 지침]
도메인 상태, 타입, 권한, 결제 상태처럼 허용 값이 제한된 필드는 문자열 상수 대신 enum 또는 명확한 값 객체로 표현하라.

[관련 키워드]
enum, state, type safety, domain modeling

### self-invocation에 `@Transactional` 효과를 기대하지 않는다

[안티패턴 이름]
Spring AOP self-invocation 오해

[나쁜 코드 예시]
같은 Service 클래스 안에서 `this.saveReservation()` 또는 `saveReservation()`을 호출하면서 호출 대상 메서드의 `@Transactional`이 새로 적용될 것이라고 기대한다.

[왜 문제인가]
Spring AOP는 프록시를 통해 외부에서 호출될 때 적용된다. 같은 객체 내부 호출은 프록시를 거치지 않는다.

[실무에서 생길 수 있는 문제]
트랜잭션 전파, 읽기 전용 설정, 재시도, 로깅, 권한 검사 같은 AOP 기반 정책이 의도대로 동작하지 않을 수 있다.

[면접에서 설명할 문장]
`@Transactional`은 프록시 기반 AOP로 동작하기 때문에 같은 클래스 내부 메서드 호출에는 별도 트랜잭션 설정이 적용되지 않습니다. 트랜잭션 경계는 보통 Service의 public 유스케이스 메서드에 명확히 두는 것이 좋습니다.

[좋은 코드 방향]
트랜잭션 경계를 유스케이스 메서드에 두고, 별도 전파 정책이 필요하면 다른 Spring bean으로 분리하거나 구조를 재검토한다.

[AI 에이전트에게 줄 코딩 지침]
Spring에서 `@Transactional`, `@Async`, 캐시, 보안 등 AOP 기반 어노테이션을 같은 클래스 내부 호출에 의존하도록 설계하지 말라.

[관련 키워드]
Spring AOP, proxy, self-invocation, transaction boundary, @Transactional

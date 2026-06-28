# Refactoring Plan

이 문서는 풀이 후 비교 학습용이다. 원본 코드가 유일하게 틀렸다는 뜻이 아니라, 더 안전하고 설명하기 쉬운 구조로 바꾸는 한 가지 예시를 정리한다.

## 기존 동작 보존 기준

- 예약 생성 시 사용자, 방, 시작 시간, 종료 시간을 받아 예약을 만든다.
- 이미 같은 방에 겹치는 예약이 있으면 예약을 막는다.
- 예약 취소 시 존재하지 않는 예약은 찾을 수 없다고 처리한다.
- 이미 취소된 예약은 다시 취소하지 않는다.
- 예약 성공, 예약 중복, 취소 성공, 이미 취소됨, 존재하지 않음 케이스를 테스트로 고정한다.

## 리팩터링 우선순위

1. 문자열 상태값을 `ReservationStatus` enum으로 바꾼다.
2. 요청 DTO에서 서버가 결정해야 하는 `status`, `createdAt`을 제거한다.
3. Service가 HTTP 상태 문자열 대신 결과 객체를 반환하게 한다.
4. Repository의 null 반환을 `Optional`로 바꾼다.
5. Repository의 비즈니스 판단을 Service 또는 도메인 객체로 옮긴다.
6. 테스트를 실행 가능한 형태로 만들고 성공/실패/경계 케이스를 추가한다.

## 계층별 변경 이유

### Controller

- 요청 DTO를 받고 Service를 호출하는 역할로 좁힌다.
- HTTP 응답 변환은 Controller가 담당하고, 예약 정책 판단은 Service에 둔다.

### Service

- 예약 생성, 중복 확인, 취소 같은 유스케이스를 담당한다.
- 트랜잭션 경계는 Service의 public 유스케이스 메서드에 둔다.
- 내부 저장 메서드에 별도 `@Transactional`을 기대하지 않는다.

### Repository

- 저장과 조회에 집중한다.
- 찾지 못한 결과는 `Optional`로 표현한다.
- 도메인 정책 검증은 Repository에 넣지 않는다.

### DTO / Entity

- `ReservationRequest`는 클라이언트 입력만 가진다.
- `ReservationResponse`는 API 응답으로 필요한 값만 가진다.
- `Reservation`은 서버가 관리하는 예약 상태를 가진다.

### Exception

- 예약 중복, 존재하지 않는 예약, 잘못된 요청을 구분할 수 있도록 예외 타입 또는 에러 코드를 둔다.

### Test

- 테스트가 실제로 실행되도록 `@Test`를 사용한다.
- 문자열 응답 포함 여부보다 결과 객체의 상태와 예외를 검증한다.

## 바꾸지 않은 부분과 이유

- 실제 DB/JPA 도입은 하지 않았다. 이 문제의 목적은 계층 책임과 안티패턴 탐지이므로 in-memory repository 예시로 충분하다.
- 실제 Spring `@RestController`, `@RequestBody`, `@Valid`, `ResponseEntity` import는 넣지 않았다. 현재 저장소에 Spring 의존성이 없기 때문에 개선 방향은 코드 구조로 보여준다.
- 시간은 단순화를 위해 Service에서 생성한다. 실무에서는 `Clock`을 주입하면 테스트 가능성이 더 좋아진다.

## 테스트로 고정할 동작

- 예약 생성 성공
- 겹치는 예약 생성 실패
- 잘못된 시간 범위 실패
- 존재하지 않는 예약 취소 실패
- 예약 취소 성공
- 이미 취소된 예약 재취소 실패

## 트레이드오프

- enum은 문자열보다 안전하지만, 외부 API와 주고받을 때 변환 규칙을 명확히 해야 한다.
- `Optional`은 null보다 의도를 드러내지만, Entity 필드나 DTO 필드에 남용하면 코드가 복잡해질 수 있다.
- Service가 결과 객체를 반환하면 테스트가 쉬워지지만, API 응답 포맷은 Controller에서 한 번 더 변환해야 한다.
- 실제 프로젝트라면 Repository는 JPA Repository로 바꾸고, 중복 예약 검사는 DB unique constraint나 lock 전략까지 함께 검토해야 한다.

## 면접에서 말할 문장

이 코드는 Controller, Service, Repository의 책임이 섞여 있고 문자열 상태값과 null 반환 때문에 변경과 테스트가 어렵습니다. 우선 요청 DTO에는 클라이언트 입력만 남기고, 상태값은 enum으로 제한하겠습니다. Service는 예약 생성과 취소 유스케이스를 담당하게 하고, Repository는 저장과 조회만 하도록 분리하겠습니다. 또 Service가 HTTP 문자열을 반환하지 않게 결과 객체를 두고, 테스트는 성공 케이스뿐 아니라 중복 예약과 취소 실패 같은 경계 케이스까지 고정하겠습니다.


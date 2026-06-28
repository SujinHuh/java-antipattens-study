# Solution Example

이 코드는 002 문제의 가능한 개선 예시다. 유일한 정답은 아니며 실제 Spring/JPA 프로젝트에서는 팀 컨벤션, 도메인 정책, DB 제약 조건에 따라 달라질 수 있다.

## 원본 대비 변경 요약

- `ReservationStatus_Refactored` enum으로 상태값을 제한했다.
- 요청 DTO에서 `status`, `createdAt`을 제거했다.
- 응답 DTO인 `ReservationResponse_Refactored`를 추가했다.
- Service가 HTTP 문자열 대신 결과 객체를 반환하도록 바꿨다.
- Repository는 저장과 조회만 담당하도록 단순화했다.
- `findById()`는 `null` 대신 `Optional`을 반환한다.
- 테스트는 실행 가능한 JUnit 형태의 예시로 작성했다.

## 비교해서 볼 지점

- 원본 `ReservationService.reserve()`는 Entity 생성, 저장, 응답 문자열 생성을 모두 한다.
- 개선 예시에서는 Service가 유스케이스를 처리하고 결과 객체를 반환한다.
- 원본 Repository는 상태와 시간 조건을 판단한다.
- 개선 예시의 Repository는 저장과 조회만 담당한다.

## 이름 규칙

원본 코드와 IntelliJ에서 충돌하지 않도록 개선 예시는 `com.example.reservation.refactoring` 패키지에 두고, 주요 타입 이름 뒤에 `_Refactored`를 붙였다.

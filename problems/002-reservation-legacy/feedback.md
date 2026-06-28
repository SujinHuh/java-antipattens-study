# Feedback

풀이 후 피드백을 기록합니다.

## 002-reservation-legacy

### 잘 잡은 부분

- Controller에서 문자열 파라미터를 직접 파싱하는 문제를 잡았다.
- 수동 if 검증과 광범위 예외 처리를 문제로 봤다.
- 문자열 상태값이 오타에 취약하다는 점을 잡았다.
- Repository의 static List, `println`, null 반환 문제를 잡았다.
- 테스트 케이스 부족과 JUnit 미사용을 잡았다.

### 놓친 부분

- DTO는 요청/응답 전달 객체이고 Entity는 DB 저장 대상이라는 역할 구분이 아직 헷갈렸다.
- `@Transactional`은 보통 Controller가 아니라 Service 유스케이스 경계에 둔다.
- 같은 클래스 내부 호출인 self-invocation에서는 Spring AOP 프록시를 거치지 않는다는 점을 놓쳤다.
- Repository의 `save()`가 비즈니스 정책을 판단하는 책임 위반을 덜 명확하게 짚었다.
- Service가 HTTP 상태 문자열을 반환하는 문제를 놓쳤다.

### 다음 문제 보강 방향

- DTO와 Entity 역할 구분을 다시 섞는다.
- `@Transactional` 위치와 self-invocation을 다시 섞는다.
- Service가 HTTP 응답 표현을 반환하는 문제를 다시 섞는다.
- Exception을 예외 타입, 메시지, 에러 코드, global handler 관점으로 보게 한다.

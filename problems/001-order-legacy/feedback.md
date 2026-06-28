# Feedback

풀이 후 피드백을 기록합니다.

## 001-order-legacy

### 잘 잡은 부분

- Controller에서 DTO로 받지 않는 점을 의심했다.
- Controller에서 if문으로 직접 400 응답을 만드는 점을 문제로 봤다.
- 입력 검증과 책임 위치를 고민했다.

### 놓친 부분

- Service의 책임 과다: 할인 계산, Entity 생성, 저장, 응답 문자열 생성을 한 메서드가 처리한다.
- 문자열 기반 고객 타입 비교: 오타와 null에 취약하다.
- Repository의 비즈니스 판단: `finalPrice == 0` 검증은 저장소 책임이 아니다.
- static List 저장소: 테스트 격리와 동시성 문제가 있다.
- DTO/Entity public field와 `finalPrice` 요청 필드 문제를 놓쳤다.
- 예외 메시지가 모호하고, Controller가 모든 예외를 삼킨다.
- 테스트가 JUnit 테스트로 실행되지 않고 케이스가 부족하다.

### 다음 문제 보강 방향

- Repository를 볼 때 "DB 접근만 하는가, 정책 판단을 하는가"를 먼저 확인한다.
- DTO/Entity를 볼 때 "클라이언트가 보내면 안 되는 값이 요청 DTO에 있는가"를 확인한다.
- Test를 볼 때 "실행 가능한 테스트인가, 실패/경계 케이스가 있는가"를 확인한다.

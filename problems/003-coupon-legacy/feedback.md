# Feedback

풀이 후 피드백을 기록합니다.

## 003-coupon-legacy

### 잘 잡은 부분

- Controller의 상태 코드 반환 문제를 잡았다.
- Controller에 검증/비즈니스 판단이 들어간 점을 의심했다.
- Util의 문자열 하드코딩 문제를 잡았다.
- 테스트에 `@Test`가 없고 시나리오가 부족한 점을 잡았다.

### 놓친 부분

- Entity를 요청/응답에 직접 사용하는 문제.
- 클라이언트가 `status` 같은 서버 관리 상태를 요청으로 보낼 수 있는 문제.
- Service의 `Object` 반환과 Entity 직접 수정/반환 문제.
- Service의 트랜잭션 경계 누락.
- 반복문 안 Repository 조회 문제.
- Repository의 static List, null 반환, println 로그, 중복 저장 문제.
- `RuntimeException` 직접 사용과 `CouponException` 미사용 문제.
- Util에 비즈니스 정책이 들어간 문제.
- `CouponUtil.canUse()`의 `status.equals("EXPIRED")`가 null-safe하지 않은 문제.

### 표현 보정

- "`@Getter/@Setter`가 없다"보다 "필드가 public이라 캡슐화가 없고 상태 변경을 통제할 수 없다"가 더 정확하다.
- "Repository는 크게 문제 없는 것 같다"에서 멈추지 말고 조회 방식, null 반환, 상태 공유, 로그, 저장 방식까지 확인한다.

### 다음 문제 보강 방향

- Repository 반복 조회를 다시 섞는다.
- Entity 직접 노출 문제를 다시 섞는다.
- Util 비즈니스 로직을 다시 섞는다.
- Exception 정의/사용 불일치를 다시 섞는다.
- 표면 버그보다 실무에서 그럴듯한 애매한 코드 비중을 늘린다.

### 면접관 기준 보정

- 이번 답변은 학습 점수로는 58점이지만, 실제 면접관 기준으로는 55점 정도가 적절하다.
- "잘 모르겠다"로 남긴 Repository/Exception/Service 영역은 면접에서 추가 질문이 들어올 가능성이 높다.
- 다음 답변부터는 문제를 찾는 데서 끝내지 말고 "왜 장애/운영/테스트 리스크가 되는지"까지 붙여야 한다.

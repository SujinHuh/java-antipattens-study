# TIL

오늘 새로 배운 내용을 기록합니다.

## 003-coupon-legacy

- Controller는 요청/응답 처리에 집중하고, 입력 검증은 DTO validation으로 표현하는 편이 좋다.
- Entity를 요청이나 응답에 직접 쓰면 API 모델과 저장 모델이 강하게 묶인다.
- Service에서 반복문 안 Repository 조회가 있으면 실제 DB 환경에서는 성능 문제가 될 수 있다.
- Util은 단순 공통 기능에는 쓸 수 있지만, 쿠폰 정책 같은 비즈니스 규칙을 넣으면 책임이 흐려진다.
- 문자열 상태값은 오타와 null에 취약하므로 enum이나 상태 전이 메서드로 제한하는 편이 안전하다.
- 예외 클래스가 있어도 실제 코드에서 사용하지 않으면 예외 정책이 없는 것과 비슷하다.
- `@Getter/@Setter` 유무보다 중요한 것은 캡슐화와 상태 변경 통제다.

## 키워드별 개념 설명

- DTO validation / `@Valid`: 요청 값의 필수 여부, 범위, 형식을 DTO에 표현하고 Controller에서는 검증된 요청을 Service로 넘기는 방식이다.
- Entity 직접 노출: Entity를 API 요청/응답에 그대로 쓰면 내부 저장 구조와 외부 API 계약이 강하게 묶인다.
- Transaction boundary / `@Transactional`: 상태 변경과 저장이 하나의 유스케이스라면 중간 실패 시 정합성을 지키기 위해 트랜잭션 경계를 명확히 둔다.
- Repository 반복 조회: 반복문 안에서 DB 조회가 발생하면 데이터 증가에 따라 쿼리 수가 늘어나는 성능 문제가 생길 수 있다.
- Exception handling: 도메인 실패, 잘못된 요청, 서버 오류를 구분해야 응답 상태 코드와 운영 로그가 명확해진다.
- Util 비즈니스 로직: 쿠폰 정책처럼 자주 바뀌고 테스트해야 하는 규칙은 Util보다 Policy/Domain/Service로 분리하는 편이 낫다.

# 004 Feedback Summary

- 학습 점수: 78점
- 실제 면접관 기준 점수: 70점

## 잘한 점

- Controller, Service, Repository, Util, DTO/Entity, Exception, Test 순서로 문제를 넓게 봤다.
- DI 직접 생성, Entity 직접 노출, 트랜잭션 경계, 반복 Repository 조회, null 반환, static List, 테스트 부실을 잡았다.

## 가장 중요한 보완점

- Service에서 `ResponseEntity.ok`로 고치자는 말은 위험하다. `ResponseEntity`는 Controller 책임이다.
- DTO 개선을 Lombok 중심으로 말하지 말고, API 계약과 validation 중심으로 말해야 한다.
- `N+1`은 "N+1 형태의 반복 Repository 조회"라고 안전하게 표현해야 한다.
- 조회 실패 시 요청 객체를 그대로 사용하는 문제를 더 강하게 잡아야 한다.
- 실제 DB 기반 코드와 인메모리 예제의 전제를 구분해서 `@Transactional`을 말해야 한다.
- 환불 대상 결제의 소유자 검증 누락을 더 강하게 잡아야 한다.

## 다음 문제에 다시 넣을 약점

- Service HTTP 응답 책임
- DTO validation
- 조회 실패 처리
- 소유자 검증 / 권한 검증
- 반복 Repository 조회
- 부가 작업 실패 정책

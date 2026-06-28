# 003. Coupon Legacy Review

## 상황

쿠폰 적용 가능 여부를 확인하고, 사용자의 주문 요약을 내려주는 짧은 Spring Boot 레거시 코드입니다.

## 다시 볼 때 먼저 확인할 것

이 문제를 복습할 때는 모든 파일을 한 번에 다 보려고 하지 않습니다.

우선 아래 5개만 먼저 확인합니다.

1. Controller가 Entity를 직접 받거나 반환하는지
2. Controller에 비즈니스 판단이 들어갔는지
3. Service에서 상태 변경/저장이 있는데 트랜잭션 경계가 보이는지
4. 반복문 안에서 Repository 조회가 발생하는지
5. `RuntimeException`, `null`, 문자열 상태값처럼 장애 원인을 숨기는 코드가 있는지

피드백을 다시 볼 때는 아래 순서만 먼저 봅니다.

1. `review-answer.md`의 `채점`
2. `review-answer.md`의 `반드시 잡았어야 하는 문제`
3. `review-answer.md`의 `시니어 개발자의 피드백 (면접관 관점)`

`solution/`과 `refactoring-plan.md`는 1차 복습 뒤에 봅니다.

## 리팩토링된 코드 위치

채점과 1차 복습이 끝난 뒤에는 아래 파일을 봅니다.

1. `refactoring-plan.md`
   - 어떤 순서로 고칠지, 기존 동작을 어떻게 보존할지 정리한 문서입니다.

2. `solution/README.md`
   - 원본 대비 어떤 점을 바꿨는지 요약한 문서입니다.

3. `solution/src/main/java/com/example/coupon/refactoring/`
   - 실제 리팩토링된 코드가 들어 있습니다.
   - 주요 타입 이름은 원본과 구분되도록 `_Refactored`를 붙였습니다.
   - 예: `CouponService` -> `CouponService_Refactored`

4. `solution/src/test/java/com/example/coupon/refactoring/CouponService_RefactoredTest.java`
   - 리팩토링된 코드 기준 테스트 예시입니다.

리팩토링된 코드에는 핵심 변경 지점마다 `기존 코드:` / `수정 코드:` 주석을 달아 원본에서 무엇이 바뀌었는지 볼 수 있게 했습니다.

## 읽는 순서

1. `src/main/java/com/example/coupon/CouponController.java`
2. `src/main/java/com/example/coupon/CouponService.java`
3. `src/main/java/com/example/coupon/CouponRepository.java`
4. `src/main/java/com/example/coupon/CouponUtil.java`
5. `src/main/java/com/example/coupon/Coupon.java`
6. `src/main/java/com/example/coupon/CouponRequest.java`
7. `src/main/java/com/example/coupon/CouponException.java`
8. `src/test/java/com/example/coupon/CouponServiceTest.java`

## 질문

코드 리뷰하듯이 문제점을 찾아 `review-answer.md`에 작성하세요.

정답, 힌트, 문제점 목록은 아직 보지 않습니다.

# Solution Example

이 코드는 003 문제의 가능한 개선 예시다. 실제 회사 코드에서는 도메인 정책, DB 구조, 팀 컨벤션에 따라 달라질 수 있다.

이 저장소는 Spring Boot 의존성을 일부러 두지 않은 리뷰 연습용 코드라 `@Service`, `@Transactional` 같은 Spring 어노테이션은 주석으로 표시했다.

## 리팩토링 코드 파일

주요 리팩토링 코드는 아래 파일에 있다.

1. `src/main/java/com/example/coupon/refactoring/CouponController_Refactored.java`
2. `src/main/java/com/example/coupon/refactoring/CouponService_Refactored.java`
3. `src/main/java/com/example/coupon/refactoring/CouponRepository_Refactored.java`
4. `src/main/java/com/example/coupon/refactoring/InMemoryCouponRepository_Refactored.java`
5. `src/main/java/com/example/coupon/refactoring/CouponPolicy_Refactored.java`
6. `src/main/java/com/example/coupon/refactoring/CouponRequest_Refactored.java`
7. `src/main/java/com/example/coupon/refactoring/CouponResponse_Refactored.java`
8. `src/main/java/com/example/coupon/refactoring/Coupon_Refactored.java`
9. `src/main/java/com/example/coupon/refactoring/CouponException_Refactored.java`
10. `src/test/java/com/example/coupon/refactoring/CouponService_RefactoredTest.java`

원본 코드와 구분되도록 주요 타입 이름에는 `_Refactored`를 붙였다.
핵심 변경 지점에는 `기존 코드:` / `수정 코드:` 주석을 달았다.

## 원본 대비 변경 요약

- `Coupon` Entity를 Controller 요청/응답에 직접 쓰지 않도록 DTO를 분리했다.
- 클라이언트가 `status` 같은 서버 관리 상태를 보내지 않도록 요청 DTO에서 제거했다.
- `CouponStatus_Refactored` enum으로 문자열 상태값을 제한했다.
- `CouponService_Refactored`는 `Object`가 아니라 `CouponResponse_Refactored`를 반환한다.
- 기존 쿠폰을 Repository에서 조회해 쿠폰 존재 여부, 소유자, 사용/만료 상태를 확인한다.
- 반복문 안 Repository 조회를 사용자 주문 목록 단일 조회로 바꿨다.
- `CouponUtil`의 비즈니스 정책을 `CouponPolicy_Refactored`로 분리했다.
- `RuntimeException` 직접 사용 대신 `CouponException_Refactored`와 `CouponErrorCode_Refactored`를 사용한다.
- 알 수 없는 쿠폰 코드는 새 쿠폰처럼 저장하지 않고 존재하지 않는 쿠폰으로 거절한다.
- 핵심 변경 지점에는 `기존 코드:` / `수정 코드:` 주석을 달았다.

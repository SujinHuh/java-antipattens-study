# 007. Stock Coupon Legacy Review

## 상황

한정 수량 쿠폰을 발급하는 짧은 Java/Spring Boot 스타일 레거시 코드입니다.

동시에 여러 사용자가 쿠폰을 발급받을 수 있고, 같은 사용자가 같은 요청을 재시도할 수도 있다고 가정합니다.

코드만 보고 문제점을 찾아 리뷰하세요.
정답, 개념 키워드, 개선 코드는 아직 보지 않습니다.

## 볼 순서

1. `src/main/java/com/example/stockcoupon/StockCouponController.java`
2. `src/main/java/com/example/stockcoupon/StockCouponService.java`
3. `src/main/java/com/example/stockcoupon/StockCouponRepository.java`
4. `src/main/java/com/example/stockcoupon/CouponIssueHistoryRepository.java`
5. `src/main/java/com/example/stockcoupon/StockCouponEventPublisher.java`
6. `src/main/java/com/example/stockcoupon/StockCouponRequest.java`
7. `src/main/java/com/example/stockcoupon/StockCoupon.java`
8. `src/main/java/com/example/stockcoupon/StockCouponException.java`
9. `src/test/java/com/example/stockcoupon/StockCouponServiceTest.java`

## 답변 작성 위치

`review-answer.md`에 Controller, Service, Repository, Event/External, DTO/Entity, Exception, Test 순서로 작성합니다.

## 주의

- 코드에 보이는 흐름을 기준으로 리뷰합니다.
- 정답 문서나 개선 코드는 답변 제출 전에는 보지 않습니다.

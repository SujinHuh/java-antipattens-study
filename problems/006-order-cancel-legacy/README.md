# 006. Order Cancel Legacy Review

## 상황

주문 취소 API의 레거시 코드입니다.
Controller -> Service -> Repository -> Util 흐름으로 이어지는 짧은 Java/Spring Boot 스타일 코드라고 가정합니다.

코드만 보고 문제점을 찾아 리뷰하세요.
정답, 개념 키워드, 개선 코드는 아직 보지 않습니다.

## 볼 순서

1. `src/main/java/com/example/order/OrderController.java`
2. `src/main/java/com/example/order/OrderService.java`
3. `src/main/java/com/example/order/OrderRepository.java`
4. `src/main/java/com/example/order/OrderCancelUtil.java`
5. `src/main/java/com/example/order/OrderCancelRequest.java`
6. `src/main/java/com/example/order/Order.java`
7. `src/main/java/com/example/order/OrderCancelException.java`
8. `src/test/java/com/example/order/OrderServiceTest.java`

## 답변 작성 위치

`review-answer.md`에 Controller, Service, Repository, Util, DTO/Entity, Exception, Test 순서로 작성합니다.

## 주의

- `답변후-개념공부.md`는 답변 제출 후에 봅니다.
- 코드 안에서 의심되는 부분을 직접 찾고, 왜 문제인지와 개선 방향을 함께 적습니다.

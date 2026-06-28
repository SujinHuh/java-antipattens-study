# 001. Order Legacy Review

## 학습 목표

Controller -> Service -> Repository -> DTO/Entity -> Exception -> Test 순서로 코드를 읽으며 안티패턴을 찾는다.

## 읽는 순서

1. `src/main/java/com/example/order/OrderController.java`
2. `src/main/java/com/example/order/OrderService.java`
3. `src/main/java/com/example/order/OrderRepository.java`
4. `src/main/java/com/example/order/OrderRequest.java`
5. `src/main/java/com/example/order/Order.java`
6. `src/main/java/com/example/order/OrderException.java`
7. `src/test/java/com/example/order/OrderServiceTest.java`

## 문제

아래 관점으로 코드 리뷰 답변을 작성하세요.

1. 각 계층에서 보이는 안티패턴을 찾으세요.
2. 왜 문제가 되는지 설명하세요.
3. 리팩터링 방향을 제안하세요.
4. 기존 동작을 보존하기 위해 어떤 테스트가 필요한지 말하세요.

## 힌트

- Controller가 HTTP 책임만 가지고 있는지 확인하세요.
- Service가 너무 많은 책임을 갖고 있지 않은지 확인하세요.
- Repository가 저장소 역할과 도메인 판단을 섞고 있지 않은지 확인하세요.
- DTO와 Entity가 같은 역할을 하고 있지 않은지 확인하세요.
- Exception이 원인을 충분히 드러내는지 확인하세요.
- Test가 실제 비즈니스 규칙을 검증하는지 확인하세요.


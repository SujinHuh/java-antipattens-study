# 반복문 내부의 DB 조회 (N+1 문제와 성능 저하)

## 1. 안티패턴: `for` / `while` 문 안에서 DB 조회하기

가장 흔하게 발생하면서도 **가장 치명적인 성능 저하**를 일으키는 안티패턴 중 하나입니다.
목록 데이터를 순회하면서, 그 내부에서 매번 Repository(DB)를 호출하여 추가 데이터를 가져오는 방식입니다.

**❌ 잘못된 코드 예시 1 (003번 문제: N+1 조회)**
```java
public void processCoupons() {
    List<Long> orderIds = orderRepository.findAllOrderIds();

    for (Long orderId : orderIds) {
        // ❌ 반복문 안에서 연관된 데이터를 계속 DB에서 조회 (1000번의 쿼리 발생!)
        int amount = orderRepository.findOrderAmount(orderId);
    }
}
```

**❌ 잘못된 코드 예시 2 (004번 문제: 이미 가져온 데이터를 또 단건 조회하는 중복 쿼리)**
```java
public List<PointPayment> getRefundHistory(Long userId) {
    // 1. 전체 결제 내역을 1번의 쿼리로 다 가져옴
    List<PointPayment> payments = pointPaymentRepository.findAll();

    for (PointPayment payment : payments) {
        // ❌ 2. 이미 payment 객체 안에 모든 정보가 들어있는데도, id로 똑같은 데이터를 또 단건 조회함!
        // 데이터가 1000개면 무의미하게 DB를 똑같은 데이터로 1000번 더 찌르게 됨.
        PointPayment latest = pointPaymentRepository.findById(payment.id);
        
        if (latest.userId.equals(userId)) { ... }
    }
}
```

## 2. 왜 치명적인가? (N+1 문제)

DB와 통신하는 작업(네트워크 I/O)은 애플리케이션 안에서 메모리를 읽는 것보다 수백~수천 배 느립니다.

- 만약 회원이 1명이라면? 
  → 전체 목록 조회(1번) + 회원 상세 조회(1번) = 총 2번의 쿼리. (문제 없음)
- **만약 회원이 10,000명이라면?**
  → 전체 목록 조회(1번) + 회원 상세 조회(10,000번) = **총 10,001번의 쿼리!** 😱

이처럼 처음에 리스트를 가져오기 위한 1번의 쿼리(`1`)와, 그 리스트의 개수만큼 추가로 발생하는 쿼리(`N`번)를 합쳐서 실무에서는 **"N+1 문제"**라고 부릅니다. 

## 3. 올바른 해결 방법

반복문 안에서 DB를 찌르는 대신, **"필요한 데이터를 DB에서 한 번에 뭉텅이로 가져온 뒤, 애플리케이션 메모리(Map 등)에 올려두고 매칭"** 하는 방식을 사용해야 합니다.

**✅ 해결된 코드 예시 (IN 절 활용)**
```java
public void processCoupons() {
    // 1. 주문 ID 목록을 가져옴
    List<Long> orderIds = orderRepository.findAllOrderIds();

    // 2. ✅ IN 쿼리를 사용해, 1000개의 주문 금액을 단 1번의 쿼리로 싹 다 가져옴
    // SQL: SELECT id, amount FROM orders WHERE id IN (1, 2, 3, ...)
    List<OrderAmountDto> amounts = orderRepository.findAmountsByOrderIds(orderIds);

    // 3. 애플리케이션 메모리에서 Map으로 변환 (빠른 조회를 위해)
    Map<Long, Integer> amountMap = amounts.stream()
        .collect(Collectors.toMap(OrderAmountDto::getId, OrderAmountDto::getAmount));

    // 4. 반복문 안에서는 DB 호출 없이 메모리(Map)에서만 값을 꺼내 씀! ⚡️
    for (Long orderId : orderIds) {
        int amount = amountMap.get(orderId);
        // ... 금액 계산 로직 ...
    }
}
```

## 4. 핵심 요약

> **"반복문(for, while) 안에는 절대로 DB 접근 코드(Repository 호출)를 넣지 않는다!"**
> 필요한 데이터가 있다면 쿼리 한 번(JOIN이나 IN 절)으로 미리 다 가져온 뒤에 반복문을 돌려야 한다.

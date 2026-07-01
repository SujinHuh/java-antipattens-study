# 트랜잭션 예외 삼킴과 롤백 메커니즘

Spring `@Transactional` 환경에서 예외 처리 시 발생하는 롤백 정책 위반과
`try-catch`를 이용한 예외 삼킴(Swallowing) 안티패턴의 위험성을 다룹니다.

---

## 1. Spring의 예외 종류별 기본 롤백 정책

Spring의 `@Transactional`은 예외가 던져졌을 때 기본적으로 다음과 같이 동작합니다.

| 예외 종류 | 대표 클래스 | 기본 롤백 여부 | 롤백 정책 설명 |
| :--- | :--- | :---: | :--- |
| **Unchecked Exception** | `RuntimeException`, `Error` | **롤백 (Rollback)** | 시스템 에러나 복구 불가능한 런타임 오류로 간주하여 롤백 |
| **Checked Exception** | `Exception` (비-RuntimeException) | **커밋 (Commit)** | 비즈니스적인 예외(복구 가능)로 간주하여 롤백하지 않음 |

### 💡 주의점
* Checked Exception(예: `IOException`, `SQLException`) 발생 시 트랜잭션을
  롤백하려면 `@Transactional(rollbackFor = Exception.class)`와 같이
  명시적으로 롤백 대상 지정을 지정해 주어야 안전합니다.

---

## 2. 트랜잭션 내부의 예외 삼킴 (Try-Catch Swallow)

### ❌ 안티패턴 (007번 예시)
트랜잭션이 선언된 비즈니스 로직 중간에 DB 저장이나 이벤트를 던지는 부분을
`try-catch`로 감싸서 예외를 정상 응답으로 무마하는 형태입니다.

```java
// StockCouponService.java
@Transactional
public StockCouponResponse issueInternal(StockCouponRequest request) {
    // 1. 재고 1 차감
    coupon.decreaseQuantity();
    stockCouponRepository.save(coupon);
    
    try {
        // 2. 발급 이력 저장 (여기서 DB 제약조건 위반 예외가 터진다면?)
        issueHistoryRepository.save(history);
    } catch (Exception e) {
        // 3. 에러를 삼키고 정상 Response를 반환해 버림!
        return new StockCouponResponse(..., "PENDING", ...);
    }
    
    return new StockCouponResponse(..., "ISSUED", ...);
}
```

### 💥 발생하는 심각한 문제
1. **데이터 정합성 파탄 (반쪽 커밋)**:
   * `issueHistoryRepository.save`에서 중복 데이터나 제약조건 오류로 예외가
     발생했으나, 이를 catch 블록이 삼켜 서비스 메서드는 오류 없이 정상 종료됩니다.
   * Spring 트랜잭션 관리자는 에러가 없었다고 생각하고 트랜잭션을 **커밋(Commit)**합니다.
   * 결국 **쿠폰 재고는 줄어들었는데 발급 이력은 적재되지 않는** 정합성 파탄이 발생합니다.
2. **`UnexpectedRollbackException` 발생**:
   * JPA 영속성 컨텍스트(또는 DB 커넥션) 내부에서 데이터 에러가 감지되면, 이미 해당
     트랜잭션은 **롤백 전용(Rollback-only)** 상태로 마킹됩니다.
   * 비록 자바 코드가 에러를 catch하여 정상 반환하더라도, Spring이 최종 커밋을 시도할 때
     이미 롤백 전용 마크가 붙어 있어 강제로 **`UnexpectedRollbackException`**을 발생시킵니다.
   * 결국 호출한 클라이언트는 예상했던 `"PENDING"` 응답 대신 500 런타임 예외를 받게 됩니다.

---

## 3. 올바른 개선 방향

비즈니스적으로 발생할 수 있는 검증 예외(예: 잔액 부족, 재고 부족)와 달리,
인프라나 DB 정합성과 직결된 물리 에러는 **절대 잡아선 안 되며 서비스 외부로 던져야 합니다.**

```java
// 서비스는 예외를 포착해서 삼키지 않고 그대로 상위로 전파(Rethrow)합니다.
public StockCouponResponse issueInternal(StockCouponRequest request) {
    coupon.decreaseQuantity();
    stockCouponRepository.save(coupon);
    
    // 예외가 발생하면 자연스럽게 서비스 밖으로 전파되어 트랜잭션이 롤백됩니다.
    issueHistoryRepository.save(history);
    
    return StockCouponResponse.of(coupon, request.userId(), "ISSUED");
}
```

---

## 4. "catch하면 무조건 트랜잭션이 안 돈다"가 아니다

헷갈리기 쉬운 지점은 이것입니다.

`try-catch`가 있다고 해서 트랜잭션 자체가 작동하지 않는 것은 아닙니다.
트랜잭션이 시작된 상태에서 예외가 발생했을 때, 그 예외를 밖으로 던지느냐 삼키느냐가 중요합니다.

### 예외를 밖으로 던지는 경우

```java
@Transactional
public void issue() {
    couponRepository.save(coupon);
    issueHistoryRepository.save(history); // RuntimeException 발생
}
```

RuntimeException이 메서드 밖으로 던져지면 Spring은 트랜잭션을 롤백합니다.

### 예외를 잡아서 정상 반환하는 경우

```java
@Transactional
public StockCouponResponse issue() {
    try {
        couponRepository.save(coupon);
        issueHistoryRepository.save(history);
    } catch (Exception e) {
        return StockCouponResponse.pending();
    }

    return StockCouponResponse.issued();
}
```

이 경우 메서드가 정상 종료된 것처럼 보이므로 Spring은 커밋을 시도할 수 있습니다.
즉, 문제는 "save가 여러 개라서"가 아니라 **DB 정합성 예외를 삼켜서 롤백 신호가 사라지는 것**입니다.

면접용 문장:

```text
try-catch 자체가 트랜잭션을 무효화하는 것은 아닙니다.
문제는 DB 저장 예외를 catch해서 정상 응답으로 바꾸면 Spring이 롤백해야 할 실패로 인식하지 못할 수 있다는 점입니다.
정합성과 관련된 예외는 삼키지 말고 전파해서 트랜잭션이 롤백되게 해야 합니다.
```

---

## 5. 면접 답변 템플릿

### Q. 트랜잭션 메서드 내부에서 try-catch로 예외를 잡으면 롤백이 어떻게 동작하나요?

> "Spring의 `@Transactional`은 메서드 실행 중 예외가 던져져야 롤백이 동작합니다.
> 
> 만약 메서드 내부에서 DB 쓰기 예외를 `try-catch`로 잡아서 삼키고 정상 종료하면,
> 트랜잭션 관리자는 성공한 것으로 판단하여 DB에 커밋을 시도하게 됩니다.
> 이 경우 정합성이 깨지거나 반쪽짜리 커밋이 될 수 있어 위험합니다.
> 
> 또한 DB 레벨에서 이미 에러가 발생하여 트랜잭션이 '롤백 전용(Rollback-only)'으로
> 마킹된 상태라면, 정상 리턴을 하더라도 최종 커밋 시점에 `UnexpectedRollbackException`이
> 발생하며 결국 실패하게 됩니다.
> 
> 따라서 DB 일관성과 직결되는 기술적인 런타임 예외는 삼키지 않고 그대로 전파시켜
> 트랜잭션이 정상적으로 롤백되도록 해야 합니다."

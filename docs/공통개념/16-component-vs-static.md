# `@Component`와 `static` 메서드의 충돌

## 1. `@Component`란?

Spring에게 **"이 클래스를 내가 관리하는 Bean(부품)으로 등록해줘!"** 라고 알려주는 어노테이션입니다.

Spring Bean으로 등록되면:
- Spring이 자동으로 객체를 하나 만들어서 애플리케이션 전체에서 관리
- `@Autowired`나 생성자 주입(DI)으로 어디서든 주입받아 사용 가능
- 테스트할 때 Mock 객체로 교체 가능 → 테스트 용이성 향상

---

## 2. `@Component` + `static` 메서드 = 모순!

아래 코드처럼 `@Component`를 달면서 동시에 메서드를 모두 `static`으로 선언하면 **두 선택이 서로 충돌**합니다.

```java
// @Component  ← Spring Bean으로 쓰겠다고 달아놨는데...
public class PointRefundUtil {

    public static boolean isWeekendRefundBlocked(...) { ... } // ← static!
    public static int calculateFee(...) { ... }               // ← static!
    public static void sendRefundNotice(...) { ... }          // ← static!
}
```

| | `@Component` (Bean) | `static` 메서드 |
|---|---|---|
| **객체 생성** | Spring이 객체를 만들어 관리 | 객체 없이 클래스명으로 바로 호출 |
| **DI 가능** | ✅ 가능 | ❌ 불가 |
| **테스트 Mock** | ✅ 교체 가능 | ❌ 불가 |

`static` 메서드는 `PointRefundUtil.isWeekendRefundBlocked()` 처럼 **객체가 없어도 클래스 이름으로 바로 호출**합니다. Spring의 DI 시스템을 전혀 활용하지 않으므로, `@Component`를 달아도 아무 효과가 없습니다.

---

## 3. 올바른 선택지

### 선택 A: 진짜 순수한 도구 메서드라면 → `static` 유지, `@Component` 제거
```java
// @Component 없이 순수 static 유틸로 사용
public class StringUtil {
    public static String maskPhoneNumber(String phone) { ... }
}

// 어디서든 클래스명으로 바로 호출
StringUtil.maskPhoneNumber("010-1234-5678");
```

### 선택 B: 비즈니스 로직이 포함되어 있다면 → Bean으로 만들어 DI + Mock 테스트 가능하게
```java
@Component // Bean으로 등록
public class RefundPolicyService { // Util이 아니라 Policy나 Service로 명칭 변경
    
    // static 제거! → DI로 주입받아 사용, Mock으로 테스트 가능
    public int calculateFee(PointPayment payment) {
        if ("VIP".equals(payment.refundReason)) return 0;
        if (payment.amount > 5000) return 500;
        return 100;
    }
}
```

---

## 4. 추가 안티패턴: Util에 비즈니스 로직 포함

`PointRefundUtil.calculateFee()` 를 보면:
```java
public static int calculateFee(PointPayment payment) {
    if ("VIP".equals(payment.refundReason)) { // ← "VIP면 수수료 0원" = 비즈니스 정책!
        return 0;
    }
    if (payment.amount > 5000) {              // ← "5000원 초과면 500원" = 비즈니스 정책!
        return 500;
    }
    return 100;
}
```

이건 단순 도구(도구 상자)가 아니라 **"이 쿠폰의 수수료가 얼마인가?"를 판단하는 비즈니스 정책**입니다.
Util이 아니라 Service 또는 별도의 Policy 객체가 담당해야 합니다.

---

## 5. 핵심 요약

> **`@Component`를 달았으면 `static` 메서드를 쓰지 마라.** 둘은 함께 쓰면 의미가 없다.
>
> - 진짜 도구 기능(날짜 변환, 문자열 처리 등) → `static` 메서드, `@Component` 없이
> - 비즈니스 정책 판단 → `static` 제거, `@Component`(또는 `@Service`)로 Bean 등록 후 DI

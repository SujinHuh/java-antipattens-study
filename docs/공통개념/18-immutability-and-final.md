# 객체 설계의 불변성(Immutability)과 final 사용 원칙

객체지향 설계와 Spring Boot 환경에서 식별자나 변하지 않는 필드에 `final`을 적용하는 이유와 설계 원칙에 대해 다룹니다.

---

## 1. final 사용의 근본적인 이유

자바에서 필드에 `final` 키워드를 적용하면 **객체가 생성된 이후 해당 필드의 참조나 값을 변경(재할당)할 수 없도록 강제**합니다.

```java
public class User {
    private final Long id; // 생성 시 할당된 이후 절대 변경 불가
    private String name;   // 개명 등으로 변경 가능
    
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

### ① 식별자(Identity) 보호 (비즈니스 규칙 강제)
객체의 고유 식별자(PK, FK 등)는 비즈니스 도메인 규칙상 생성 이후 값이 변해서는 안 됩니다.
- 예: 배송지 엔티티의 `orderId`는 특정 주문에 묶여 있으므로 다른 주문 ID로 변경될 수 없습니다.
- 이를 `final`로 고정하면 도메인 객체의 정체성(Identity)을 생명주기 전체에 걸쳐 안전하게 보호할 수 있습니다.

### ② 컴파일 타임의 버그 방지
필드가 `final`이 아니면 개발자가 실수로 값 변경 메서드 내에서 식별자 값을 오염시키는 코드를 작성할 위험이 있습니다.
- `final`을 사용하면 재할당을 시도하는 즉시 **컴파일러가 오류를 잡아내어** 런타임 버그를 원천 차단합니다.

### ③ 안전한 멀티스레드 환경 (Thread-Safe)
불변 객체(Immutable Object)는 여러 스레드가 동시에 접근해도 상태가 변하지 않기 때문에 동기화(`synchronized`) 처리를 하지 않아도 항상 동시성 안전성을 보장합니다.

---

## 2. DTO vs Entity에서의 final 적용 원칙

DTO와 Entity는 역할이 완전히 다르므로 `final`을 적용하는 전략도 다릅니다.

### ① DTO (Data Transfer Object) — "100% 불변" 권장

DTO는 단순히 데이터를 계층 간 전달하기 위한 껍데기입니다. 비즈니스 로직을 가지지 않으며 도중에 데이터가 수정될 필요가 전혀 없습니다.
따라서 **DTO의 모든 필드는 `private final`로 선언하여 완전한 불변 객체**로 만드는 것이 모범 사례(Best Practice)입니다.

* **일반 클래스 기반 불변 DTO:**
```java
public class OrderCancelRequest {
    private final Long orderId;
    private final Long userId;
    private final String reason;

    public OrderCancelRequest(Long orderId, Long userId, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public String getReason() { return reason; }
}
```

* **Java 16+ Record 기반 DTO (가장 권장):**
자바의 `record`를 사용하면 모든 필드가 자동으로 `private final`로 지정되며 생성자와 Getter가 자동으로 빌드됩니다.
```java
public record OrderCancelRequest(
    Long orderId,
    Long userId,
    String reason
) {}
```

### ② Entity — "식별자는 final, 상태는 private 변경 통제"

Entity는 식별자 외에 비즈니스 흐름에 따라 지속적으로 상태가 변경되는 필드(예: `status`, `address`)를 가집니다.
따라서 모든 필드를 `final`로 만들 수는 없지만, 다음과 같이 구분하여 설계해야 합니다.

* **식별자/상수 필드 (`final` 사용)**: `id`, `userId`, `orderId`, `createdAt` 등 생성 이후 불변이어야 하는 값.
* **상태 필드 (`private` + 도메인 메서드)**: 상태가 변해야 하는 값은 `final`을 사용하지 않되, `public` 노출이나 `Setter` 제공을 철저히 금지하고 Entity 내부의 의미 있는 메서드(예: `cancel()`, `changeAddress()`)를 통해서만 상태 변경을 허용(캡슐화)합니다.

---

## 3. 면접 답변 템플릿

### Q. Entity나 DTO 설계 시 필드에 final을 적용하는 이유는 무엇인가요?

> "`final` 키워드를 적용하면 객체 생성 이후 값이 변경되거나 재할당되는 것을 차단할 수 있습니다. 
> 
> 첫째, 엔티티의 식별자(PK, FK 등)처럼 생성 이후 변경되어서는 안 되는 값을 보호하여 도메인 규칙의 일관성을 유지할 수 있습니다.
> 둘째, 개발자의 단순 실수로 인한 상태 변경 오류를 컴파일 타임에 즉시 잡아낼 수 있습니다.
> 셋째, DTO의 경우 데이터를 전달하는 역할에 집중하므로 모든 필드를 불변(final)으로 설계하여 다중 스레드 환경에서도 동시성 문제없이 안전하게 데이터를 교환하도록 만듭니다."

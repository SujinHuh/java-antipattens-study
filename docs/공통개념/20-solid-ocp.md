# SOLID 개방-폐쇄 원칙 (OCP)과 분기 처리 안티패턴

객체지향 설계의 5대 원칙(SOLID) 중 하나인 개방-폐쇄 원칙(OCP, Open-Closed Principle)의 정의와 실무 코드에서 흔히 발생하는 위반 사례(특히 분기 처리 안티패턴) 및 해결 방법을 다룹니다.

---

## 1. 개방-폐쇄 원칙 (OCP)이란?

> **"소프트웨어 개체(클래스, 모듈, 함수 등)는 확장에는 열려 있어야 하고, 변경에는 닫혀 있어야 한다."**

- **확장에 열려 있다 (Open for Extension)**: 새로운 기능이나 새로운 타입(클래스)이 추가될 때, 시스템의 기능을 쉽게 확장할 수 있어야 합니다.
- **변경에 닫혀 있다 (Closed for Modification)**: 새로운 기능이 추가되더라도, **기존에 작성된 핵심 코드(특히 비즈니스 로직을 담당하는 Service 등)는 수정하지 않아야 합니다.**

---

## 2. OCP 위반의 가장 흔한 징후: "if/else 분기 처리"

실무에서 OCP를 위반하는 가장 대표적인 코드 모양은 **"타입이나 상태값을 가지고 if-else 혹은 switch-case 분기를 타는 절차지향적 코드"**입니다.

### ❌ OCP 위반 사례 (006번 예시)
주문 타입이 추가될 때마다 `cancel()` 메서드 내부의 if-else 분기를 계속 뜯어고쳐야 합니다.
```java
// 새로운 타입(예: GROUP_BUY)이 추가되면 이 핵심 Service 코드를 직접 수정해야 함 ➜ OCP 위반!
if ("NORMAL".equals(order.type)) {
    refundAmount = order.amount;
} else if ("PREORDER".equals(order.type)) {
    refundAmount = (int) (order.amount * 0.9);
} else if ("DIGITAL".equals(order.type)) {
    throw new OrderCancelException("digital items cannot be cancelled");
}
```

---

## 3. 005번 배송지 변경 문제에서의 OCP 위반 포인트 🔍

005번 배송지 변경 문제에서도 은밀하게 OCP를 위반하고 있는 부분이 2군데 존재합니다.

### ① 검증 규칙 추가 시 Service 코드의 수정 불가피 (Address Validation)
```java
// DeliveryAddressService.java
if (DeliveryAddressUtil.isBlockedAddress(request.address)) {
    throw new DeliveryAddressException("blocked address");
}
```
* **문제점**: 
  지금은 "차단 주소 검사" 규칙 하나만 존재하지만, 실무에서는 **"올바른 우편번호 포맷 검사"**, **"배송 불가 도서산간 지역 검사"** 등 새로운 검증 규칙들이 계속 추가됩니다.
  그때마다 `DeliveryAddressService` 클래스 안에 `if` 문을 한 줄씩 계속 추가해야 합니다. (변경에 닫혀있지 못하므로 OCP 위반)
* **해결책 (OCP 준수)**:
  `AddressValidator`라는 인터페이스를 정의하고, 다양한 검증기(Validator)들을 구현체로 만듭니다. Service는 이 검증기 리스트를 주입받아 루프만 돌려 검증합니다.
  새로운 검증 룰이 추가되어도 핵심 Service 코드는 단 한 줄도 수정되지 않고, 새 Validator 클래스만 구현하면 됩니다.

```java
// 인터페이스 정의
public interface AddressValidator {
    void validate(String address);
}

// Service에서는 변경 없이 사용 (OCP 준수)
private final List<AddressValidator> validators;

public void changeAddress(DeliveryAddressRequest request) {
    // 새로운 검증 규칙이 백 개 추가되어도 이 코드는 변경 없음!
    validators.forEach(v -> v.validate(request.address));
    ...
}
```

### ② 배송 상태 추가 시 분기 처리 문제 (Delivery Status)
```java
// DeliveryAddressService.java
if ("DELIVERING".equals(current.status)) {
    return Map.of("status", 200, "message", "already delivering");
}
```
* **문제점**:
  배송 주소 변경을 막아야 하는 상태가 `DELIVERING`뿐만 아니라, 나중에 `SHIPPED`(배송 완료), `RETURNING`(반품 진행 중) 등이 추가될 경우, 이 `if` 조건식을 계속 수정해야 합니다. (`if ("DELIVERING".equals(...) || "SHIPPED".equals(...))`)
* **해결책 (OCP 준수)**:
  상태값을 Enum(`DeliveryStatus`)으로 정의하고, 각 상태가 "주소 변경이 가능한 상태인지" 여부를 반환하는 책임을 Enum이나 Entity 내부 메서드에 위임합니다.
  새로운 상태가 추가되더라도 Service 코드는 수정되지 않습니다.

```java
public enum DeliveryStatus {
    READY(true),
    DELIVERING(false),
    SHIPPED(false); // 새로운 상태가 추가되어도 여부가 Enum 내부에 고정됨

    private final boolean addressChangeable;
    
    DeliveryStatus(boolean addressChangeable) {
        this.addressChangeable = addressChangeable;
    }

    public boolean isAddressChangeable() { return addressChangeable; }
}

// Service 코드
if (!current.getStatus().isAddressChangeable()) {
    throw new DeliveryAddressException("주소 변경이 불가능한 상태입니다.");
}
```

---

## 4. 면접 답변 템플릿

### Q. OCP(개방-폐쇄 원칙)란 무엇이며, 어떤 코드 모양에서 OCP 위반을 의심할 수 있나요?

> "OCP는 새로운 요구사항이 추가될 때 기존 핵심 비즈니스 코드는 수정하지 않고(변경에 폐쇄), 확장 인터페이스나 다형성을 통해 쉽게 기능을 확장할 수 있어야 한다(확장에 개방)는 설계 원칙입니다.
> 
> 대표적으로 **상태값이나 타입을 판별하기 위해 if-else나 switch 분기문이 절차지향적으로 나열되어 있는 코드**에서 OCP 위반을 가장 강력하게 의심할 수 있습니다. 
> 새로운 타입이나 규칙이 추가될 때마다 해당 분기 조건식을 계속 뜯어고쳐야 하기 때문입니다. 이를 해결하기 위해 **인터페이스 분리와 전략 패턴(Strategy Pattern)** 등을 사용하여 조건별 동작을 클래스로 분리해 주입받는 구조로 개선해야 합니다."

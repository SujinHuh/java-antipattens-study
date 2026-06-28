# Repository를 Interface와 구현체로 나누는 이유

리팩토링된 코드를 보면 기존에 하나였던 `Repository` 클래스가 다음과 같이 두 개로 나뉘어 있는 것을 볼 수 있습니다.

1. **`OrderRepository` (Interface)**: 껍데기 (어떤 기능이 있는지만 정의)
2. **`InMemoryOrderRepository` (Class)**: 실제 구현체 (메모리에 리스트로 저장하는 실제 로직)

왜 굳이 코드를 두 개로 쪼개서 복잡하게 만들었을까요? 아주 중요한 객체지향 원칙 때문입니다.

---

## 1. 레고 블록처럼 갈아 끼우기 위해서 (유연성)

가장 큰 이유는 **"나중에 DB를 진짜로 붙일 때 Service 코드를 하나도 안 고치기 위해서"** 입니다.

**❌ 인터페이스가 없을 때 (기존 방식)**
```java
// Service는 "메모리 저장소"라는 구체적인 클래스에 꽉 묶여있습니다.
public class OrderService {
    private InMemoryOrderRepository repository = new InMemoryOrderRepository();
}
```
만약 회사가 커져서 이제 메모리가 아니라 **진짜 MySQL DB**에 저장해야 한다면?
`OrderService` 코드를 열어서 `new JpaOrderRepository()`로 직접 코드를 다 뜯어고쳐야 합니다. 비즈니스 로직(Service)은 가만히 있어야 하는데, 저장 방식이 바뀌었다고 Service까지 수정해야 하는 상황이 발생합니다.

**✅ 인터페이스로 분리했을 때 (리팩토링 방식)**
```java
// Service는 "저장소(Interface)"라는 추상적인 개념에만 의존합니다.
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```
Service는 `repository.save()`라는 기능이 있다는 것만 알 뿐, **그게 메모리에 저장되는지 MySQL에 저장되는지 모릅니다.** (관심도 없습니다!)
나중에 MySQL용 `JpaOrderRepository`를 새로 만들어서 주입(DI)만 해주면, **Service 코드는 단 한 줄도 수정할 필요가 없습니다!** 레고 블록 갈아 끼우듯 부품만 교체하면 됩니다.

---

## 2. 인터페이스와 구현체의 역할

### ① Interface (규약, 약속)
```java
public interface OrderRepository {
    void save(Order order);
    Order findById(Long id);
}
```
*   **역할:** "주문 저장소라면 무조건 `save`랑 `findById` 기능은 가지고 있어야 해!" 라는 **약속(메뉴판)**입니다.

### ② 구현체 (실제 동작)
```java
// 메모리에 저장하는 버전
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<Long, Order> store = new HashMap<>();
    
    @Override
    public void save(Order order) { store.put(order.getId(), order); }
    // ...
}

// 나중에 만들 진짜 DB 버전
public class JpaOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) { 
        // Jpa EntityManager를 써서 실제 DB에 Insert 하는 복잡한 로직
    }
    // ...
}
```
*   **역할:** 인터페이스의 약속을 **실제로 어떻게 수행할 것인지** 구체적인 방법을 적어둔 곳입니다.

---

## 3. 핵심 요약 (DIP 원칙)

이런 방식을 객체지향 설계 원칙 중 **의존성 역전 원칙(DIP, Dependency Inversion Principle)** 이라고 부릅니다.

> "고수준 모듈(Service)은 저수준 모듈(Repository 구현체)에 의존해서는 안 된다. 둘 다 추상화(Interface)에 의존해야 한다."

**결론:** Repository를 인터페이스와 `InMemory` 구현체로 나눈 것은, 당장 테스트용으로 메모리에 저장하되 **나중에 실제 DB로 갈아탈 때 Service 코드를 건드리지 않기 위한 아주 똑똑한 대비책**입니다!

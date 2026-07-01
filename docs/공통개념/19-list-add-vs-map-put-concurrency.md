# List.add()와 Map.put()의 차이 및 인메모리 저장소 설계

인메모리 DB를 구현하거나 컬렉션을 다룰 때 자주 사용하는 `List.add()`와 `Map.put()`의 내부 동작 차이 및 저장소 관점의 설계적 의미를 다룹니다.

---

## 1. List.add() vs Map.put() 동작 차이

가장 큰 차이는 **"중복 허용 여부"**와 **"수정(Update)의 처리 방식"**에 있습니다.

```
List (순서가 있는 목록) ➜ add() ➜ 항상 맨 뒤에 데이터를 추가 (중복 허용)
Map (Key-Value 쌍)     ➜ put() ➜ Key가 없으면 추가, 있으면 덮어쓰기 (중복 불가)
```

### ① List.add() — "무조건 추가 (Append)"
- **동작**: 리스트의 맨 끝에 새 원소를 삽입합니다.
- **중복**: 동일한 식별자(ID)를 가진 객체를 여러 번 `add`하면 리스트 내에 중복된 데이터가 그대로 다 들어갑니다.
- **문제점 (Repository 관점)**:
  배송지나 주문 상태를 변경한 후 `save(entity)`를 할 때 `list.add(entity)`를 수행하면 기존 데이터가 수정되는 것이 아니라, **중복 데이터가 리스트에 쌓이게 되어** 나중에 조회 시 어떤 데이터가 최신 상태인지 보장할 수 없습니다.

```java
List<Order> orders = new ArrayList<>();
orders.add(new Order(1L, "ACTIVE"));
orders.add(new Order(1L, "CANCELLED")); // 👈 중복 추가됨! orders.size() == 2
```

### ② Map.put(Key, Value) — "삽입 또는 수정 (Upsert)"
- **동작**: 지정한 Key로 Value를 맵에 저장합니다.
- **중복**: Map은 중복된 Key를 허용하지 않습니다.
- **수정**: 만약 이미 존재하는 Key로 `put`을 호출하면, 기존 Value를 새 Value로 **덮어씁니다(Update)**. 존재하지 않는 Key라면 새로 **삽입(Insert)**합니다.
- **장점 (Repository 관점)**:
  데이터의 유일성이 식별자(Key) 단위로 보장되므로, 데이터베이스의 기본키(PK) 제약조건과 유사하게 동작합니다.

```java
Map<Long, Order> orders = new HashMap<>();
orders.put(1L, new Order(1L, "ACTIVE"));
orders.put(1L, new Order(1L, "CANCELLED")); // 👈 1L Key의 데이터가 덮어써짐! orders.size() == 1
```

---

## 2. 인메모리 저장소(InMemory Repository) 설계 원칙

DB 연결 없이 가짜(Fake) 저장소를 구현해 테스트를 돌리거나 개발할 때, 아래 규칙을 지키는 것이 좋습니다.

### ① List 대신 Map을 사용하자
고유 식별자(PK)로 단건 조회가 잦은 저장소는 `List`를 순회하는 것보다 `Map`을 사용하는 것이 조회 성능(시간 복잡도 $O(1)$)과 중복 차단 관점에서 훨씬 유리합니다.

```java
// ✅ 올바른 인메모리 Repository 구현 예시
public class InMemoryOrderRepository {
    private final Map<Long, Order> store = new ConcurrentHashMap<>(); // 동시성 고려

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Order save(Order order) {
        store.put(order.id, order); // 존재하면 수정, 없으면 삽입 (정확한 Upsert 동작)
        return order;
    }
}
```

### ② 동시성 환경에서는 ConcurrentHashMap을 사용하자
일반 `HashMap`은 스레드 안전(Thread-Safe)하지 않습니다. 여러 스레드가 동시에 `put`을 호출하면 데이터가 유실되거나 무한 루프가 발생할 수 있으므로, 실무 및 멀티스레드 테스트 환경에서는 반드시 **`ConcurrentHashMap`**을 사용해야 합니다.

---

## 3. 면접 답변 템플릿

### Q. 레거시 코드의 save() 메서드에서 List.add()를 쓸 때의 문제점과 해결책은 무엇인가요?

> "수정(Update) 유스케이스가 포함된 저장 로직에서 `List.add()`를 사용하면 기존 데이터가 교체되는 것이 아니라 동일한 식별자의 데이터가 중복해서 계속 쌓이는 버그가 발생합니다.
> 
> 데이터베이스의 기본키(PK) 유일성 제약조건처럼 동작하도록 인메모리 저장소를 설계하려면, `List` 대신 식별자를 Key로 하는 `Map` 구조를 사용하고 **`Map.put(id, entity)`** 방식을 취해야 합니다. 이렇게 하면 Key가 이미 존재할 때는 자동으로 최신 객체로 덮어씌워져(Update) 정합성이 유지됩니다."

---

## 4. 006번 Order Cancel Repository 예시

### 기존 코드의 문제점 (`OrderRepository.java`)

```java
public class OrderRepository {
    private static final List<Order> orders = new ArrayList<>();

    public void save(Order order) {
        orders.add(order); // 👈 수정 유스케이스에서도 매번 add만 수행!
    }
}
```

주문 취소 시 기존 주문 정보를 찾아서 `status`를 `CANCELLED`로 변경한 뒤 `save(order)`를 호출합니다. 이때 `ArrayList.add()`를 사용하면, 기존 `ACTIVE` 상태의 주문 객체가 리스트에 남아있고 `CANCELLED` 상태의 동일한 ID를 가진 새 주문 객체가 뒤에 하나 더 추가됩니다. 

결과적으로 같은 ID의 주문이 여러 개 존재하게 되고, 다음 번 단건 조회(`findById`) 시 리스트를 순회하며 처음 발견된 `ACTIVE` 상태의 객체를 반환하게 되어 취소 상태가 정상적으로 보이지 않는 데이터 부정합 버그가 일어납니다.

### 개선 방향

인메모리 저장소를 `Map<Long, Order>` 구조로 전환하고, `save` 메서드는 `put`을 이용하여 동일 ID 발생 시 데이터를 덮어쓰도록(Update) 유도합니다.

```java
public class OrderRepository {
    private static final Map<Long, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.id, order); // 덮어쓰기 방식으로 저장 및 수정 해결
    }
}
```

# Optional과 NPE 방지 전략

## 1. Optional이란?

`Optional`은 **"값이 있을 수도 있고, 없을 수도 있다"는 것을 명시적으로 표현하는 포장 박스**입니다.
Java 8에서 도입되었으며, `null`을 직접 반환하는 것보다 훨씬 안전하게 "값 없음"을 표현할 수 있습니다.

---

## 2. null 반환의 문제점

```java
// ❌ null을 직접 반환하는 Repository
public PointPayment findById(Long id) {
    return null; // 못 찾으면 null 반환
}

// ❌ Service에서 null 체크를 깜빡하는 순간 NPE 발생!
PointPayment payment = repository.findById(1L);
payment.status; // 💥 NullPointerException!
```

- `null`은 받는 쪽이 체크를 깜빡할 수 있어 매우 위험합니다.
- 컴파일 시점에 에러가 나지 않고 **런타임에 NPE로 터집니다.** (프로덕션 장애!)

---

## 3. Optional을 활용한 안전한 설계

```java
// ✅ Optional로 "없을 수 있다"는 것을 타입으로 명시!
public Optional<PointPayment> findById(Long id) {
    return payments.stream()
        .filter(p -> p.id.equals(id))
        .findFirst(); // 찾으면 Optional.of(값), 못 찾으면 Optional.empty() 반환
}

// ✅ Service에서 Optional을 받으면, 타입 자체가 "없을 수 있어!"라고 알려줌
Optional<PointPayment> result = repository.findById(1L);

// 없으면 의미있는 도메인 예외를 던짐 (NPE 대신!)
PointPayment payment = result
    .orElseThrow(() -> new PointRefundException("결제 정보를 찾을 수 없습니다"));
```

### Optional 주요 메서드
| 메서드 | 설명 |
|--------|------|
| `orElseThrow(예외)` | 값이 없으면 지정한 예외를 던짐 (가장 많이 씀) |
| `orElse(기본값)` | 값이 없으면 기본값을 반환 |
| `isPresent()` | 값이 있으면 true, 없으면 false |
| `ifPresent(람다)` | 값이 있을 때만 람다 실행 |

---

## 4. `static final List`의 문제점과 해결 방법

### 문제점 정리
| 문제 | 내용 |
|------|------|
| **동시성** | 여러 요청이 동시에 `add/remove`하면 데이터가 꼬임 (`ArrayList`는 Thread-Unsafe) |
| **테스트 격리 깨짐** | 테스트 A에서 추가한 데이터가 테스트 B에도 그대로 남아있음 |
| **데이터 영속성 없음** | 서버 재시작하면 모든 데이터 사라짐 (DB 역할 불가) |

### 해결 방법별 비교

**① 근본적 해결 (실무): 실제 DB + JPA Repository 사용**
```java
// ✅ Spring Data JPA Repository 인터페이스 사용
public interface PointPaymentRepository extends JpaRepository<PointPayment, Long> {
    // save(), findById(), findAll() 등 모든 CRUD가 자동으로 제공됨!
    // 트랜잭션, 동시성, 데이터 영속성을 JPA와 DB가 보장함
}
```

**② 테스트/임시 코드에서 동시성만 해결: `CopyOnWriteArrayList` 사용**
```java
// static List를 쓸 수밖에 없는 상황이라면, 동시성이 보장되는 컬렉션으로 교체
private static final List<PointPayment> payments = new CopyOnWriteArrayList<>();
```

**③ key-value 구조로 관리: `ConcurrentHashMap` 사용**
```java
// id로 빠르게 찾아야 할 때 Map이 더 효율적
private static final Map<Long, PointPayment> store = new ConcurrentHashMap<>();

public Optional<PointPayment> findById(Long id) {
    return Optional.ofNullable(store.get(id)); // null이면 Optional.empty() 반환
}
```

---

## 5. 핵심 요약

> **`null` 반환 대신 `Optional` 반환** → 받는 쪽이 "없을 수 있다"는 것을 타입으로 강제 인식, NPE 예방
>
> **`static List` 대신 JPA Repository** → 동시성, 데이터 영속성, 테스트 격리 문제를 한 번에 해결
>
> `static List`는 학습/프로토타이핑용이지, 실무에서는 반드시 실제 DB로 대체해야 합니다.

# 동시성(Concurrency)과 Thread-Safe

## 1. 왜 동시성 문제가 발생하는가?

- JVM 웹 애플리케이션(Spring Boot 등)은 **멀티스레드 환경**에서 동작합니다. HTTP 요청마다 하나의 스레드가 할당됩니다.
- 스레드들은 각자의 **스택(Stack) 영역**을 가지지만, **힙(Heap)과 메서드(Static) 영역은 모든 스레드가 공유**합니다.
- 여러 스레드가 공유 메모리에 **동시에 쓰기(Write)**를 할 때, 동기화 처리가 없으면 데이터가 꼬이거나 유실됩니다.

```
스레드 1: ORDERS.add(orderA)  ──┐
                                 ├── 동시에 실행! → 💥 데이터 유실 / 예외 발생
스레드 2: ORDERS.add(orderB)  ──┘
```

---

## 2. `ArrayList`가 Thread-Safe하지 않은 이유

`ArrayList`는 내부적으로 동기화(Synchronization) 처리가 되어 있지 않습니다.

```java
// ArrayList.add()의 개념적 흐름
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // 1. 공간 확인
    elementData[size++] = e;           // 2. 데이터 쓰기 & size 1 증가
    return true;
}
```

**문제 시나리오**:
1. 스레드 A가 `size = 5` 확인
2. 스레드 B도 동시에 `size = 5` 확인
3. 스레드 A가 `elementData[5]`에 값을 씀
4. 스레드 B도 동일한 `elementData[5]`에 덮어씀 → **스레드 A 데이터 유실** 💥
5. `size`가 정상 증가하지 않거나 `ArrayIndexOutOfBoundsException` 발생 가능

---

## 3. Thread-Safe한 대체 컬렉션

| 컬렉션 | 특징 | 적합한 상황 |
|--------|------|------------|
| `Collections.synchronizedList()` | 모든 메서드에 `synchronized` 적용 | 읽기/쓰기 모두 있을 때 |
| `CopyOnWriteArrayList` | 쓰기 시 배열 복사본 생성 | 읽기가 압도적으로 많을 때 |
| `ConcurrentHashMap` | Lock Striping으로 영역별 동기화 | Map이 필요할 때 |

```java
// 임시 해결
private static final List<Order> ORDERS = Collections.synchronizedList(new ArrayList<>());

// 근본 해결: static 자체를 없애고 실제 DB 사용
```

---

## 4. 핵심 요약

> `ArrayList`는 단일 스레드 환경에서만 안전하다.
> 서버처럼 멀티스레드 환경에서는 Thread-Safe 컬렉션을 쓰거나, 근본적으로 실제 DB + 트랜잭션을 활용해야 한다.

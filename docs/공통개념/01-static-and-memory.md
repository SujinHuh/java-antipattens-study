# Java `static` 키워드와 메모리 구조

## 1. `static`의 기본 정의

- `static` 키워드가 붙은 멤버(필드, 메서드)는 **클래스 멤버** 또는 **정적 멤버**라고 부릅니다.
- 객체(인스턴스)가 생성될 때마다 메모리에 개별적으로 생성되는 인스턴스 변수와 달리, **클래스가 JVM에 로딩되는 시점에 단 한 번 메모리에 할당**됩니다.
- 생성된 모든 인스턴스가 이 메모리 공간을 **공유**합니다.

```
[ Heap Area (힙 영역) ]                  [ Method/Static Area (메서드 영역) ]
┌──────────────────────────────┐        ┌──────────────────────────────┐
│  repo1 (Instance)            │        │  OrderRepository Class       │
│  └─ (인스턴스 필드들)          │        │                              │
├──────────────────────────────┤        │  └─ ORDERS (static)          │
│  repo2 (Instance)            │        │     [Order1, Order2, ...]    │
│  └─ (인스턴스 필드들)          │        │               ▲              │
└──────────────────────────────┘        └───────────────┼──────────────┘
               │                                        │
               └─────────────── 공유/참조 ───────────────┘
```

---

## 2. `static final`의 특징과 오해

- **`final`**: 한 번 할당된 **참조 주소값**을 변경할 수 없게 만듭니다.
- `static final List<Order> ORDERS = new ArrayList<>()`로 선언하는 경우:
  - `ORDERS = new ArrayList<>()` 처럼 다른 리스트 객체로 **참조를 변경(재할당)하는 것은 불가능**합니다.
  - 하지만 `add()`, `remove()`, `clear()` 등의 **내부 내용물 변경은 얼마든지 가능**합니다.
  - 따라서 참조는 상수이지만, **담고 있는 데이터는 전역적으로 변경 가능한 공유 상태**가 됩니다.

```java
private static final List<Order> ORDERS = new ArrayList<>();

ORDERS = new ArrayList<>();   // ❌ 컴파일 에러 — final이라 재할당 불가
ORDERS.add(order);            // ✅ 가능 — 내부 데이터 변경은 허용됨
ORDERS.clear();               // ✅ 가능
```

---

## 3. static을 DB처럼 쓰면 생기는 문제

### 문제 1: 테스트 간 데이터 공유 (테스트 격리 깨짐)

JVM이 살아있는 동안 `static` 변수는 초기화되지 않으므로, 테스트 A가 저장한 데이터가 테스트 B에 그대로 남습니다.

```
[테스트 A 실행]
  repo.save(order1)
  → ORDERS = [order1]  ← JVM 메모리에 남아있음

[테스트 B 실행] (A 직후)
  → ORDERS = [order1, ...]  ← 테스트 A 데이터가 그대로! ❌
```

```java
@Test
void testSaveOrder() {
    repo.save(order1);
    assertThat(ORDERS.size()).isEqualTo(1);  // ✅ 통과
}

@Test
void testEmptyOrders() {
    assertThat(ORDERS.size()).isEqualTo(0);  // ❌ 실패! order1이 남아있음
}
```

**해결**: `@BeforeEach`에서 `ORDERS.clear()` 명시 호출, 또는 실제 DB + `@Transactional` 롤백 활용.

### 문제 2: 동시성 취약

→ [02-concurrency.md](02-concurrency.md) 참고

---

## 4. 핵심 요약

> `static final List`는 JVM이 살아있는 한 **모든 객체가 공유하는 전역 변수**처럼 동작한다.
> `final`이 붙어도 리스트 **내부 데이터는 변경 가능**하므로 전역 변경 공유 상태가 된다.

# Spring AOP와 프록시 패턴 (`@Transactional` 동작 원리)

## 1. Spring이 `@Transactional`을 구현하는 방법 — 프록시 패턴

`@Transactional`은 Spring이 직접 코드를 수정하는 게 아니라, **프록시(Proxy) 객체**를 중간에 끼워넣는 방식으로 동작합니다.
Spring은 `@Transactional`이 붙은 Bean을 등록할 때 원본 객체를 그대로 주는 것이 아니라, **원본을 감싸는 프록시 객체를 대신 만들어서** 다른 Bean에 주입합니다.

```
내가 만든 코드:
  ReservationService (원본)
    ├── reserve()
    └── saveReservation()

Spring이 실제로 주입해주는 것:
  ReservationServiceProxy (프록시) ← Spring이 자동으로 생성
    ├── reserve()         → [트랜잭션 시작] → 원본.reserve() → [커밋 or 롤백]
    └── saveReservation() → [트랜잭션 시작] → 원본.saveReservation() → [커밋 or 롤백]
```

---

## 2. 정상 동작 — 외부에서 호출하는 경우

```
Controller (외부)
    │
    │  reservationService.reserve(request)
    │  (주입받은 것은 프록시 객체!)
    ▼
┌──────────────────────────────────────────┐
│  ReservationServiceProxy                 │
│  1. 트랜잭션 시작 (BEGIN)                 │
│  2. 원본 reserve() 실행                  │
│  3. 성공 → 커밋 / 예외 → 롤백           │
└──────────────────────────────────────────┘
    │
    ▼
  원본 ReservationService.reserve() 실행  ✅ 트랜잭션 정상 적용
```

---

## 3. Self-Invocation — 같은 클래스 내부에서 호출하는 경우

```java
// ReservationService.java
@Transactional
public String reserve(...) {
    // ...
    saveReservation(reservation); // ← this.saveReservation() 과 동일
                                  //   프록시를 거치지 않고 자신을 직접 호출!
}

@Transactional  // ← 이 어노테이션은 이 호출 경로에서 무시됨
public void saveReservation(Reservation reservation) { ... }
```

```
외부 호출 경로:
  Controller → [Proxy] → 원본.reserve()
                               │
                               │  saveReservation() 직접 호출 (this.)
                               ▼
                          원본.saveReservation()  ← ❌ 프록시 없음! 트랜잭션 미적용
```

**왜 프록시를 안 거치는가?**
- 프록시는 원본 객체 **바깥**을 감싸고 있는 별도의 객체입니다.
- `this.saveReservation()`은 **원본이 원본 자신을 직접 부르는 것**이므로, 바깥의 프록시는 개입할 수 없습니다.

---

## 4. 비유로 이해하기

```
프록시 = 콜센터 교환원 (트랜잭션 처리)
원본  = 실제 담당자

정상 흐름:
  고객(Controller)
    → 콜센터(Proxy)에 전화
    → 교환원이 통화 기록 시작(트랜잭션 시작)
    → 담당자(원본)에게 연결
    → 통화 종료 후 교환원이 기록 마감(커밋)

self-invocation:
  담당자(원본)가 일하는 도중
    → 같은 부서 동료(saveReservation)에게 직접 내선 전화
    → 이 통화는 콜센터(Proxy)를 거치지 않음
    → 교환원(Proxy)은 이 통화를 모름 → 트랜잭션 없이 실행됨 💥
```

---

## 5. 결과 비교

| | 외부 호출 (정상) | Self-Invocation (문제) |
|--|----------------|----------------------|
| 호출 경로 | Controller → **프록시** → 원본 | 원본 내부 → **직접** 원본 메서드 |
| 프록시 개입 | ✅ 있음 | ❌ 없음 |
| `@Transactional` 적용 | ✅ 됨 | ❌ 안됨 |
| 실패 시 롤백 | ✅ 됨 | ❌ 안됨 → 데이터 반쪽만 저장 위험 |

---

## 6. 해결 방법

```java
// 방법 1: self-invocation 로직을 별도 Bean으로 분리
@Service
public class ReservationSaver {
    @Transactional
    public void save(Reservation reservation) {
        reservationRepository.save(reservation); // 외부 Bean → 프록시 정상 동작
    }
}

// 방법 2: saveReservation()을 없애고 reserve() 안에서 직접 repository 호출
@Transactional
public String reserve(ReservationRequest request) {
    // ...
    reservationRepository.save(reservation); // reserve()의 트랜잭션 안에서 실행됨
}
```

---

## 7. 문항 007번 자가 호출 (Self-Invocation) 분석 케이스

### ❌ 문제 코드 상황
`issue` 메서드(트랜잭션 없음) 내부에서 같은 클래스의 `@Transactional`이 선언된 `issueInternal`을 직접 호출하고 있습니다.

```java
public class StockCouponService {
    public StockCouponResponse issue(StockCouponRequest request) {
        // ...
        return issueInternal(request); // 👈 자가 호출 (this.issueInternal()과 동일)
    }

    @Transactional
    public StockCouponResponse issueInternal(StockCouponRequest request) {
        // ...
    }
}
```

### 💥 발생하는 실제 문제
* 외부에서 `service.issue()`를 호출하면 프록시 객체가 아닌 원본 객체의 `issue()`가 먼저 실행됩니다.
* `issue()` 내부에서 `issueInternal()`을 직접 호출하면 프록시를 통하지 않고 원본 객체의 메서드를 바로 호출하게 되므로, `@Transactional` 어노테이션이 붙어 있어도 트랜잭션이 동작하지 않습니다.
* 결과적으로 모든 DB 작업이 개별 트랜잭션으로 처리되어 발급 중 오류가 발생하더라도 이전 변경 사항이 롤백되지 않는 데이터 정합성 장애를 유발합니다.

---

## 8. 핵심 요약

> `@Transactional`은 Spring 프록시가 감싸줄 때만 동작한다.
> 같은 클래스 안에서 `this.메서드()`를 직접 부르면 프록시를 건너뛰어 트랜잭션이 적용되지 않는다.
> 이를 **Self-Invocation 문제**라고 한다.

# 의존성 주입 (Dependency Injection, DI)

## 1. `new`로 직접 생성하는 방식의 문제

```java
// ❌ 안티패턴 — 직접 생성
public class ReservationController {
    private final ReservationService reservationService = new ReservationService();
}

public class ReservationService {
    private final ReservationRepository reservationRepository = new ReservationRepository();
}
```

`new`로 직접 생성하면 **Controller가 어떤 Service를 쓸지 스스로 결정**해버립니다.
이렇게 되면 두 클래스가 **강하게 묶여(강결합, Tight Coupling)** 버립니다.

```
강결합 구조:
  ReservationController
    └── new ReservationService()      ← Controller가 직접 만듦
          └── new ReservationRepository()  ← Service가 직접 만듦
                └── (DB 연결 필요)
```

### 발생하는 문제들

**문제 1: 단위 테스트가 불가능**
```
"Controller만 테스트하고 싶어"
  → new ReservationService()가 자동으로 생성됨
  → new ReservationRepository()까지 생성됨
  → DB 연결까지 필요해짐 😱
  → 가짜 객체(Mock)를 끼워넣을 방법이 없음
```

**문제 2: 구현체 교체가 어려움**
```
ReservationRepository를 DB용 → Redis용으로 바꾸고 싶다면?
  → Service 코드 안에 직접 들어가서 new를 수정해야 함
  → Service 코드를 건드려야 하니 버그 발생 위험 ↑
```

---

## 2. 의존성 주입(DI)이란?

**"내가 직접 만들지 않고, 외부에서 만들어서 줘"** 라는 패턴입니다.
Spring에서는 Spring 컨테이너가 객체를 만들어서 필요한 곳에 주입해줍니다.

```java
// ✅ 의존성 주입 — 생성자 주입 방식 (Spring 권장)
@RestController
public class ReservationController {

    private final ReservationService reservationService;

    // Controller는 Service를 직접 만들지 않음
    // Spring이 만든 Service 객체를 생성자로 전달받음
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
}

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }
}
```

```
DI 구조:
  Spring 컨테이너
    ├── ReservationRepository 객체 생성
    ├── ReservationService 생성 → Repository 주입
    └── ReservationController 생성 → Service 주입
```

---

## 3. DI의 장점

### 장점 1: 테스트 시 가짜 객체(Mock) 주입 가능

```java
// 테스트 코드에서
ReservationRepository mockRepo = mock(ReservationRepository.class);
ReservationService service = new ReservationService(mockRepo); // 가짜 Repository 주입
// → DB 없이도 Service 로직만 테스트 가능! ✅
```

### 장점 2: 구현체 교체가 쉬움

```java
// 인터페이스로 추상화
public interface ReservationRepository {
    void save(Reservation r);
    Reservation findById(long id);
}

// 구현체 1: DB 저장
@Repository
public class DbReservationRepository implements ReservationRepository { ... }

// 구현체 2: Redis 저장
@Repository
public class RedisReservationRepository implements ReservationRepository { ... }

// Service는 인터페이스만 알면 됨 → 구현체가 바뀌어도 Service 코드 변경 없음
@Service
public class ReservationService {
    private final ReservationRepository repository; // 어떤 구현체든 상관없음
}
```

---

## 4. `new` 직접 생성 vs DI 비교

| | `new` 직접 생성 | 의존성 주입 (DI) |
|--|----------------|----------------|
| 결합도 | 강결합 (변경 어려움) | 느슨한 결합 (유연함) |
| 단위 테스트 | ❌ Mock 주입 불가 | ✅ Mock 주입 가능 |
| 구현체 교체 | ❌ 코드 직접 수정 필요 | ✅ 주입만 바꾸면 됨 |
| Spring AOP 적용 | ❌ 프록시 미적용 | ✅ 프록시 정상 적용 |

> `new`로 직접 생성하면 Spring의 `@Transactional` 같은 AOP 기능도 적용되지 않습니다.
> Spring이 관리하는 Bean이어야만 프록시를 통해 AOP가 동작하기 때문입니다.

---

## 5. 핵심 요약

> `new`로 직접 생성하면 두 클래스가 강하게 묶여 테스트도 어렵고 변경도 어렵다.
> 의존성 주입은 **"내가 만들지 않고, 외부(Spring)에서 만들어서 줘"** 라는 방식으로
> 결합도를 낮추고 테스트 가능성과 유연성을 높인다.

# 계층별 책임(SRP) — Controller / Service / Repository

## 1. 각 계층의 역할

```
[Controller]
  - HTTP 요청 수신 (URL 매핑, @RequestBody, @PathVariable 등)
  - 입력값을 DTO로 파싱하고 기본 포맷 검증 (@Valid)
  - Service 호출 후 결과를 HTTP 응답(@ResponseBody)으로 반환
  - ❌ 비즈니스 로직 금지
  - ❌ DB 직접 접근 금지

[Service]
  - 핵심 비즈니스 로직 수행 (할인 계산, 상태 변경, 유효성 판단 등)
  - Request DTO → Entity 변환
  - 트랜잭션 경계 설정 (@Transactional)
  - Repository를 호출해 데이터 저장/조회
  - ❌ HTTP 관련 코드 금지 (HttpServletRequest, ResponseEntity 등)

[Repository]
  - 데이터 저장소 접근 전담 (save, findById, delete, exists 등)
  - SQL 또는 ORM(JPA)을 통해 DB와 직접 통신
  - ❌ 비즈니스 판단 금지
  - ❌ 다른 Service 호출 금지
```

---

## 2. 책임 위반 예시

### ❌ Repository가 비즈니스 판단을 하는 경우

```java
// ReservationRepository.java
public void save(Reservation reservation) {
    // "CANCELLED 상태이고 시간이 잘못됐으면 예외" → 비즈니스 규칙!
    // Repository에 있으면 안 됨!
    if ("CANCELLED".equals(reservation.status) && reservation.endHour <= reservation.startHour) {
        throw new ReservationException("invalid");  // ← 여기 있으면 안됨
    }
    RESERVATIONS.add(reservation);
}
```

### ✅ 올바른 위치 — Service가 비즈니스 판단

```java
// ReservationService.java
public String reserve(ReservationRequest request) {
    // 시간 유효성은 Service가 판단
    if (request.endHour <= request.startHour) {
        throw new InvalidReservationTimeException(
            "예약 시간 오류: startHour=" + request.startHour + ", endHour=" + request.endHour
        );
    }
    // ...
    reservationRepository.save(reservation); // Repository는 저장만
}

// ReservationRepository.java
public void save(Reservation reservation) {
    RESERVATIONS.add(reservation); // 판단 없이 순수하게 저장만
}
```

---

## 3. 책임 위반이 생기는 이유와 결과

| 위반 패턴 | 생기는 문제 |
|----------|-----------|
| Repository에 비즈니스 판단 | 같은 검증이 Service/Repository에 중복됨, 규칙 변경 시 두 곳 수정 필요 |
| Controller에 비즈니스 로직 | 테스트 시 HTTP 환경 없이는 로직 검증 불가 |
| Service에서 HTTP 객체 사용 | Service가 웹 레이어에 종속되어 재사용 불가 |
| Service가 Entity 필드를 직접 여기저기 수정 | 도메인 규칙이 흩어져 재고 음수, 상태 불일치 같은 버그가 생김 |
| Service가 외부 이벤트 발행까지 한 try-catch로 삼킴 | DB 저장 결과와 외부 이벤트 상태가 어긋나고 롤백 신호가 사라질 수 있음 |

---

## 4. 007번 Stock Coupon Service에서 봐야 할 책임 경계

Service는 비즈니스 흐름을 조율하는 계층이지만, 모든 세부 규칙을 긴 절차 코드로 직접 풀어쓰면 유지보수가 어려워집니다.

```java
if (coupon.remainingQuantity <= 0) {
    throw new StockCouponException("sold out");
}

int quantity = request.requestedQuantity <= 0 ? 1 : request.requestedQuantity;
coupon.remainingQuantity = coupon.remainingQuantity - quantity;
coupon.version = coupon.version + 1;
```

이 코드는 Service가 재고 검증, 기본 수량 보정, 재고 차감, 버전 증가를 전부 직접 처리합니다.
처음에는 단순해 보이지만 `requestedQuantity > remainingQuantity`, 동시 요청, 버전 충돌 같은 조건이 늘어나면 규칙이 흩어집니다.

더 나은 방향은 Entity나 도메인 메서드에 핵심 상태 변경을 모으는 것입니다.

```java
public class StockCoupon {
    public void issue(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException();
        }
        if (remainingQuantity < quantity) {
            throw new SoldOutException();
        }
        this.remainingQuantity -= quantity;
    }
}
```

Service는 아래처럼 흐름을 조율합니다.

```java
StockCoupon coupon = stockCouponRepository.findByIdForUpdate(request.couponId())
    .orElseThrow(CouponNotFoundException::new);

coupon.issue(request.requestedQuantity());
issueHistoryRepository.save(CouponIssueHistory.of(coupon, request.userId(), request.idempotencyKey()));
```

면접용 문장:

```text
Service는 비즈니스 흐름을 조율하되, Entity의 핵심 상태 변경 규칙까지 직접 흩뿌리면 안 됩니다.
재고 검증과 차감 같은 불변식은 도메인 메서드로 모으고, Service는 트랜잭션 경계와 Repository 호출 흐름을 담당하게 하겠습니다.
```

---

## 5. 계층 책임을 지켜야 하는 이유

| 이유 | 설명 |
|------|------|
| **테스트 용이성** | 각 계층을 독립적으로 단위 테스트 가능 |
| **변경 영향 최소화** | 비즈니스 규칙이 바뀌어도 Service만 수정 |
| **코드 추적 용이** | "비즈니스 로직은 Service" 약속이 지켜져야 찾기 쉬움 |
| **중복 방지** | 같은 검증이 여러 계층에 흩어지는 것을 막음 |

---

## 6. 핵심 요약

> Controller는 요청/응답 변환만, Service는 비즈니스 로직만, Repository는 저장소 접근만.
> 이 경계가 무너지면 테스트도 어렵고 변경도 어렵고 원인 추적도 어려워진다.

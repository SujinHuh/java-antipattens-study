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

---

## 4. 계층 책임을 지켜야 하는 이유

| 이유 | 설명 |
|------|------|
| **테스트 용이성** | 각 계층을 독립적으로 단위 테스트 가능 |
| **변경 영향 최소화** | 비즈니스 규칙이 바뀌어도 Service만 수정 |
| **코드 추적 용이** | "비즈니스 로직은 Service" 약속이 지켜져야 찾기 쉬움 |
| **중복 방지** | 같은 검증이 여러 계층에 흩어지는 것을 막음 |

---

## 5. 핵심 요약

> Controller는 요청/응답 변환만, Service는 비즈니스 로직만, Repository는 저장소 접근만.
> 이 경계가 무너지면 테스트도 어렵고 변경도 어렵고 원인 추적도 어려워진다.

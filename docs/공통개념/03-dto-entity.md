# DTO vs Entity — 역할과 책임의 분리

## 1. Entity란?

**DB 테이블의 한 행(Row)을 자바 객체로 표현한 클래스**입니다.
Spring Data JPA 환경에서는 `@Entity` 어노테이션이 붙으며, DB와 직접 매핑됩니다.

```java
// Entity: DB 테이블과 1:1 대응
public class Reservation {
    public long id;          // DB PK — 서버/DB가 생성
    public long userId;
    public long roomId;
    public int startHour;
    public int endHour;
    public String status;    // 비즈니스 로직이 결정 — 서버 전용
    public String createdAt; // 서버가 현재 시각으로 채움 — 서버 전용
}
```

---

## 2. DTO(Data Transfer Object)란?

**계층 간에 데이터를 운반하기 위한 단순한 그릇(Carrier) 객체**입니다.
비즈니스 로직이 없고, 순수하게 데이터를 담아 이동시키는 역할만 합니다.

- **Request DTO**: 클라이언트 → 서버 방향 입력값
- **Response DTO**: 서버 → 클라이언트 방향 결과값

면접 직전 암기 문장:

> Request DTO는 "클라이언트가 무엇을 해달라고 요청하는지"를 담는다.
> Response DTO는 "서버가 처리한 결과를 어떻게 보여줄지"를 담는다.
> 요청 DTO를 그대로 응답으로 돌려주는 것이 아니라, 서버 처리 결과를 응답 DTO로 새로 구성한다.

```java
// Request DTO: 클라이언트가 보내는 '의도(Intent)'만 담아야 함
public class ReservationRequest {
    public long userId;
    public long roomId;
    public int startHour;
    public int endHour;
    // status   → ❌ 없어야 함! 서버 비즈니스 로직이 결정
    // createdAt → ❌ 없어야 함! 서버가 현재 시각으로 채움
    // id       → ❌ 없어야 함! DB가 생성
}
```

---

## 3. 어느 필드가 어디 있어야 하는가?

> **핵심 질문: "이 필드, 클라이언트가 결정하는가? 서버가 결정하는가?"**

| 필드 | 누가 결정? | 있어야 할 곳 |
|------|-----------|------------|
| `userId`, `roomId` | 클라이언트 (어떤 방을 예약할지) | Request DTO ✅ |
| `startHour`, `endHour` | 클라이언트 (몇 시에 할지) | Request DTO ✅ |
| `id` | 서버/DB (자동 생성) | Entity ✅, Request DTO ❌ |
| `status` | 서버 비즈니스 로직 ("CONFIRMED" 등) | Entity ✅, Request DTO ❌ |
| `createdAt` | 서버 (`LocalDateTime.now()`) | Entity ✅, Request DTO ❌ |

---

## 4. Entity를 직접 외부에 노출하면 안 되는 이유

1. **보안**: `password`, `internalScore` 등 노출되면 안 되는 필드가 포함될 수 있음
2. **결합도**: API 응답 포맷이 DB 스키마에 종속됨. DB 컬럼 하나 바꾸면 API 스펙도 바뀜
3. **조작 위험**: 클라이언트가 `status`, `createdAt` 같은 서버 전용 필드를 직접 채워서 보낼 수 있음

---

## 5. 클라이언트 신뢰 문제 (Client Trust Problem)

클라이언트가 보내는 값은 **언제든 위조 가능**합니다.

```
정상 요청:
  { itemCount: 10, pricePerItem: 1000 } → 서버가 9000원으로 계산

악의적 조작:
  { itemCount: 10, pricePerItem: 1000, finalPrice: 1 } → "나 1원에 샀어!" 💥
```

→ 금액, 상태, 등급처럼 **비즈니스에 영향을 주는 값은 반드시 서버에서 계산**해야 합니다.

---

## 6. 올바른 계층별 변환 흐름

```
[클라이언트]
    │  Request DTO (userId, roomId, startHour, endHour 만)
    ▼
[Controller]  → Request DTO를 Service로 전달
    ▼
[Service]     → DTO를 Entity로 변환
    │           reservation.status   = "CONFIRMED"          // 서버가 결정
    │           reservation.createdAt = LocalDateTime.now() // 서버가 채움
    │           reservation.id        = generateId()        // 서버가 생성
    ▼
[Repository]  → Entity를 DB에 저장
    ▼
[Service]     → 결과를 Response DTO로 변환
    ▼
[Controller]  → Response DTO를 클라이언트에 반환
    ▼
[클라이언트]  Response DTO (id, status, createdAt 포함)
```

---

## 7. 핵심 요약

> Request DTO에는 **클라이언트가 의도를 전달하는 값**만 담는다.
> 서버가 계산하거나 생성해야 하는 값(`id`, `status`, `createdAt`)은 절대 Request DTO에 넣지 않는다.
> Entity는 DB 저장용이므로 직접 외부에 노출하지 않는다.

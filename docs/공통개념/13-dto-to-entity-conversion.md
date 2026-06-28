# DTO → Entity 변환은 어디서 해야 할까?

Controller에서 DTO를 받아서 Service에 넘길 때, 변환을 어느 계층에서 해야 하는지 헷갈리기 쉬운 포인트입니다.

---

## 1. 자주 하는 오해

> "Controller가 DTO를 받았으니, Controller에서 Entity로 변환해서 Service에 넘겨야 하는 거 아닌가?"

**❌ 아닙니다.** Controller는 DTO를 그대로 Service에 넘기고, 변환은 Service가 담당합니다.

---

## 2. 올바른 흐름

```
클라이언트
    ↓ (JSON)
Controller  → DTO를 그대로 Service로 전달 (변환 X)
    ↓ (DTO)
Service     → DTO를 받아서 Entity로 변환, 비즈니스 로직 처리
    ↓ (Entity)
Repository  → Entity를 DB에 저장
```

---

## 3. 왜 Controller가 아니라 Service에서 변환하는가?

### 계층별 역할(책임) 때문입니다.

| 계층 | 역할 | Entity 변환 여부 |
|------|------|----------------|
| **Controller** | 웹 요청을 받아서 Service에게 전달 | ❌ 알 필요 없음 |
| **Service** | 비즈니스 로직 처리. "이 데이터로 어떤 Entity를 만들어야 하는가?"를 판단 | ✅ 여기서 변환 |
| **Repository** | DB에 저장/조회 | Entity를 그대로 받아서 처리 |

만약 Controller에서 Entity를 만들어버리면?
- Controller가 DB 구조(Entity)를 알아야 하므로, **웹 계층과 DB 계층이 강하게 결합**됩니다.
- Entity 구조가 바뀌면 Controller도 함께 수정해야 하는 번거로움이 생깁니다.

---

## 4. 코드 예시

```java
// ✅ 올바른 흐름

// Controller: DTO를 받아서 그대로 Service에 전달. Entity 변환 X!
@RestController
public class PointRefundController {

    private final PointRefundService service;

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@RequestBody PointRefundRequest request) {
        service.refund(request); // DTO 그대로 전달
        return ResponseEntity.ok().build();
    }
}

// Service: 여기서 DTO → Entity 변환 담당!
@Service
@Transactional
public class PointRefundService {

    public void refund(PointRefundRequest request) {
        // 비즈니스 판단: 이 DTO로 어떤 Entity를 만들어야 하는가?
        PointPayment payment = PointPayment.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .build(); // Entity 완성!

        repository.save(payment); // Repository에 Entity를 넘겨 저장
    }
}
```

---

## 5. 핵심 요약

> **Controller = DTO를 받아서 Service에 '그대로' 전달하는 택배 기사**
> **Service = DTO를 받아서 Entity로 변환 후 비즈니스 로직을 처리하는 주체**
>
> DTO → Entity 변환의 책임은 **비즈니스 로직의 일부**이기 때문에, 항상 **Service** 계층에 있어야 합니다.

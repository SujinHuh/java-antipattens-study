# `@Builder` 패턴이란? (빌더 패턴)

객체를 생성할 때 `new` 키워드와 생성자를 직접 쓰지 않고, **레고 블록을 조립하듯이 필요한 데이터만 쏙쏙 골라서 객체를 만드는 방법**입니다. Java 개발, 특히 Spring Boot Entity 클래스에서 아주 널리 쓰이는 표준적인 방법입니다.

---

## 1. 생성자 방식의 한계 (왜 빌더가 필요한가?)

만약 `User` 객체를 만들 때, 기존 생성자 방식을 쓰면 이런 문제가 생깁니다.

```java
// User 생성자: id, name, age, address, phone, createdAt
User user = new User(null, "수진", 25, "서울", null, null);
```

**[문제점]**
1. **순서 헷갈림**: 3번째가 나이인지 주소인지 외워야 합니다. 만약 실수로 `new User(..., "010-1111", "서울", ...)` 처럼 순서를 바꾸면 에러도 안 나고 DB에 엉뚱하게 들어갑니다.
2. **불필요한 null 강제**: 지금 당장 이름과 나이만 넣고 싶은데, 생성자 모양을 맞추기 위해 억지로 `null`을 여러 개 적어줘야 합니다. 보기 흉하고 가독성이 떨어집니다.

---

## 2. `@Builder`를 사용한 깔끔한 객체 생성

Lombok의 `@Builder` 어노테이션을 클래스(또는 생성자) 위에 붙이면, 아래처럼 **이름을 직접 지정해서** 값을 넣을 수 있습니다.

```java
// Builder를 사용한 객체 생성
User user = User.builder()
        .name("수진")
        .age(25)
        .address("서울")
        .build(); // 조립 끝! 완성!
```

**[빌더 패턴의 장점]**
1. **명확한 가독성**: 어떤 필드에 어떤 값이 들어가는지 한눈에 보입니다. 순서가 바뀌어도 상관없습니다.
2. **필요한 값만 세팅 가능**: `id`, `phone`, `createdAt` 같은 값들은 아예 안 적어도 됩니다. (안 적으면 자동으로 null 또는 기본값이 들어갑니다.)
3. **안전한 Entity 생성**: 외부에서 강제로 `id`나 상태값을 덮어쓰는 실수를 방지하고, 필요한 데이터만으로 깨끗한 Entity를 만들 수 있습니다.

---

## 3. 실무적인 Entity 설계 구조 (Builder 활용)

가장 권장되는 깔끔한 Entity 형태는 아래와 같습니다.

```java
@Entity
@Getter // 값은 읽을 수 있어야 함 (Setter는 쓰지 않음!)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자는 막아둠
public class PointPayment {

    @Id @GeneratedValue
    private Long id;            // DB가 알아서 생성
    private Long userId;
    private int amount;
    private String status;      // 비즈니스 로직이 관리
    private String requestedAt; // DB가 알아서 생성

    // 💡 핵심: 외부에 열어둘 '생성 전용' 생성자에만 @Builder를 붙임
    @Builder 
    public PointPayment(Long userId, int amount) {
        this.userId = userId;
        this.amount = amount;
        this.status = "REQUESTED"; // 생성될 때 상태는 무조건 REQUESTED로 고정!
    }
}
```

이렇게 설계해두면 Service 계층에서 객체를 만들 때 아주 안전해집니다.

```java
// Service 코드
PointPayment payment = PointPayment.builder()
        .userId(1L)
        .amount(5000)
        // .id(999L) 👈 애초에 Builder에 id나 status 구멍을 안 뚫어놔서 조작 불가! 안전함!
        .build();
```

---

## 4. Builder와 상태 변경 메서드를 구분하기

Builder는 **새 객체를 생성할 때** 가독성과 안전성을 높이는 도구입니다.
반대로 이미 존재하는 Entity의 상태를 바꾸는 유스케이스에서는 Builder보다 **의미 있는 도메인 메서드**가 더 적절합니다.

### 주문 취소 예시

```java
// ❌ Service가 Entity 필드를 직접 변경
order.status = "CANCELLED";
order.cancelledAt = LocalDateTime.now();
order.cancelReason = request.reason;
```

위 코드를 Builder로 새 객체 생성처럼 바꾸는 것이 항상 정답은 아닙니다.
주문 취소는 "새 주문 생성"이 아니라 "기존 주문의 상태 전이"이기 때문입니다.

```java
// ✅ Entity 안에 상태 변경 규칙을 캡슐화
order.cancel(request.reason, LocalDateTime.now());
```

### 암기 포인트

> Builder는 생성 패턴이다.
> `cancel()`, `changeAddress()`, `refund()` 같은 도메인 메서드는 상태 변경 규칙을 보호하는 캡슐화다.
> 기존 Entity의 상태 전이는 Builder보다 의미 있는 도메인 메서드로 표현하는 것이 더 자연스럽다.

---

## 5. `@Builder` 패턴 vs Java `record` 사용 기준

수진님이 질문하신 빌더 패턴과 레코드의 사용 시점을 비교하여 정리한 핵심 가이드라인입니다.

| 비교 항목 | `@Builder` (Lombok) | Java `record` (Java 16+) |
| :--- | :--- | :--- |
| **핵심 목적** | 객체 생성 시 가독성 및 필드 선택 조립 | 변경 불가능한 불변 데이터 캐리어 정의 |
| **불변성 여부** | 가변(Mutable) 객체와 불변 객체 모두 사용 가능 | **100% 불변(Immutable)** (모든 필드가 `final`) |
| **주요 대상** | **JPA Entity**, 복잡한 가변 도메인 모델 | **Request/Response DTO**, 단순 데이터 운반 객체 |
| **JPA Entity 가능 여부**| **가능** (기본 생성자 및 프록시 생성 제약 없음) | **불가능** (Hibernate 프록시 생성 및 가변 필드 수정 제약) |

### 💡 실무 적용 원칙

1. **JPA Entity에는 `@Builder`를 사용합니다.**
   * JPA(Hibernate)는 프록시 생성(지연 로딩) 및 dirty checking(변경 감지) 동작을 위해 **인스턴스 필드의 수정(가변성)**과 **인자가 없는 기본 생성자**가 필수적입니다.
   * 레코드(`record`)는 필드가 강제로 `final`이 되고 상속이 금지되므로 **JPA Entity로 사용할 수 없습니다.**
   * 따라서 엔티티를 생성할 때는 생성자 위에 `@Builder`를 선언해 가독성을 확보하는 것이 모범 사례입니다.

2. **API 요청/응답(DTO)에는 `record`를 사용합니다.**
   * 컨트롤러가 받거나 반환하는 DTO(Data Transfer Object)는 데이터를 전달하는 동안 값이 변해서는 안 되며, 비즈니스 로직을 가지지 않습니다.
   * 레코드를 사용하면 보일러플레이트 코드(Getter, 생성자, equals, hashCode, toString) 없이 깔끔하게 불변 DTO를 정의할 수 있습니다.

# Util (Utility) 클래스의 역할과 의미

Spring의 기본적인 계층 구조(Controller → Service → Repository)를 공부하다 보면, 종종 `Util`이라는 이름이 붙은 클래스나 패키지를 보게 됩니다. `Util`이 정확히 무엇인지, 어떤 역할을 하는지 정리합니다.

---

## 1. Util이란 무엇인가?

`Util`은 **Utility(유틸리티, 도구)** 의 줄임말입니다. 
집에 있는 **'공구함(드라이버, 망치)'** 이라고 생각하면 이해하기 쉽습니다. 

Controller, Service, Repository는 각자의 명확한 역할(웹 요청 처리, 비즈니스 로직, DB 저장)이 있는 반면, **Util은 특정 계층에 속하지 않고 프로젝트 전반에서 공통으로 쓰이는 자잘한 기능들을 모아둔 도구 상자**입니다.

---

## 2. 계층 구조 속에서의 위치

Util은 "Controller → Service → Repository → Util" 처럼 특정한 순서나 흐름을 가지는 **계층(Layer)이 아닙니다.**
오히려 모든 계층에서 필요할 때마다 자유롭게 가져다 쓰는 **공통 도우미(Helper)** 역할입니다.

```
[Controller] ─────┐
                  │
[Service]    ─────┼────▶ [ Util 클래스들 ] (DateUtil, StringUtil 등)
                  │
[Repository] ─────┘
```

---

## 3. Util 클래스의 특징

Util 클래스는 다음과 같은 엄격한 규칙을 가져야 합니다.

### ① 상태(State)를 가지지 않는다
객체마다 달라지는 변수(인스턴스 필드)를 가지면 안 됩니다. 항상 입력값이 같으면 출력값도 같아야 합니다.

### ② 대부분 `static` 메서드로 구성된다
객체를 `new`로 생성할 필요 없이, `DateUtil.format(date)` 처럼 클래스 이름으로 바로 호출해서 쓰도록 만듭니다. (Spring Bean으로 등록하지 않는 경우가 많습니다.)

### ③ 비즈니스 로직이나 DB 접근을 하지 않는다
Util은 순수한 계산, 변환만 담당해야 합니다. 여기서 "이 유저가 VIP인가?" 같은 비즈니스 판단을 하거나 Repository를 호출해서 DB에서 값을 가져오면 안 됩니다.

---

## 4. 대표적인 Util의 예시

보통 이름 뒤에 `Util`이나 `Utils`를 붙입니다.

| 예시 | 역할 | 사용 예시 |
|------|------|----------|
| `DateUtils` | 날짜/시간 변환 및 계산 | 문자열 "20230101"을 `LocalDate` 객체로 변환 |
| `StringUtils` | 문자열 조작 및 검증 | 문자열이 비어있는지(`isEmpty`) 확인, 마스킹 처리 |
| `PasswordUtils` | 비밀번호 암호화 | 평문 비밀번호를 해시 암호문으로 변환 |
| `MathUtils` | 복잡한 수학 계산 | 반올림 로직 등 |

### 코드 예시
```java
public class DateUtil {
    // 1. 객체 생성을 막기 위해 생성자를 private으로 숨김
    private DateUtil() {} 

    // 2. static 메서드로 제공
    public static String formatToYMD(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
```

### 실제 활용 시나리오 (전화번호 마스킹)

회원가입 컨트롤러에서도, 관리자 페이지 서비스에서도 전화번호 뒷자리를 `010-1234-****` 처럼 가려야(마스킹) 하는 상황을 가정해 보겠습니다.

**[Util이 없을 때 (나쁜 예)]**
```java
// UserController.java (컨트롤러에서도 로직 작성)
String phone = request.getPhone();
String maskedPhone = phone.substring(0, phone.length() - 4) + "****";

// AdminService.java (서비스에서도 똑같은 로직 중복 작성)
String phone = user.getPhone();
String maskedPhone = phone.substring(0, phone.length() - 4) + "****";
```
여기저기서 같은 로직을 쓰다 보니 코드가 중복되고 지저분해집니다.

**[Util을 만들어서 사용할 때 (좋은 예)]**

1. 공통 도구(Util)를 하나 만듭니다.
```java
// StringUtil.java (공통 도구 상자)
public class StringUtil {
    // static으로 만들어서 new 없이 바로 쓸 수 있게 함!
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
```

2. 모든 계층에서 편하게 가져다 씁니다.
```java
// UserController.java
String maskedPhone = StringUtil.maskPhoneNumber(request.getPhone());

// AdminService.java
String maskedPhone = StringUtil.maskPhoneNumber(user.getPhone());
```
이렇게 **특정 계층에 얽매이지 않고, 누구나 쉽게 꺼내 쓸 수 있는 공통 함수들을 모아둔 곳**이 바로 Util 입니다!

---

## 5. 주의할 점 (Util 남용의 위험성)

*   **무분별한 Util 생성 금지**: "어디 넣을지 모르겠네? 대충 Util에 넣자"라고 하다 보면, Util 클래스가 쓰레기통처럼 변합니다.
*   **도메인 로직은 Entity 안으로**: 데이터 변환 로직이 특정 Entity(예: User)에만 강하게 연관되어 있다면, `UserUtil`을 만들기보다는 `User` Entity 내부의 메서드로 만드는 것이 객체지향적입니다.
*   **외부 라이브러리 활용**: 문자열이나 컬렉션 조작은 내가 직접 만들기보다 Apache Commons (`StringUtils`)나 Guava 같은 이미 검증된 라이브러리의 Util을 쓰는 것이 좋습니다.

---

## 6. 006번 Order Cancel Util 예시

### 기존 코드의 문제점 (`OrderCancelUtil.java`)

```java
public class OrderCancelUtil {
    public static boolean isCancelWindowClosed(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        return LocalDateTime.now().minusMinutes(30).isAfter(createdAt);
    }

    public static void requestExternalPgRefund(Long orderId, int refundAmount) {
        System.out.println("PG Refund Request: orderId=" + orderId + ", amount=" + refundAmount);
    }

    public static void sendAuditLog(Long userId, String message) {
        System.out.println("audit log send to user " + userId + ": " + message);
    }
}
```

1. **비즈니스/도메인 정책의 Util 포함**: "30분 이내 취소 가능"이라는 도메인 정책이 Util 클래스 안에 굳어있어 정책 변경(예: 1시간) 시 Util 코드를 수정해야 합니다.
2. **테스트 불가능성 (LocalDateTime.now())**: `LocalDateTime.now()`가 static 메서드에 박혀있어 테스트 코드 실행 시점에 따라 결과가 바뀌며 고정하기 어렵습니다.
3. **외부 API 호출/부작용(Side Effect)의 static 처리**: 외부 PG 호출 및 감사 로그 전송이 static 메서드로 되어 있어 단위 테스트 시 Mocking이나 Fake 객체로 교체하는 것이 불가능합니다.
4. **로깅 대신 `System.out.println`**: 디버깅과 모니터링이 필요한 실무 로그를 `System.out`으로 출력하여 로그 관리 및 추적이 불가능합니다.

### 개선 방향

- **시간 격리**: `Clock` 객체를 주입받아 `LocalDateTime.now(clock)`으로 변경하고, Util을 Spring Bean(`@Component`)으로 등록하여 의존성을 주입받도록 설계합니다.
- **인터페이스 분리 및 DI**: 외부 PG 환불은 `PgRefundClient` 컴포넌트로, 감사 로그는 `AuditLogger` 등으로 분리하여 의존성 주입을 받습니다.

```java
@Component
public class OrderCancelValidator {
    private final Clock clock;

    public OrderCancelValidator(Clock clock) {
        this.clock = clock;
    }

    public boolean isCancelWindowClosed(LocalDateTime createdAt) {
        if (createdAt == null) {
            return true;
        }
        return LocalDateTime.now(clock).minusMinutes(30).isAfter(createdAt);
    }
}
```

---

## 7. 007번 Service의 `LocalDateTime.now()` 직접 호출

`LocalDateTime.now()` 문제는 Util 클래스에만 생기는 문제가 아닙니다.
Service 안에서 발급 가능 시간, 만료 시간, 취소 가능 시간 같은 정책을 직접 현재 시간으로 판단해도 같은 문제가 생깁니다.

```java
LocalDateTime now = LocalDateTime.now();

if (now.isBefore(coupon.startsAt) || now.isAfter(coupon.endsAt)) {
    throw new StockCouponException("not issuable");
}
```

이 코드는 실행하는 순간의 실제 시간에 따라 결과가 달라집니다.
그래서 테스트에서 "발급 시작 직전", "발급 시작 시각", "발급 종료 직후" 같은 경계값을 고정하기 어렵습니다.

개선 방향은 `Clock`을 주입받거나 시간 판단 정책을 별도 객체로 분리하는 것입니다.

```java
public class StockCouponIssuePolicy {
    private final Clock clock;

    public StockCouponIssuePolicy(Clock clock) {
        this.clock = clock;
    }

    public void validateIssuable(StockCoupon coupon) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(coupon.startsAt) || now.isAfter(coupon.endsAt)) {
            throw new CouponNotIssuableException();
        }
    }
}
```

면접용 문장:

```text
시간 기준 정책은 LocalDateTime.now()를 직접 호출하면 테스트가 실행 시각에 의존합니다.
Clock을 주입하거나 정책 객체로 분리해서 발급 시작/종료 같은 경계값을 고정해 검증할 수 있게 하겠습니다.
```

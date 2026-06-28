# Review Answer

## 먼저 볼 것

이 파일은 피드백을 모두 기록한 문서라 길다. 처음 복습할 때는 전부 읽지 말고 아래만 먼저 본다.

1. `채점`
2. `반드시 잡았어야 하는 문제`
3. `시니어 개발자의 피드백 (면접관 관점)`

코드를 다시 볼 때는 아래 5개를 먼저 확인한다.

1. Controller가 Entity를 직접 받거나 반환하는지
2. Controller에 비즈니스 판단이 들어갔는지
3. Service에서 상태 변경/저장이 있는데 트랜잭션 경계가 보이는지
4. 반복문 안에서 Repository 조회가 발생하는지
5. `RuntimeException`, `null`, 문자열 상태값처럼 장애 원인을 숨기는 코드가 있는지

`solution/`과 `refactoring-plan.md`는 1차 복습 뒤에 본다.

## 리팩토링된 코드 보기

리팩토링된 코드는 아래에 있다.

- 리팩토링 계획: `refactoring-plan.md`
- 리팩토링 요약: `solution/README.md`
- 리팩토링 코드: `solution/src/main/java/com/example/coupon/refactoring/`
- 리팩토링 테스트: `solution/src/test/java/com/example/coupon/refactoring/CouponService_RefactoredTest.java`

주요 파일:

1. `CouponController_Refactored.java`
2. `CouponService_Refactored.java`
3. `CouponRepository_Refactored.java`
4. `InMemoryCouponRepository_Refactored.java`
5. `CouponPolicy_Refactored.java`
6. `CouponRequest_Refactored.java`
7. `CouponResponse_Refactored.java`
8. `Coupon_Refactored.java`
9. `CouponException_Refactored.java`

리팩토링된 흐름의 핵심:

1. `CouponRequest_Refactored`는 `userId`, `code`만 받는다.
2. `CouponService_Refactored`는 `code`로 인메모리 저장소의 기존 쿠폰을 먼저 조회한다.
3. 쿠폰이 없으면 `COUPON_NOT_FOUND`로 실패한다.
4. 쿠폰 소유자가 다르거나 이미 사용/만료 상태면 `COUPON_NOT_AVAILABLE`로 실패한다.
5. 정상 쿠폰만 `USED` 상태로 저장하고 `CouponResponse_Refactored`를 반환한다.

각 파일의 핵심 변경 지점에는 `기존 코드:` / `수정 코드:` 주석을 달았다.

## Controller

- 500 SERVER_ERROR -> 에러코드르 바로 뱉는것 내가 알기론느 감싸서 내보내야한는 걸로 알고 있느데,
- equest.orderAmount < 0 -> 이런 로직적인 부분이 들어가는거? 이런것도 
- controller는 사용자의 데이터를 받아서 service한테 전달을 하는 service안에는 로직으로 풀어야 하니까 request.orderAmount < 0 이건 service에서 해야한는게 아닌가 라는 생각을해
## Service

- 여기부분에선 잘 모르겠어 appluCoupon도 dto로 받아서 entity로 변환을 해주고

## Repository

- 이부분도 잘 모르겟어 크게 문제가 없는것 같기도한데 

## Util

- 여기서 말하는 util이 의미하는게 뭔지 모르겟어 
- vip, welcom 이라는거가 ..하드코딩이되어잇어서 오류가 범할 수 있어서 이넘으로 하면 좋을것 가ㅏㅌ아 

## DTO / Entity

- @Getter @Setter @Allaugument 이런 어테이션이 없어. 

## Exception

- 이부분도 정확히 잘모르겟어 에러가 생기거나 문제가 생기면 exception을 내라는것 같은데... 

## Test

- 여기엔 @test도 없고 , 그리고 ... 여러 필요한 어노테이션이 안보이는데..? 로직에 맞는 시나리오도 없는것 같고 말이야. 
- 

## 채점

학습 점수: 58점

실전 면접관 기준 점수: 55점

58점은 학습 피드백 기준으로는 가능하지만, 실제 1차 기술면접에서라면 조금 후한 점수다. 답변에서 Controller와 Test의 표면 문제는 잡았지만, Service/Repository/Exception/Entity 책임의 핵심을 많이 놓쳤고 "잘 모르겠다", "크게 문제가 없는 것 같다"에서 멈춘 부분이 많다.

면접관 입장에서는 "문제를 일부 볼 줄은 알지만, 계층별 책임과 장애/운영 리스크까지 안정적으로 설명하는 수준은 아직 부족하다"에 가깝다.

### 잘한 점

- Controller에서 잘못된 상태 코드 반환을 잡았다.
- Controller에서 `orderAmount < 0` 실패를 `"200 OK"`로 처리하는 이상한 흐름을 의심했다.
- Util 안의 `VIP`, `WELCOME` 문자열 하드코딩 문제를 잡았다.
- Test에 `@Test`가 없고 시나리오가 부족하다는 점을 잡았다.

### 보완할 점

- Repository에서 반복문 안 DB 조회에 해당하는 흐름을 놓쳤다. Service가 주문 id 목록을 가져온 뒤 반복문 안에서 `findOrderAmount()`를 계속 호출한다.
- `Coupon` Entity를 Controller 요청과 Service 응답에 그대로 쓰는 문제를 놓쳤다.
- Service가 `Object`를 반환하고 Entity를 직접 수정해서 반환하는 문제를 놓쳤다.
- Service에 상태 변경과 저장이 있는데 트랜잭션 경계가 보이지 않는 문제를 놓쳤다.
- `RuntimeException` 직접 사용과 `CouponException`이 정의만 되고 사용되지 않는 문제를 놓쳤다.
- Repository의 `static List`, `return null`, `System.out.println()` 문제를 놓쳤다.
- `@Getter/@Setter`가 없다는 지적은 핵심이 아니다. 중요한 것은 필드가 public이고 상태 변경이 아무 곳에서나 가능하다는 점이다.

### 반드시 잡았어야 하는 문제

- `CouponRequest`가 있는데도 Controller가 `Coupon` Entity를 요청으로 받는다.
- `CouponRequest`에 `status`가 들어 있어 클라이언트가 서버 관리 상태를 보낼 수 있다.
- Service가 기존 쿠폰을 조회하지 않고 클라이언트가 보낸 `Coupon` 객체의 상태와 코드를 그대로 신뢰한다.
- 쿠폰 존재 여부, 이미 사용된 쿠폰 여부, 중복 사용 방지 흐름이 없다.
- Service가 `Object`를 반환하고 Entity를 직접 수정해서 응답한다.
- Repository 조회가 반복문 안에서 발생하고, null 반환/static 상태/중복 저장 문제가 있다.
- `CouponException`이 정의되어 있지만 사용하지 않고 `RuntimeException`으로 뭉뚱그린다.
- `CouponUtil.canUse()`의 `status.equals("EXPIRED")`는 status가 null이면 NPE가 날 수 있다.

## 이번 문제 개념 키워드

### DTO validation / `@Valid`

- 이 코드에서 나온 위치: `CouponController.apply()`가 요청 검증을 직접 if문으로 처리한다.
- 개념 설명: Controller에서 수동 검증을 늘리기보다 요청 DTO에 검증 규칙을 두고 `@Valid`로 검증 흐름을 위임하는 방식이 일반적이다.
- 면접용 문장: "요청 검증은 Controller에 흩뿌리기보다 DTO validation으로 표현하고, Controller는 요청을 받아 Service에 위임하는 역할에 집중시키겠습니다."

### Entity 직접 노출

- 이 코드에서 나온 위치: Controller가 `Coupon` Entity를 요청으로 받고, Service가 Entity를 그대로 반환한다.
- 개념 설명: Entity는 저장/도메인 모델이고 API Request/Response는 외부 계약이다. 둘을 섞으면 DB 구조나 내부 상태가 API에 노출된다.
- 면접용 문장: "Entity를 요청/응답에 직접 쓰면 API 계약과 도메인 모델이 강하게 결합되므로 DTO를 분리하는 편이 안전합니다."

### Transaction boundary / `@Transactional`

- 이 코드에서 나온 위치: `CouponService.applyCoupon()`에서 쿠폰 상태 변경과 저장이 일어나지만 트랜잭션 경계가 보이지 않는다.
- 개념 설명: 이 예제는 인메모리 저장소라 실제 트랜잭션이 동작하지는 않지만, 실제 DB나 영속 저장소라면 조회, 상태 변경, 저장을 하나의 작업 단위로 묶어 정합성을 지켜야 한다.
- 면접용 문장: "실제 DB를 사용하는 코드라면 쿠폰 적용은 상태 변경과 저장이 하나의 작업 단위라서 Service 유스케이스 메서드에 트랜잭션 경계를 두는 게 적절합니다."

### Repository 반복 조회

- 이 코드에서 나온 위치: Service가 주문 id 목록을 가져온 뒤 반복문 안에서 `findOrderAmount()`를 계속 호출한다.
- 개념 설명: 실제 DB나 외부 저장소라면 반복문 안 조회가 데이터 수만큼 반복되어 성능 문제로 이어질 수 있다. 필요한 데이터를 한 번에 조회하는 방향을 검토한다.
- 면접용 문장: "반복문 안에서 Repository를 계속 호출하면 데이터가 늘어날수록 조회 횟수가 증가하므로, 필요한 주문 금액을 한 번에 조회하도록 바꾸겠습니다."

### Exception handling

- 이 코드에서 나온 위치: `CouponException`이 있지만 Service는 `RuntimeException("coupon failed")`를 던지고 Controller는 모두 400으로 바꾼다.
- 개념 설명: 도메인 예외, 검증 예외, 시스템 예외를 구분하지 않으면 클라이언트 응답과 운영 로그에서 원인을 파악하기 어렵다.
- 면접용 문장: "RuntimeException으로 뭉뚱그리기보다 도메인 예외와 에러 코드를 분리해서 클라이언트 오류와 서버 오류를 구분하겠습니다."

### Util에 비즈니스 로직 포함

- 이 코드에서 나온 위치: `CouponUtil`이 쿠폰 사용 가능 여부와 할인 계산 정책을 담당한다.
- 개념 설명: Util은 보통 순수하고 범용적인 보조 기능에 가깝다. 쿠폰 정책처럼 바뀔 수 있는 비즈니스 규칙은 정책 객체나 Service/Domain으로 분리하는 편이 낫다.
- 면접용 문장: "쿠폰 할인 정책은 단순 Util이 아니라 도메인 정책이므로 별도 Policy 객체로 분리해 테스트 가능하게 만들겠습니다."

## 보완 답변

### Controller

- 요청을 `CouponRequest`가 아니라 `Coupon` Entity로 직접 받고 있다. API 요청 모델과 저장/도메인 모델이 섞인다.
- `@Valid`에 해당하는 검증 흐름이 보이지 않는다. `userId`, `code`, `orderAmount` 검증을 수동 if문으로 처리한다.
- `request == null` 같은 잘못된 요청을 `"500 SERVER_ERROR"`로 반환한다. 클라이언트 입력 오류는 보통 400 계열로 내려야 한다.
- `orderAmount < 0`일 때 `"200 OK"`를 반환한다. 실패인데 성공 응답처럼 보인다.
- 모든 `RuntimeException`을 `"400 BAD_REQUEST"`로 뭉뚱그린다. 쿠폰 만료, 중복 사용, 시스템 오류를 구분할 수 없다.
- Service를 직접 `new`로 생성하고 있어 의존성 주입 구조가 아니다.
- `orderSummary()`에서 `Long.parseLong(userId)`를 직접 수행해 잘못된 입력이 들어오면 예외 처리 없이 터질 수 있다.

### Service

- Service에 상태 변경과 저장이 있는데 트랜잭션 경계가 보이지 않는다.
- `Object`를 반환해 API 응답 타입이 불명확하다.
- `Coupon` Entity를 직접 수정하고 그대로 반환한다. 응답 DTO를 따로 두는 편이 안전하다.
- 기존 쿠폰을 `code`로 조회하지 않고, 클라이언트가 보낸 쿠폰 상태와 코드를 그대로 신뢰한다.
- 쿠폰 존재 여부, 이미 사용 여부, 만료 여부를 서버 데이터 기준으로 확인하지 않아 중복 사용이나 조작 요청에 취약하다.
- 주문 id 목록을 가져온 뒤 반복문 안에서 주문 금액을 다시 조회한다. 실제 DB나 외부 저장소라면 데이터 수만큼 조회가 반복되는 성능 문제가 될 수 있다.
- 쿠폰 실패 시 `RuntimeException("coupon failed")`를 직접 던진다. 도메인 예외와 에러 코드를 쓰는 편이 낫다.
- `getOrderSummary()`에서 문자열을 직접 조립한다. Service가 응답 포맷까지 담당하고 있고, 반복 문자열 결합도 비효율적이다.
- `coupon.status = "USED"`처럼 문자열 상태값을 직접 넣는다. enum이나 상태 전이 메서드가 더 안전하다.

### Repository

- `static List`를 저장소처럼 사용해 테스트 간 상태가 공유될 수 있다.
- `findOrderIdsByUserId()`와 `findOrderAmount()`가 하드코딩된 데이터를 반환한다. 실제 Repository라면 조회 조건과 데이터 접근 방식이 불명확하다.
- `findByCode()`는 못 찾으면 `null`을 반환한다.
- `coupon.code.equals(code)`는 `coupon.code`가 null이면 NPE가 발생할 수 있다.
- `save()`에서 `System.out.println()`으로 로그를 직접 출력한다.
- 같은 쿠폰을 저장해도 기존 데이터를 갱신하지 않고 계속 add한다.

### Util

- Util에 쿠폰 사용 가능 여부와 할인 계산이라는 비즈니스 정책이 들어 있다.
- `VIP`, `WELCOME`, `EVENT`, `EXPIRED` 같은 문자열이 하드코딩되어 있다.
- `status.equals("EXPIRED")`는 status가 null이면 NPE가 발생한다.
- static method라 정책을 테스트하거나 교체하기 어렵다. CouponPolicy 같은 도메인 정책 객체나 Service로 분리하는 편이 낫다.

### DTO / Entity

- `CouponRequest`가 있지만 Controller는 `Coupon`을 직접 받는다.
- `CouponRequest`에도 `status`, `orderAmount`가 들어 있다. 특히 `status`는 클라이언트가 임의로 보내면 안 되는 값일 수 있다.
- `Coupon`과 `CouponRequest`의 필드가 거의 같아 DTO와 Entity 역할이 흐려져 있다.
- 모든 필드가 public이라 캡슐화가 없고, 어디서든 상태를 바꿀 수 있다.
- 중요한 것은 `@Getter/@Setter`가 없다는 점이 아니라, 상태 변경을 통제할 수 없다는 점이다.

### Exception

- `CouponException`이 정의되어 있지만 실제로 사용되지 않는다.
- Service는 `RuntimeException`을 직접 던지고, Controller는 이를 모두 400으로 바꾼다.
- 예외 메시지가 `"coupon failed"`처럼 모호하다.
- 실제 구조라면 쿠폰 사용 불가, 존재하지 않는 쿠폰, 이미 사용된 쿠폰, 시스템 오류를 구분해야 한다.

### Test

- JUnit `@Test`가 없어 실제 테스트로 실행되지 않는다.
- 성공 케이스 하나만 확인하고, 검증도 `result != null` 수준이다.
- 만료 쿠폰, 최소 주문 금액 미달, 이미 사용된 쿠폰, 잘못된 요청, 주문 금액 조회 실패 같은 케이스가 없다.
- Repository가 static 상태를 갖고 있어 테스트 간 격리가 깨질 수 있다.
- Service가 직접 Repository를 new로 생성해 테스트 대역을 넣기 어렵다.

## 면접관 예상 꼬리질문

### 내 답변에서 바로 이어질 질문

1. "Controller에 로직이 있다고 했는데, 어떤 로직까지는 Controller에 있어도 되고 어떤 로직부터 Service로 내려야 한다고 보나요?"
   - 질문 의도: Controller 책임과 Service 책임의 경계를 설명할 수 있는지 확인한다.
   - 답변 방향: 요청 파싱, 인증 정보 추출, DTO 검증 결과 처리, 응답 생성은 Controller에 둘 수 있지만 쿠폰 사용 가능 여부, 할인 계산, 상태 변경 같은 비즈니스 판단은 Service/Domain으로 옮긴다고 말한다.

2. "`@Getter/@Setter`가 없다고 했는데, 이 코드에서 진짜 문제는 Lombok 어노테이션이 없는 건가요?"
   - 질문 의도: 어노테이션 유무가 아니라 캡슐화와 상태 변경 통제가 본질임을 아는지 확인한다.
   - 답변 방향: Lombok이 핵심이 아니라 public field 때문에 아무 계층에서나 상태를 바꿀 수 있고, 상태 전이 규칙을 Entity나 Service에서 통제하지 못하는 것이 문제라고 말한다.

3. "Repository는 크게 문제가 없는 것 같다고 했는데, 실제 DB Repository라면 어떤 장애나 성능 문제가 생길 수 있나요?"
   - 질문 의도: Repository 조회 방식과 테스트 격리 문제를 볼 수 있는지 확인한다.
   - 답변 방향: 반복문 안 조회로 쿼리 수가 증가할 수 있고, null 반환은 NPE를 유발하며, static 저장소는 테스트 간 상태 공유 문제를 만들 수 있다고 말한다.

### 내가 놓친 개념을 확인하는 질문

1. "쿠폰 적용 메서드에 트랜잭션 경계가 없으면 실제 DB를 사용하는 코드에서는 어떤 문제가 생길 수 있나요?"
   - 질문 의도: `@Transactional`을 단순 어노테이션이 아니라 정합성 보장 관점으로 이해하는지 확인한다.
   - 답변 방향: 이 예제는 인메모리 코드지만, 실제 DB를 사용한다면 쿠폰 조회, 상태 변경, 저장이 하나의 작업 단위라서 중간 실패 시 일부만 반영되지 않도록 Service 유스케이스 단위에 트랜잭션 경계를 둔다고 말한다.

2. "왜 Entity를 Request/Response로 직접 쓰면 위험한가요?"
   - 질문 의도: DTO와 Entity 분리 이유를 API 계약과 내부 모델 결합 관점에서 설명할 수 있는지 확인한다.
   - 답변 방향: 내부 필드가 외부에 노출되고, 클라이언트가 `status` 같은 서버 관리 값을 보낼 수 있으며, DB 구조 변경이 API 변경으로 번질 수 있다고 말한다.

3. "RuntimeException을 그냥 던지는 것과 도메인 예외를 정의하는 것의 차이는 뭔가요?"
   - 질문 의도: 예외 정책, 상태 코드 매핑, 운영 로그 관점을 볼 수 있는지 확인한다.
   - 답변 방향: 실패 원인을 구분해야 적절한 HTTP 상태 코드와 에러 메시지를 줄 수 있고, 운영 중에도 원인 파악이 쉬워진다고 말한다.

### 위험한 표현을 파고드는 질문

1. "`DTO로 받아서 Entity로 변환하면 된다`고 했는데, 변환 위치는 어디가 적절하다고 보나요?"
   - 질문 의도: DTO 변환 책임을 Controller, Service, Mapper 중 어디에 둘지 트레이드오프를 설명할 수 있는지 확인한다.
   - 답변 방향: 이 문제에서는 요청 DTO를 새 Entity로 단순 변환하기보다, 요청의 `code`로 서버의 기존 쿠폰을 조회한 뒤 도메인 상태를 검증하고 변경하는 흐름이 더 안전하다고 말한다.

2. "`Exception을 내면 된다`고 했는데, 어떤 예외를 어디서 처리해야 하나요?"
   - 질문 의도: 예외를 던지는 것과 예외 처리 정책을 구분하는지 확인한다.
   - 답변 방향: Service는 도메인 예외를 던지고, ControllerAdvice 같은 전역 예외 처리에서 에러 코드와 HTTP 상태 코드로 매핑한다고 말한다.

## 다음 문제에서 다시 볼 약점

- Repository에서 반복 조회가 발생하는지 보기
- DTO가 있는데도 Entity를 요청/응답에 직접 쓰는지 보기
- 클라이언트가 보낸 상태값을 서버가 그대로 신뢰하는지 보기
- 기존 데이터를 조회하지 않고 요청 객체만으로 상태 변경을 처리하는지 보기
- Util에 비즈니스 정책이 들어갔는지 보기
- 예외 타입이 정의만 되어 있고 실제로 쓰이지 않는지 보기
- `@Getter/@Setter` 유무보다 캡슐화와 상태 변경 통제를 먼저 보기

---

## 시니어 개발자의 피드백 (면접관 관점)

### 인정되는 답변

1. Controller에 비즈니스 판단이 들어간 점을 의심했다.
2. Util의 문자열 하드코딩을 enum으로 바꾸자는 방향을 말했다.
3. 테스트에 `@Test`가 없고 시나리오가 부족하다는 점을 잡았다.

### 실제 면접에서 부족하게 들리는 지점

1. Util의 핵심 문제를 "하드코딩"으로만 봤고, 비즈니스 정책이 Util에 들어간 책임 위반까지는 설명하지 못했다.
2. `@Getter/@Setter` 유무를 지적했지만, 더 중요한 Entity 직접 노출과 public field로 인한 캡슐화 붕괴를 놓쳤다.
3. Repository를 "크게 문제 없는 것 같다"고 말해 반복 조회, null 반환, static 상태 공유, 중복 저장 문제를 놓쳤다.
4. 상태 변경과 저장이 있는 Service 메서드에서 트랜잭션 경계가 없다는 점을 잡지 못했다.
5. 클라이언트가 보낸 쿠폰 상태를 신뢰하고, 서버의 기존 쿠폰 조회/중복 사용 검증이 없다는 점을 잡지 못했다.

### 다음 리뷰에서 먼저 확인할 것

1. Controller가 Entity를 직접 요청/응답으로 쓰는지 확인한다.
2. 반복문 안에서 Repository 조회가 발생하는지 확인한다.
3. 상태 변경이 있는 Service 메서드에 트랜잭션 경계가 있는지 확인한다.
4. Util이 단순 공통 기능이 아니라 도메인 정책을 판단하고 있는지 확인한다.
5. 서버가 관리해야 할 상태를 클라이언트 입력으로 신뢰하고 있지 않은지 확인한다.

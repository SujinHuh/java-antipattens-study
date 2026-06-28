# 004. Point Refund Legacy Review - 피드백

## 먼저 볼 것

처음 복습할 때는 아래 순서만 먼저 봅니다.

1. `핵심 문제 요약`
2. `채점`
3. `반드시 잡았어야 하는 문제`
4. `문제 유형 분류`
5. `안티패턴 지침화 후보`
6. `시니어 개발자의 피드백 (면접관 관점)`

`solution/`과 `refactoring-plan.md`는 1차 복습 뒤에 봅니다.

## 핵심 문제 요약

### 1. Entity를 Request/Response로 직접 사용

- 문제 유형: 명백한 안티패턴
- 코드 위치: `PointRefundController.refund(PointPayment payment)`
- 왜 핵심인지: 외부 API 계약이 내부 저장 모델과 결합되고, 클라이언트가 `status`, `amount`, `userId` 같은 서버 관리 상태를 조작할 수 있다.
- 면접용 문장: "Controller가 내부 모델을 직접 받거나 반환하면 API 계약과 도메인 상태가 결합되므로 Request/Response DTO로 분리해야 합니다."

### 2. Service가 클라이언트 입력 상태를 신뢰함

- 문제 유형: 돌아가지만 위험한 코드
- 코드 위치: `PointRefundService.refund`
- 왜 핵심인지: 조회 실패 시 요청 객체를 그대로 사용하고, 요청의 `amount`, `reason`, `status`를 기준으로 환불 상태를 바꾼다. 결제가 요청 사용자 소유인지도 검증하지 않는다.
- 면접용 문장: "환불은 서버에 저장된 결제 상태와 소유자 검증을 기준으로 처리해야 하며, 클라이언트가 보낸 Entity 상태를 신뢰하면 없는 결제나 조작된 금액도 환불될 수 있습니다."

### 3. 반복 Repository 조회

- 문제 유형: 성능 안티패턴
- 코드 위치: `PointRefundService.getRefundHistory`
- 왜 핵심인지: `findAll()` 후 반복문에서 `findById()`를 다시 호출하므로 실제 DB라면 1번 조회 뒤 N번 단건 조회가 추가될 수 있다.
- 면접용 문장: "엄밀한 JPA 연관관계 N+1이라고 단정하기보다는, DB 기반이라면 N+1 형태의 반복 Repository 조회 문제라고 설명하는 것이 안전합니다."

### 4. 예외 정책과 HTTP 응답 책임이 섞임

- 문제 유형: 명백한 안티패턴
- 코드 위치: `RuntimeException`, `PointRefundException`, `"200 OK: " + result`
- 왜 핵심인지: 실패 원인과 HTTP 상태를 일관되게 매핑하기 어렵고, Service가 HTTP 응답 문자열을 만든다.
- 면접용 문장: "예외는 에러 코드와 공통 처리로 일관되게 매핑하고, Service는 HTTP 문자열이 아니라 유스케이스 결과를 반환해야 합니다."

### 5. static Util에 비즈니스 로직과 부가 작업이 섞임

- 문제 유형: 리팩토링 방향 제안
- 코드 위치: `PointRefundUtil`
- 왜 핵심인지: `@Component`처럼 보이지만 static 호출이라 Bean 주입 의미가 약하고, 환불 차단 정책과 알림 발송 책임이 Util에 섞여 있다.
- 면접용 문장: "Util에 정책과 부가 작업이 들어가면 테스트와 변경이 어려우므로 Policy, Notifier 같은 역할로 분리하는 것이 좋습니다."

## 리팩토링된 코드 보기

- `refactoring-plan.md`
- `solution/README.md`
- `solution/src/main/java/com/example/point/refactoring/PointRefundController_Refactored.java`
- `solution/src/main/java/com/example/point/refactoring/PointRefundService_Refactored.java`
- `solution/src/main/java/com/example/point/refactoring/PointPaymentRepository_Refactored.java`
- `solution/src/main/java/com/example/point/refactoring/InMemoryPointPaymentRepository_Refactored.java`
- `solution/src/main/java/com/example/point/refactoring/PointRefundPolicy_Refactored.java`
- `solution/src/test/java/com/example/point/refactoring/PointRefundService_RefactoredTest.java`

## 사용자 답변 원문

```text
### 1. Controller
-"CANCEL" -> 이부분은 enum으로 사용하면 좋을 것 같고 
- 파라미터 타입 : PointPayment 함수에 PointPayment-> Entity로 들어왔어 PointRefundRequest이걸로 들오아야한단 말이지? 
- new 생성해서 DI의 의존성 주입에 서 문제가 생김 : private final PointRefundService pointRefundService /final를 사용해서 해야함 
- 위의 문제 1.spring이 관리하는 bean을 쓰지 않는다. 실제 spring 환경이라면 @서비스, 트랜잭션, aop, 프록시 spring기능이 제대로 적용되지 않을 수 있다. 
- 2. 테스트가 어려워진다. 테스트에 가짜 서비스, 목서비서, 넣고싶은데 이미 컨트롤러에서 new 해버려서 3. 컨트롤러가 객체 생성 책임까지 다 가져ㄴ다. 컨트롤러는 http요청/응답을 처리하느 역활이 집중을 ㅎ해야하는데, service는 어떻게 만들지 알게 도니다. 그려먼 결합도가 높아진다. 
- 응답 타입: pubicd PoinPay~~-> public ResponseEntity<PointRefundResponse> 변경 현재 문제는 PointPayment 이걸 그대로 반환하기 때문에
- 문자열 retrun : "200 OK: []";이건 문자열이 실제 응답에 대한 답이 아닌다. 변경하려면 -> "ResponseEntity.ok" 수정해야한다. 
### 2. Service
- 1.Service가 응답 포멧을 직접 만들고 있다 : return "200 OK: " + result; -> 문자열보 반환을 한다. 이것도 "ResponseEntity.ok" 수정해야한다. 
- 2. DI 의존성 문제 : private PointPaymentRepository pointPaymentRepository = new PointPaymentRepository();-> 생성자 new 바로 써서 final로 해줘야함 의존성 역전이되는것 
- 3. public PointPayment refund(PointPayment payment) { -> PointPayment이게 PointRefundRequest 로 바뀌어야 하고
- 4. @Trnasaction이 없음: 데이터를 조회하고 변경하는데, 문제가 생기면? (궁금한점 이분 트랜젝션은 serviec에 있어야하는거지? 로직적으로 풀어내고 레파지토리를 조회하면서 값을 바꾸는거니까?)
- 5. 예외 처리에 일관성 없음 :  throw new RuntimeException("weekend refund blocked");,쓰는 곳도 있고        PointRefundUtil.sendRefundNotice(savedPayment.userId, "refund ok: " + savedPayment.amount); 예외처리에 일관서이 없음
- 6. N+1 의 문제 : 42줄에서 findAll 을 했는데,  46번줄에서 findById 을 다시 하고 있다. 그래서 N+1의 문제 

### 3. Repository
- 1. NPE 발생우려 : ? Optional 변경을 해야함
- 2. static final List : 동시성의 문제가 생길 수 있다
- 3. save가 호출될때 add를 함 : 존대하는 데이터를 업데이트 
### 4. Util
- @Compenet 는 Spring이 객채를 만들어서 DI로 주입해주는것 근데 static 객체 자체가 피료 ㅇ벗이 클래스 명으로 바로 호출이 되는것 static이 잇어서 @Compnent가 의미가 없음
- 로그 출력방식에서 println을 사용하는게 아니라 , Logger를 사용해서 Slf4j를 사용해서 써야한다.

### 5. DTO/Entity

- PointRefundRequest가 외부에서 받아오는 DTO 역활을하고 잇어서 ,@AllArgsConstructor,@Getter ,  @Setter사용하면 좋을 것같고
- PointPayment 는 Entity 인데 @Builder를 사용해서 보면 코드 관리가 더 쉬울 것 같아. 

### 6. Exception
- RuntimeException을 그대로 상속받아서 바로 메시지로 뱉어 그러면 어떤 종류의 오류인지 정확하게 모를꺼야 문자열을 직접 비교해야할 것 같은데?? 그럼http상태코드를 직접 맵핑하는것도 방법인것 같은데 

### 7. Test
- Assertions.assertEquals 이런것도 없고 ... mockc객체를 만들어서 하는것도 없고 
- @Test 이것도 없고 
```

## 채점

- 학습 점수: 78점
- 실제 면접관 기준 점수: 70점

### 잘한 점

- Controller에서 Entity 직접 입력/반환 문제를 잡았다.
- 직접 `new` 생성이 DI, 테스트, AOP 적용에 불리하다는 점을 잘 짚었다.
- Service의 트랜잭션 경계, 문자열 응답, 반복 조회, 예외 일관성 문제를 잡았다.
- Repository의 `null`, `static final List`, `save`가 `add`만 하는 문제를 잡았다.
- Util의 static 호출과 `@Component` 의미 약화, `System.out.println` 문제를 잡았다.
- Test에 `@Test`, assertion이 없다는 점을 정확히 봤다.

### 보완할 점

- Service에서 `ResponseEntity.ok`로 고치자는 말은 위험하다. `ResponseEntity`는 Controller의 HTTP 응답 표현이고, Service는 도메인 결과나 응답 DTO를 반환하는 쪽이 낫다.
- `N+1`은 맞는 방향이지만, 이 문제는 JPA 연관관계 lazy loading이 아니므로 "N+1 형태의 반복 Repository 조회"라고 말해야 안전하다.
- DTO에 `@Getter`, `@Setter`, `@AllArgsConstructor`를 붙이면 좋다는 답변은 본질이 아니다. 핵심은 DTO validation, 필요한 필드만 받기, 서버 관리 상태를 클라이언트에게 받지 않기다.
- Entity에 `@Builder`를 쓰면 관리가 쉬워진다는 말도 핵심 개선이 아니다. public field 제거, 상태 전이 메서드, 불변성, 캡슐화가 먼저다.
- 가장 큰 본질인 "조회 실패 시 요청 객체를 그대로 환불 대상으로 사용하는 문제"를 명확히 분리하지 못했다.

### 위험한 표현

- "Service도 ResponseEntity.ok로 수정"  
  -> Service는 HTTP 응답이 아니라 유스케이스 결과를 반환한다고 말해야 한다.
- "의존성 역전이 되는 것"  
  -> 여기서는 의존성 역전 원칙보다는 직접 생성으로 인한 DI 위반, 강한 결합, 테스트 어려움이 정확하다.
- "Entity인데 @Builder를 사용"  
  -> Builder는 선택지일 뿐이고 Entity 책임/캡슐화 개선의 본질은 아니다.
- "N+1의 문제"  
  -> "JPA N+1"로 들리면 과장이다. 반복 Repository 조회라고 보정해야 한다.

### 모호한 표현

- "문제가 생기면?"  
  -> "상태 변경과 저장이 하나의 유스케이스 안에서 원자적으로 처리되어야 하므로 Service public 메서드에 트랜잭션 경계를 둬야 한다"로 고쳐야 한다.
- "http상태코드를 직접 맵핑하는것도 방법"  
  -> "예외 타입/에러 코드와 ControllerAdvice 같은 공통 예외 처리에서 HTTP 상태로 매핑한다"가 더 정확하다.
- "Optional 변경"  
  -> "Repository의 조회 결과처럼 없을 수 있는 반환 타입에 Optional을 쓰고, Service에서 도메인 예외로 변환한다"까지 말해야 한다.

### 놓친 본질

- `if (savedPayment == null) savedPayment = payment;` 때문에 없는 결제도 요청 객체만 있으면 환불될 수 있다.
- 환불 대상 결제가 요청한 사용자 소유인지 검증하지 않는다.
- `payment.reason.contains("VIP")`로 금액을 늘리는 이상한 정책이 들어가 있다. 클라이언트 입력 문자열로 환불 금액을 바꾸면 안 된다.
- `PointRefundUtil.calculateFee`는 사용되지 않고, 정책 위치도 애매하다.
- `findAll()`이 내부 mutable List를 그대로 반환한다.
- 알림 발송 실패 시 환불을 롤백할지, 환불은 성공시키고 알림만 재시도할지 정책이 없다.

## 반드시 잡았어야 하는 문제

- 문제: Entity 직접 Request/Response 사용
  - 유형: 명백한 안티패턴
  - 코드 위치: `PointRefundController.refund(PointPayment payment)`
  - 왜 문제인지: 클라이언트가 서버 내부 상태까지 보낼 수 있고 API 계약이 내부 모델과 결합된다.
  - 실무 영향: 금액/상태 조작, API 변경 영향 확대, FE/QA와 계약 불명확.
  - 면접용 문장: "요청은 `PointRefundRequest`, 응답은 `PointRefundResponse`로 분리해야 합니다."

- 문제: Service가 없는 결제를 요청 객체로 대체
  - 유형: 돌아가지만 위험한 코드
  - 코드 위치: `if (savedPayment == null) savedPayment = payment;`
  - 왜 문제인지: 결제가 존재하지 않아도 환불 처리가 진행될 수 있다.
  - 실무 영향: 정산 오류, 포인트 부정 환불, 데이터 정합성 훼손.
  - 면접용 문장: "조회 실패는 요청 객체로 보정할 게 아니라 `PAYMENT_NOT_FOUND` 예외로 중단해야 합니다."

- 문제: 트랜잭션 경계 누락
  - 유형: 개선 필요 코드
  - 코드 위치: `PointRefundService.refund`
  - 왜 문제인지: 실제 DB를 사용하는 Repository라면 조회, 상태 변경, 저장이 하나의 유스케이스인데 원자성이 보이지 않는다.
  - 실무 영향: 저장과 알림/예외 흐름이 섞일 때 부분 실패를 설명하기 어렵다.
  - 면접용 문장: "현재 예제는 인메모리 코드지만, 실제 DB 기반 Repository라면 상태 변경 유스케이스는 Service 계층 public 메서드에 트랜잭션 경계를 두는 것이 일반적입니다."

- 문제: 반복 Repository 조회
  - 유형: 성능 안티패턴
  - 코드 위치: `getRefundHistory`
  - 왜 문제인지: 전체 조회 후 반복문에서 단건 조회를 반복한다.
  - 실무 영향: 데이터 증가 시 DB 호출이 급증한다.
  - 면접용 문장: "userId와 status 조건 조회로 Repository에서 한 번에 가져오도록 바꿔야 합니다."

- 문제: 예외 처리 정책 부재
  - 유형: 명백한 안티패턴
  - 코드 위치: `RuntimeException`, `PointRefundException`
  - 왜 문제인지: 커스텀 예외를 둔 방향 자체는 나쁘지 않지만, 에러 코드와 HTTP 상태 매핑이 일관되지 않다.
  - 실무 영향: FE/QA/운영자가 실패 원인을 구분하기 어렵다.
  - 면접용 문장: "도메인 예외와 에러 코드를 정의하고 공통 예외 처리에서 HTTP 상태로 매핑해야 합니다."

## 문제 유형 분류

- 명백한 안티패턴: Entity 직접 입력/반환, 직접 `new` 생성, RuntimeException 직접 사용, `System.out.println`, 실행되지 않는 테스트
- 돌아가지만 위험한 코드: 조회 실패 시 요청 객체 사용, 문자열 상태값, `save`가 항상 add, static mutable List
- 개선하면 좋은 코드: Response DTO 분리, enum 사용, Optional 반환, Logger 사용
- 트레이드오프 판단: 알림 실패를 환불 실패로 볼지 별도 재시도할지
- 리팩토링 방향 제안: Policy/Notifier 분리, Repository 조건 조회, Service 트랜잭션 경계 설정

## 이번 문제 개념 키워드

### DI와 직접 생성

- 위치: `new PointRefundService()`, `new PointPaymentRepository()`
- 개념 설명: Spring Bean을 생성자 주입으로 받지 않고 직접 생성하면 DI, 테스트 대체, AOP 적용이 어려워진다.
- 면접용 문장: "직접 new로 생성하면 Spring이 관리하는 Bean이 아니므로 생성자 주입으로 바꾸는 것이 적절합니다."

### DTO/Entity 분리

- 위치: `PointPayment`를 Request/Response로 사용
- 개념 설명: DTO는 API 계약이고 Entity/도메인 모델은 내부 상태다.
- 면접용 문장: "JPA 여부와 무관하게 내부 모델을 외부 API에 직접 노출하면 결합도가 높아집니다."

### Transaction boundary

- 위치: `PointRefundService.refund`
- 개념 설명: 실제 DB를 사용하는 Repository라면 조회, 상태 변경, 저장은 하나의 유스케이스로 묶여야 한다.
- 면접용 문장: "현재 예제는 인메모리 코드지만, 운영 저장소라면 트랜잭션 경계는 보통 상태 변경 Service 메서드에 둡니다."

### Optional

- 위치: `PointPaymentRepository.findById`
- 개념 설명: 조회 결과가 없을 수 있음을 타입으로 드러내는 반환 방식이다.
- 면접용 문장: "Repository 조회 결과는 `Optional`로 표현하고 Service에서 도메인 예외로 변환할 수 있습니다."

### 반복 Repository 조회

- 위치: `findAll` 후 `findById`
- 개념 설명: 실제 DB라면 1번 목록 조회 뒤 N번 단건 조회가 추가된다.
- 면접용 문장: "JPA N+1이라고 단정하지 말고 N+1 형태의 반복 Repository 조회라고 표현하겠습니다."

### Util 책임

- 위치: `PointRefundUtil`
- 개념 설명: static Util에 정책과 부가 작업이 들어가면 테스트와 변경이 어려워진다.
- 면접용 문장: "순수 계산 유틸이 아니라 정책/알림 책임이면 별도 컴포넌트로 분리해야 합니다."

## 안티패턴 지침화 후보

### 1. Controller가 Entity를 직접 받거나 반환

- 문제 유형: 명백한 안티패턴
- 코드 위치: `PointRefundController.refund`
- 왜 위험한지: 클라이언트가 내부 상태를 조작할 수 있고 API 계약이 내부 모델과 결합된다.
- 좋은 코드 방향: Request/Response DTO 분리, `@Valid` 검증.
- AI 지침 후보: Controller API에는 내부 Entity를 직접 노출하지 말고 DTO를 사용하라.
- 반복 출제 여부: 반복 출제

### 2. 조회 실패를 요청 객체로 보정

- 문제 유형: 돌아가지만 위험한 코드
- 코드 위치: `savedPayment = payment`
- 왜 위험한지: 없는 결제를 정상 환불할 수 있다.
- 좋은 코드 방향: 조회 실패는 명확한 예외로 중단.
- AI 지침 후보: 서버 저장 상태 변경은 클라이언트 입력이 아니라 저장소에서 조회한 기존 상태를 기준으로 처리하라.
- 반복 출제 여부: 반복 출제

### 3. 반복 Repository 조회

- 문제 유형: 성능 안티패턴
- 코드 위치: `findAll` + 반복 `findById`
- 왜 위험한지: 실제 DB에서는 데이터 수만큼 추가 조회가 발생한다.
- 좋은 코드 방향: 조건 조회 메서드로 한 번에 조회.
- AI 지침 후보: 목록 조회 후 루프 내부에서 단건 Repository 조회를 반복하지 말라.
- 반복 출제 여부: 반복 출제

### 4. static mutable 저장소

- 문제 유형: Java 안티패턴
- 코드 위치: `static final List`
- 왜 위험한지: final은 참조만 고정하고 내부 변경은 가능해 동시성/테스트 격리 문제가 생긴다.
- 좋은 코드 방향: 실제 서비스는 DB/트랜잭션, 학습용은 인스턴스 저장소나 thread-safe 구조 사용.
- AI 지침 후보: 상태 저장용 static mutable collection을 운영 코드처럼 만들지 말라.
- 반복 출제 여부: 반복 출제

## 보완 답변

### Controller

`PointPayment`를 직접 Request/Response로 사용하고 있고, `PointRefundService`를 `new`로 직접 생성합니다. Controller는 HTTP 요청/응답과 경계 검증에 집중해야 하므로 `PointRefundRequest`와 `PointRefundResponse`로 분리하고, Service는 생성자 주입으로 받아야 합니다. `"CANCEL"` 같은 문자열 상태값도 enum으로 분리하는 것이 좋습니다.

### Service

Service는 클라이언트가 보낸 `PointPayment` 상태를 신뢰하고 있습니다. 특히 결제가 조회되지 않으면 요청 객체를 그대로 사용해 환불을 진행하는데, 이는 없는 결제를 환불할 수 있는 심각한 문제입니다. Service는 결제 ID로 기존 결제를 조회하고, 소유자/상태/정책을 검증한 뒤 상태 변경과 저장을 하나의 트랜잭션 경계에서 처리해야 합니다. Service가 `"200 OK"` 문자열이나 `ResponseEntity`를 만들기보다 유스케이스 결과 DTO를 반환하는 것이 좋습니다.

### Repository

`findById`가 `null`을 반환하고, `findAll`이 내부 List를 그대로 노출합니다. 조회 결과가 없을 수 있는 메서드는 `Optional`로 표현하고, 환불 이력은 `findRefundedByUserId` 같은 조건 조회 메서드로 제공하는 것이 좋습니다. `save`도 항상 `add`가 아니라 id 기준으로 저장/갱신해야 합니다.

### Util

`@Component`처럼 보이지만 메서드가 static이라 Bean으로 주입받는 구조가 아닙니다. 또한 환불 차단 정책, 수수료 계산, 알림 발송이 한 Util에 섞여 있습니다. 환불 정책은 `PointRefundPolicy`, 알림은 `RefundNotifier`처럼 분리하는 것이 더 테스트하기 좋습니다.

### DTO/Entity

DTO는 Lombok을 붙이는 것이 핵심이 아니라, 외부에서 필요한 필드만 받고 검증 규칙을 명확히 두는 것이 핵심입니다. Entity/도메인 모델은 public field를 피하고 상태 전이를 메서드로 제한해야 합니다.

### Exception

문자열 메시지만 가진 `RuntimeException`은 실패 원인을 구조적으로 구분하기 어렵습니다. `PointRefundErrorCode`를 두고 공통 예외 처리에서 HTTP 상태로 매핑하는 구조가 더 낫습니다.

### Test

현재 테스트는 `@Test`가 없고 성공 케이스만 수동으로 확인합니다. 없는 결제, 다른 사용자 결제, 이미 환불된 결제, 환불 이력 조회 같은 실패/경계 케이스를 검증해야 합니다.

## 면접관 예상 꼬리질문

### 1. Service에서 `ResponseEntity.ok`를 반환하면 왜 안 좋나요?

- 의도: 계층 책임을 아는지 확인
- 답변 방향: `ResponseEntity`는 HTTP 응답 표현이므로 Controller 책임이고, Service는 도메인 결과나 응답 DTO를 반환해야 한다.

### 2. 이 코드는 정확히 JPA N+1 문제인가요?

- 의도: 용어를 과장하지 않는지 확인
- 답변 방향: JPA 연관관계 lazy loading에서 말하는 N+1은 아니지만, DB 기반 Repository라면 N+1 형태의 반복 조회 문제가 될 수 있다.

### 3. `Optional`은 언제 쓰는 게 적절한가요?

- 의도: Optional 오남용 여부 확인
- 답변 방향: Repository 조회 결과처럼 값이 없을 수 있는 반환 타입에 제한적으로 쓰고, Entity 필드나 DTO 필드에 남발하지 않는다.

### 4. 알림 발송 실패 시 환불도 실패해야 하나요?

- 의도: 트랜잭션과 부가 작업 분리 감각 확인
- 답변 방향: 도메인 정책에 따라 다르다. 환불을 롤백할지, 환불은 성공시키고 알림만 재시도할지는 정책 확인 후 결정해야 한다.

### 5. `static final List`에서 final인데 왜 동시성 문제가 있나요?

- 의도: Java 기본기 확인
- 답변 방향: final은 참조 재할당만 막고 List 내부 변경은 막지 못하므로 mutable shared state 문제가 생긴다.

## 다음 문제에서 다시 볼 약점

- Service에서 HTTP 응답 타입을 만들지 않는다는 계층 책임
- DTO 개선을 Lombok이 아니라 API 계약/검증 책임으로 설명하기
- JPA N+1과 반복 Repository 조회를 구분해서 말하기
- 조회 실패를 요청 객체로 보정하는 문제를 더 강하게 잡기
- 알림/로그 같은 부가 작업 실패 정책 말하기

## 시니어 개발자의 피드백 (면접관 관점)

### 인정되는 답변

이번 답변은 이전보다 확실히 넓게 봤습니다. Controller, Service, Repository, Util, DTO/Entity, Exception, Test를 순서대로 훑었고, DI, Entity 직접 노출, 트랜잭션, 반복 조회, null, static List, 테스트 부실을 잡은 점은 면접에서 인정됩니다.

### 실제 면접에서 부족하게 들리는 지점

다만 답변이 아직 "문제 냄새를 찾았다" 수준에서 멈추는 부분이 있습니다. 면접관은 "그래서 정확히 어떤 장애가 생기고, 어느 계층에서 어떻게 고칠 것인가"를 봅니다. 특히 Service에서 `ResponseEntity.ok`를 쓰자는 말은 계층 책임을 잘못 이해한 것으로 들릴 수 있습니다. DTO에 Lombok이나 Builder를 붙이자는 답변도 본질보다 도구에 먼저 가는 느낌입니다.

### 다음 리뷰에서 먼저 확인할 것

다음에는 문제를 보면 먼저 "클라이언트가 보낸 값을 서버가 신뢰하고 있는가", "없는 데이터를 요청 객체로 보정하고 있지 않은가", "Service가 HTTP 응답을 만들고 있지 않은가"를 먼저 확인하세요. 이번 답변은 합격권에 가까워졌지만, 용어와 계층 책임을 더 정확히 말해야 실제 면접에서 안정적으로 들립니다.

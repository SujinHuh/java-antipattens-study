# 005. Delivery Address Legacy Review - 내 답변

## 내 답변

각 항목에는 가능하면 `문제점`, `왜 문제인지`, `실무 영향`, `개선 방향`을 같이 적어보세요.

### 1. Controller
- 1. HTTP 응답 표현 안티패턴 : return Map.of("status", 200, "message", "empty request"); -> 요청이 nulldlaus 클라이언트 잘못인데 , 200성공을 반환하고 있고 실패인데 성공처럼 보임
- 1. Map<String, Object> changeAddress 응답 구조 만드는 것도 문제 : ResponsEntity<응답 DTO> 이렇게 되어야함., 아니면 DeliveryAddressRequest 가 들어가도됨
- 2. 단일 책임의 원칙 SRP : if ("URGENT".equals(request.memo)) -> 비지니스로직이 controller에있는건 단일 책임의 원칙 + 계층책임의 원칙이있음
- 3. DI 위반 :     private final DeliveryAddressService deliveryAddressService = new DeliveryAddressService();
- 4. DTO 불변성 위반 : request 가 들어온걸 변경을 직접하고 있어. 이렇게 하면안되는데? 

#### 힌트

21번 힌트 제공 가이드 기준으로, 정답 전체가 아니라 Controller를 더 깊게 보기 위한 힌트만 적습니다.

##### 1. 위치 힌트

- `changeAddress`의 반환 타입이 API 응답 계약을 명확히 드러내는지 보세요.
- `request == null`일 때 성공 응답처럼 보이는 구조인지 확인하세요.
- Controller가 요청 DTO를 그대로 읽기만 하는지, 직접 수정하고 있는지 보세요.
- Controller가 Service를 어떻게 얻고 있는지 보세요.

##### 2. 개념 힌트

- Controller는 HTTP 요청/응답, 입력 경계 검증, 상태 코드 표현에 집중하는 계층입니다.
- `Map<String, Object>` 응답은 빠르게 만들 수 있지만, API 응답 스펙이 불명확해질 수 있습니다.
- `ResponseEntity<DeliveryAddressResponse>`는 응답 상태와 응답 Body를 명확히 표현할 때 사용할 수 있습니다.
- 요청은 `DeliveryAddressRequest`, 응답은 별도 `DeliveryAddressResponse`로 분리해서 생각하세요. `DeliveryAddressRequest`를 응답으로 쓰는 것은 적절하지 않습니다.

##### 3. 장애/실무 영향 힌트

- 실패 상황을 200으로 반환하면 FE/QA는 실패를 성공으로 처리할 수 있습니다.
- 운영 지표에서도 실패율이 드러나지 않을 수 있습니다.
- Controller가 `request.priority`를 직접 바꾸면 사용자가 보낸 값과 서버가 계산한 값이 섞여 추적이 어려워집니다.
- Service를 직접 `new`로 만들면 테스트에서 Mock Service로 대체하기 어렵고, Spring Bean/AOP 적용도 어려워질 수 있습니다.

##### 4. Controller 개선 힌트

- Controller는 `Map<String, Object>`보다 명확한 응답 DTO와 HTTP 상태를 반환하는 쪽이 좋습니다.
- Controller는 `request.priority = 1`처럼 요청 객체를 직접 변경하지 않는 쪽이 좋습니다.
- Controller는 Service를 직접 `new`로 만들지 않고 생성자 주입으로 받는 쪽이 좋습니다.
- Controller는 null 요청을 성공 응답처럼 처리하지 않고, 실패 응답으로 구분해야 합니다.

##### 5. 면접 답변 힌트

```text
Controller에서는 실패 상황을 200 OK Map으로 반환하고 있어 API 계약이 불명확합니다.
또한 URGENT 메모를 보고 priority를 바꾸거나 요청 DTO를 직접 변경하고 있어 Controller의 책임이 커져 있습니다.
Controller는 요청/응답 경계에 집중하고, Service는 생성자 주입으로 받아야 테스트와 Spring AOP 적용에 유리합니다.
```

### 2. Service
- 1. DI 위반 new 제거: private final DeliveryAddressRepository deliveryAddressRepository = new DeliveryAddressRepository(); -> 이것도 new 빼고 사용하면되
- 2. DTO-> Entity 변환위치 : request-> 들어온 값을 entity 로 변경해줘야해 그래야 계층적으로 데이터를 분리할수 있어.
- 2. @Builder로 안전하게 Entity생성: Entity로 아니면 @Builder같은 걸로 써서 하면될텐데 : current = new DeliveryAddress(request.orderId, request.userId, request.zipCode, request.address, "READY", false); 
- 3. if ("DELIVERING".equals(current.status)) {-> 이부분도 DELIVERING: 이넘을 사용해서 오타가 나도 누락이 나지 않도록 
- 4. 데이터 정합성의 위험-> null일때 성공하도록 보이는 패턴? 문제 :       if (current == null) {
     current = new DeliveryAddress(request.orderId, request.userId, request.zipCode, request.address, "READY", false);
     } 
- 5. getChangeHistory 에서 findall을 호출하고 findByOrderId 한번 더 호출을 했어.. 
- 6. 문자열을 이넘으로 해서 비교하는게 좋을 것 같아 

#### 힌트

21번 힌트 제공 가이드 기준으로, 정답 전체가 아니라 Service를 더 깊게 보기 위한 힌트만 적습니다.

##### 1. 위치 힌트

- `DeliveryAddressService`가 `DeliveryAddressRepository`를 어떻게 얻고 있는지 보세요.
- `findByOrderId(request.orderId)` 결과가 없을 때 실패로 처리하는지, 요청값으로 정상 흐름을 이어가는지 보세요.
- `current.status`, `"READY"`, `"DELIVERING"`, `"ADDRESS_CHANGED"`처럼 상태값을 문자열로 직접 비교/대입하는 부분을 보세요.
- `getChangeHistory`에서 전체 조회 후 반복문 안에서 다시 Repository를 호출하는지 보세요.
- Service가 `Map.of("status", 200, "data", current)`처럼 HTTP 응답 형태를 직접 만드는지 보세요.

##### 2. 개념 힌트

- Service는 Repository를 직접 `new`로 만들기보다 생성자 주입으로 받아야 합니다.
- Service가 Entity를 조회하고 변경하는 것은 자연스럽지만, 조회 실패 상황에서 클라이언트 요청값만으로 Entity를 새로 만들어 정상 처리하면 안 됩니다.
- `@Builder`는 생성자 가독성을 높일 수는 있지만, 조회 실패를 정상 처리하는 문제를 해결하지는 못합니다.
- 제한된 상태값은 문자열보다 enum으로 표현하는 것이 오타와 잘못된 상태 저장을 줄이는 데 유리합니다.
- Service는 `ResponseEntity`나 `status/message/data` Map 같은 HTTP 응답 구조보다 비즈니스 결과 DTO를 반환하는 쪽이 계층 책임에 맞습니다.

##### 3. 장애/실무 영향 힌트

- 조회 실패를 요청값 기반 Entity 생성으로 숨기면 존재하지 않는 주문의 배송지가 변경된 것처럼 처리될 수 있습니다.
- 서버에 저장된 사용자/주문/배송 상태가 아니라 클라이언트가 보낸 값을 기준으로 처리하면 권한 검증과 데이터 정합성이 깨질 수 있습니다.
- 문자열 상태값은 오타나 허용되지 않은 상태값이 저장되어도 컴파일 타임에 잡기 어렵습니다.
- 전체 목록 조회 후 반복문 안에서 다시 조회하면 데이터가 많아질수록 조회 비용이 커집니다.
- Service가 HTTP 응답 구조를 만들면 웹 계층에 묶여 테스트와 재사용이 어려워집니다.

##### 4. Service 개선 힌트

- Repository는 생성자 주입으로 받는 구조가 좋습니다.
- `findByOrderId` 결과가 없으면 요청값으로 Entity를 만들기보다 예외로 분리하는 방향을 먼저 생각하세요.
- 상태값은 `DeliveryStatus` 같은 enum으로 분리할 수 있습니다.
- 이력 조회는 필요한 조건으로 한 번에 조회하거나, 목적에 맞는 Repository 메서드를 두는 방향이 좋습니다.
- Service 반환은 `Map<String, Object>`보다 `DeliveryAddressResponse` 같은 결과 DTO가 적절합니다.

##### 5. 면접 답변 힌트

```text
Service에서 Repository를 직접 new로 생성하고 있어 DI를 우회하고 테스트 대체가 어렵습니다.
또한 배송지 조회 결과가 없을 때 요청값으로 Entity를 만들어 정상 처리하고 있는데,
배송지 변경은 서버에 저장된 주문/배송지 상태를 기준으로 해야 하므로 조회 실패는 예외로 분리해야 합니다.
상태값도 문자열로 비교하고 있어 enum으로 제한하는 편이 안전하고,
Service가 status/data Map을 반환하는 것은 HTTP 응답 책임이 Service로 넘어간 구조라 결과 DTO 반환으로 분리하는 것이 좋습니다.
```
### 3. Repository
- 1. static에서 add 하게되면 save를 계속 호출하게되고 데이터를 호출하게되. -> 
- 2. private static final List<DeliveryAddress> addresses = new ArrayList<>(); -> new를 하는게 아니라.. 선언헤서 바로 쓰는거 이거야., 만약에 list애서 내부 데이터를 직접 변경하면 그것도 문제가 생긴다. 
- 3. return null; -> Optional 반환해서 null에 문제가 없도록해야한다. 
- 1. 저장 로직 오류 : save()가 항상 add()-> 중복 데이터가 쌓임 2. java 안티패턴 : static fianl List-> 동시성, 테스트 경리 문제 3.NPE위험 ,Optional 필요 : finadByOrderId()rk null로 반환됨, 4. 성능 개선 포인트 : for전체 탐색 -> Map 으로 개선 가능

#### 조언

Repository에서 잡은 방향은 좋습니다. 다만 면접에서는 아래처럼 표현을 조금 더 정확하게 바꾸는 것이 안전합니다.

##### 1. `static final List`의 핵심 문제

```java
private static final List<DeliveryAddress> addresses = new ArrayList<>();
```

- `final`이 문제라는 뜻은 아닙니다.
- `final`은 `addresses` 변수가 다른 List를 가리키지 못하게 할 뿐, List 내부 데이터 변경까지 막지는 않습니다.
- 문제는 `static`으로 공유되는 mutable List를 저장소처럼 쓰면서 테스트 격리, 동시성, 데이터 초기화 문제가 생길 수 있다는 점입니다.

면접용 문장:

```text
static final List는 참조 재할당만 막을 뿐 내부 변경은 가능합니다.
Repository 저장소로 공유 mutable List를 쓰면 테스트 간 데이터가 섞이고, 동시 요청에서 일관성이 깨질 수 있습니다.

static final List는 참조 재할당만 막을 뿐 내부 변경은 가능하다. rpostitory는 저장소로 공유 변경가능한 리스트를 쓰면 데이터가 섞이고 동시 요청에서 일관성이 깨질 수 잇ㄷ. 
```

##### 2. `save()`가 항상 `add()`만 하는 문제

```java
public DeliveryAddress save(DeliveryAddress address) {
    addresses.add(address);
    return address;
}
```

- 배송지 변경이라면 기존 `orderId`의 데이터를 갱신해야 할 가능성이 큽니다.
- 그런데 항상 `add()`만 하면 같은 주문의 배송지 데이터가 여러 개 쌓일 수 있습니다.
- 이후 `findByOrderId`가 어떤 데이터를 반환하는지 불명확해집니다.

면접용 문장:

```text
변경 유스케이스인데 save가 항상 add만 수행하면 같은 orderId의 데이터가 중복 저장될 수 있습니다.
저장 로직이 생성인지 수정인지 명확히 분리하거나, orderId 기준으로 기존 데이터를 갱신해야 합니다.

변경 유스케이스인데 save가 항상 add만 수행하면 같은 orderId의 데이터가 중복 저장될 수 있ㄷ. 
저장 로직이 생성인지 수정인지 명확히 분리하거나 , oredeIdr기준으로 기존데이터를 갱신해야한다. 

저정로직인지 변경 로직인지 기준을 명확히 해야한다. orderId 기준으로 기존데이터를 갱신해야한다. 
```

##### 3. `findByOrderId()`의 `null` 반환

```java
return null;
```

- Repository가 `null`을 반환하면 호출하는 Service가 매번 null 처리를 놓칠 수 있습니다. // servier가 매번 null처리를 놓칠 수 있다. 
- 이 문제에서는 Service가 null을 실패로 처리하지 않고 새 Entity 생성으로 숨겨버렸습니다. // service가 실패로 처리 하지 않고 새 entity로 생성을 숨겨버렸다. 
- `Optional<DeliveryAddress>`로 조회 실패 가능성을 타입에 드러내는 방향이 더 낫습니다.// Option<> 조회 실패 간으성을 타입에 드러내는 방향이 낫다.

면접용 문장:

```text
조회 실패 가능성이 있는 메서드가 null을 반환하면 NPE나 실패 흐름 누락으로 이어질 수 있습니다.
Optional로 실패 가능성을 드러내고, Service에서 orElseThrow로 명확히 처리하는 편이 좋습니다.
```

##### 4. `findAll()`이 내부 List를 그대로 반환하는 문제

```java
public List<DeliveryAddress> findAll() {
    return addresses;
}
```

- 내부 저장소 List를 그대로 반환하면 외부 코드가 Repository 내부 상태를 직접 변경할 수 있습니다.
- 최소한 복사본이나 읽기 전용 리스트를 반환하는 편이 낫습니다.

면접용 문장:

```text
Repository 내부 컬렉션을 그대로 반환하면 외부에서 저장소 상태를 직접 변경할 수 있어 캡슐화가 깨집니다.
필요한 조회 조건을 Repository 메서드로 제공하거나, 최소한 방어적 복사본을 반환해야 합니다.
```
// 레파지토리 내부 컬렉션을 그대로 반환하면 외부에서 저장소 상태를 직접 변경할 수 잇어서 캡슐화가 깨진다. 필요한 조회 조건을 레파티토리 메서드로 제공하거나 , 카피해서 제공해야함

##### 5. 전체 탐색과 성능 표현

- 지금 코드는 인메모리 List라서 `Map`으로 개선할 수 있다는 말은 가능하지만, 실제 DB 코드라면 `Map`이 아니라 조회 조건, 인덱스, 실행계획 관점으로 말하는 것이 더 자연스럽습니다.
- 이 문제에서는 "JPA N+1"이라고 단정하기보다 "반복 Repository 조회와 전체 탐색으로 인한 비효율"이라고 표현하는 것이 안전합니다.

면접용 문장:

```text
현재 Repository는 전체 List를 순회해 조회하므로 데이터가 많아지면 비효율이 커집니다.
인메모리 구현이라면 orderId 기준 Map을 고려할 수 있고, 실제 DB라면 조건 조회와 인덱스 관점에서 개선해야 합니다.
```


### 4. Util
- 1. return address.contains("TEST") || address.contains("BLOCK"); -> 이넘으로 해서 오타가 안나도록하는거
- 2. System.out.println("[DELIVERY] " + message); -> loggin으로 하는게 좋을 것 같아. 
- 3. 비즈니스 정책의 문제 :  return address.contains("TEST") || address.contains("BLOCK");
- 4. @컴포넌트인데 static 때문에 @Bean 주입을 받지 못해... public으로 해야할 것 같아. 모순의 문제야 

#### 조언

Util에서 잡은 핵심 방향은 맞습니다. 다만 `TEST`, `BLOCK`을 enum으로 바꾸는 것보다 더 먼저 말해야 할 핵심은 **순수 Util에 도메인 정책과 부가 작업이 들어가 있다**는 점입니다.

##### 1. `isBlockedAddress()` [Util 책임 / 도메인 정책 분리]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: Util 책임, Policy 분리, 테스트 가능성
- 왜 문제인지: `DeliveryAddressUtil`이라는 이름은 단순 변환/보조 기능처럼 보이지만, 실제로는 차단 주소 정책을 판단한다.
- 실무 영향: 차단 기준이 바뀌거나 DB/설정/관리자 정책과 연결되면 static Util로는 테스트와 변경이 어려워진다.
- 개선 방향: 주소 정규화는 Util로 둘 수 있지만, 차단 주소 판단은 `DeliveryAddressPolicy`나 `BlockedAddressValidator` 같은 컴포넌트로 분리한다.
- 면접용 문장: "주소 차단 여부는 단순 문자열 유틸이 아니라 도메인 정책이므로, static Util보다 Policy/Validator 컴포넌트로 분리하는 편이 테스트와 변경에 유리합니다."

##### 2. `@Component`와 static 메서드 혼재 [Spring Bean / DI]

- 문제 유형: 명백한 안티패턴
- 관련 개념: Spring Bean, static 의존성, DI
- 왜 문제인지: `// @Component`로 Bean처럼 보이게 해놓고 메서드는 모두 static이라 실제 주입받아 대체하거나 확장하기 어렵다.
- 실무 영향: 테스트에서 fake 정책이나 mock logger로 바꾸기 어렵고, 설정값이나 외부 의존성을 주입하기도 어렵다.
- 개선 방향: 순수 함수는 어노테이션 없이 static Util로 제한하고, 정책/알림/로그는 주입 가능한 컴포넌트로 분리한다.
- 면접용 문장: "`@Component`로 관리할 객체라면 static 호출이 아니라 주입받아 사용해야 하고, static으로 둘 거라면 순수 유틸 역할에만 제한하는 것이 좋습니다."

##### 3. `System.out.println` [운영/Logging]

- 문제 유형: 개선하면 좋은 코드
- 관련 개념: Logging, 운영 추적성
- 왜 문제인지: `System.out.println`은 로그 레벨, 포맷, 추적 ID, 수집 도구와 연결하기 어렵다.
- 실무 영향: 장애 발생 시 어떤 요청에서 실패했는지 추적하기 어렵고, 개인정보가 섞이면 통제하기도 어렵다.
- 개선 방향: 로깅 프레임워크를 사용하고, 감사 로그가 도메인 이벤트라면 별도 AuditLogger/Notifier로 분리한다.
- 면접용 문장: "운영 로그는 System.out이 아니라 로깅 프레임워크나 감사 로그 컴포넌트를 통해 레벨과 추적 정보를 관리해야 합니다."

### 5. DTO/Entity
- public인데 안에는 private로 감싸지 않았어 필드를말이야. 

#### 조언

방향은 맞습니다. 여기서는 `public field`만 말하면 조금 약하고, **Request DTO와 Entity가 각각 어떤 책임을 가져야 하는지**까지 같이 말해야 합니다.

##### 1. Entity public field [Java 안티패턴 / 캡슐화]

- 문제 유형: 명백한 안티패턴
- 관련 개념: 캡슐화, Entity responsibility
- 왜 문제인지: `DeliveryAddress`의 모든 필드가 public이라 Controller/Service/Util 어디서든 상태를 직접 바꿀 수 있다.
- 실무 영향: 상태 변경 규칙을 한 곳에서 통제하기 어렵고, 잘못된 상태값이나 누락된 검증이 쉽게 들어간다.
- 개선 방향: 필드는 private로 감추고, 상태 변경은 의미 있는 메서드나 Service 유스케이스를 통해 수행한다.
- 면접용 문장: "Entity 필드가 public이면 상태 변경을 통제할 수 없어 도메인 규칙이 흩어집니다. 필드는 감추고 의미 있는 변경 메서드로 상태를 바꾸는 편이 안전합니다."

##### 2. Request DTO 검증 부재 [Validation / API 계약]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: DTO validation, `@Valid`, API 계약
- 왜 문제인지: `DeliveryAddressRequest`에 필수값, 길이, 형식 검증 기준이 없다.
- 실무 영향: Service에서 null이나 잘못된 값이 뒤늦게 발견되고, 실패 원인이 API 계약에 명확히 드러나지 않는다.
- 개선 방향: Request DTO에는 `orderId`, `userId`, `zipCode`, `address` 같은 입력 형식 검증을 두고, 소유자/상태 전이 같은 도메인 검증은 Service/Domain에서 처리한다.
- 면접용 문장: "DTO는 외부 입력 계약이므로 형식 검증을 표현하고, Entity는 내부 상태 모델로 분리해야 API 변경과 도메인 변경을 따로 관리할 수 있습니다."

##### 3. Request DTO public field [DTO 설계 / 입력 객체 변경]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: DTO 불변성, 입력 계약, 계층 책임
- 왜 문제인지: `DeliveryAddressRequest`도 public field라 Controller나 Service에서 요청 객체를 쉽게 변경할 수 있다.
- 실무 영향: 사용자가 보낸 값과 서버가 계산한 값이 섞여 디버깅이 어려워지고, 입력 계약이 불명확해진다.
- 개선 방향: Request DTO는 불변 객체나 private field 기반으로 만들고, 서버가 결정하는 값은 별도 도메인 로직에서 계산한다.
- 면접용 문장: "Request DTO는 외부 입력 계약이므로 서버 내부 로직에서 임의로 변경하지 않는 편이 좋습니다. 서버 결정값은 요청 객체를 수정하기보다 Service나 도메인 정책에서 별도로 계산해야 합니다."


### 6. Exception
- Exception은 예외 클래스 자체만 보는것이 아니라 service, controller 에서 어떤 상황에 어떤 예외를 더니고, 그 예외가 공통 예외 처리에서 어떤 http 상태코드와 응답 dto로 변환되는지를 봐야한다. 이 코드에서는 runtimeException을 직접 던지고 있고, 커스텀 예외도 메시지만 가지고 있어 실패 원인과 api실패 계약이 명확하지 않다. 
- 1. throw new RuntimeException : 어떤 입력의 오류 타입인지 드나지 않음. http 400, 409 인지 모름. -> 실ㅠㅐ원인과 응답 정책이 드러나 있지 않음. 
- 2. throw new DeliveryAddressException : 커스텀 예외를 만들었다고 충분히 보이지 않고 그냥..메시지만 전달해주는걸로만 보임. 

#### 조언

이번 Exception 답변은 방향이 좋습니다. 특히 "예외 클래스만 보는 게 아니라 Service/Controller에서 던지고 응답으로 바뀌는 흐름을 봐야 한다"는 표현은 면접에서도 좋게 들립니다. 다만 `return Map.of("status", 200, ...)`와 `throw new RuntimeException(...)`은 같은 문제가 아니라 **연결된 두 문제**로 구분해서 말해야 합니다.

##### 1. `throw new RuntimeException(...)` [Exception / 실패 흐름]

- 문제 유형: 명백한 안티패턴
- 관련 개념: 구체 예외, 실패 원인 표현, HTTP 상태 매핑
- 왜 문제인지: 입력 누락 상황인데 범용 `RuntimeException`을 던져 실패 원인과 처리 정책이 타입으로 드러나지 않는다.
- 실무 영향: 공통 예외 처리에서 400 Bad Request로 내려야 할지, 500으로 봐야 할지 기준이 불명확하다.
- 개선 방향: 입력 검증 실패는 DTO validation 또는 `InvalidDeliveryAddressRequestException` 같은 구체 예외로 분리하고, 공통 예외 처리에서 400으로 매핑한다.
- 면접용 문장: "RuntimeException을 직접 던지면 실패 원인과 응답 정책이 드러나지 않습니다. 입력 오류, 조회 실패, 상태 충돌을 구체 예외와 에러 코드로 구분해야 합니다."

##### 2. `DeliveryAddressException`이 메시지만 가짐 [Exception / API 실패 계약]

- 문제 유형: 개선하면 좋은 코드
- 관련 개념: 도메인 예외, ErrorCode, ControllerAdvice
- 왜 문제인지: 커스텀 예외 이름은 있지만 메시지만 가지고 있어 차단 주소가 400인지 409인지 같은 API 정책이 드러나지 않는다.
- 실무 영향: FE/QA는 실패 응답 스펙을 예측하기 어렵고, Service마다 예외 메시지만 다르게 던지는 구조가 될 수 있다.
- 개선 방향: 예외에 에러 코드나 실패 분류를 포함하고, `ControllerAdvice`에서 일관된 ErrorResponse DTO와 HTTP 상태로 변환한다.
- 면접용 문장: "커스텀 예외가 있어도 메시지만 담고 있으면 실패 계약이 약합니다. 에러 코드와 공통 예외 매핑을 통해 클라이언트가 예측 가능한 실패 응답을 받아야 합니다."

##### 3. 실패를 200 Map으로 감싸는 문제와의 구분 [API 계약 / HTTP 상태코드]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: HTTP 상태코드, ErrorResponse DTO, FE/QA 협업
- 왜 문제인지: `return Map.of("status", 200, ...)`는 예외를 던지는 문제가 아니라 실패를 성공처럼 표현하는 응답 문제다.
- 실무 영향: 클라이언트는 실패를 성공으로 처리할 수 있고, QA와 운영 지표에서도 실패가 드러나지 않을 수 있다.
- 개선 방향: 실패는 예외 또는 명확한 실패 결과로 처리하고, ControllerAdvice에서 HTTP 상태와 ErrorResponse로 일관되게 내려준다.
- 면접용 문장: "200 Map 반환 문제는 예외 타입 문제가 아니라 API 실패 표현 문제입니다. RuntimeException 직접 사용 문제와 함께 보면, 이 코드는 실패 흐름과 실패 응답 계약이 모두 불명확합니다."

### 7. Test
- 테스트도 @Juint 테스트도 안보이고 , assertion? 이낙? 이런걸로 값도 비교 없고 시나리오 테스트도 없어.. 

#### 조언

Test 답변도 핵심은 잘 잡았습니다. 면접에서는 "`@Test`가 없다"에서 끝내지 말고, **실행 여부, 검증 여부, 실패 케이스, 테스트 대체 가능성**까지 말하면 좋습니다.

##### 1. 실행되지 않는 테스트 [Test / 테스트 가능성]

- 문제 유형: 명백한 안티패턴
- 관련 개념: JUnit 5, 실행 가능한 테스트
- 왜 문제인지: 테스트 메서드에 `@Test`가 없어 테스트 러너가 실행하지 않을 수 있다.
- 실무 영향: 테스트가 있는 것처럼 보이지만 CI에서 검증되지 않아 회귀 버그를 잡지 못한다.
- 개선 방향: JUnit 5 `@Test`를 붙이고, 테스트가 실제로 실패/성공을 판단하도록 만든다.
- 면접용 문장: "테스트 클래스가 있어도 테스트 러너가 실행하지 않으면 검증 자산이 아닙니다. `@Test`와 assertion으로 실행 가능한 테스트를 만들어야 합니다."

##### 2. assertion과 실패 케이스 부재 [Test / 실패 흐름]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: assertion, 실패 케이스, 경계값 테스트
- 왜 문제인지: `service.changeAddress(request)`를 호출만 하고 결과나 저장 상태를 검증하지 않는다.
- 실무 영향: 잘못된 주소 변경, 조회 실패, 차단 주소, 권한 오류가 있어도 테스트가 통과할 수 있다.
- 개선 방향: 성공 결과뿐 아니라 null 요청, 없는 orderId, 차단 주소, 배송중 상태, userId 불일치 같은 실패 케이스를 검증한다.
- 면접용 문장: "테스트는 메서드를 호출하는 것이 아니라 정책과 실패 흐름을 고정해야 합니다. 성공 케이스뿐 아니라 예외와 경계값을 assertion으로 확인해야 합니다."

##### 3. static 저장소로 인한 테스트 격리 문제 [Test / 테스트 격리]

- 문제 유형: 돌아가지만 위험한 코드
- 관련 개념: 테스트 격리, 공유 mutable state
- 왜 문제인지: Repository가 static List를 저장소처럼 쓰면 테스트 간 데이터가 남아 다음 테스트에 영향을 줄 수 있다.
- 실무 영향: 테스트 실행 순서에 따라 성공/실패가 달라지는 불안정한 테스트가 된다.
- 개선 방향: 테스트마다 저장소를 초기화하거나, Repository를 DI로 주입받아 fake/in-memory 구현을 테스트 단위로 분리한다.
- 면접용 문장: "static 저장소는 테스트 간 상태가 섞일 수 있으므로 테스트 격리가 깨집니다. 테스트마다 독립된 저장소나 fake Repository를 주입할 수 있는 구조가 필요합니다."

## 채점

- **학습 점수**: 92점
- **실제 면접관 기준 점수**: 88점 (합격권)

### ✅ 잘한 점 (칭찬)
- **Controller/Service의 의존성 및 흐름 설계**: `new`를 통한 직접 생성의 문제점(DI 위반), DTO와 Entity의 역할 구분 및 변환 위치 책임을 아주 명확하게 짚어냈습니다.
- **치명적인 정합성 버그 포착**: 배송지 조회 실패 시(`current == null`) 예외를 던지지 않고 클라이언트가 요청한 데이터로 마음대로 보정해 정상 처리해버리는 위험천만한 로직을 아주 정확하게 포착했습니다.
- **Repository의 캡슐화 및 중복**: `save()`가 단순히 `add()`만 수행하여 같은 `orderId` 데이터가 계속 중복 적재되는 저장 오류와 `static final List`에 따른 동시성/테스트 격리 문제를 꼼꼼하게 다 적으셨습니다.
- **Util의 모순과 AOP**: `@Component`를 달았음에도 모든 메서드가 `static`이라 Bean 주입을 전혀 받지 못하는 모순성을 논리적으로 설명했습니다.
- **Test의 검증 부재**: `@Test` 누락과 `Assertions` 값 비교가 없어 실제로 실행/검증조차 안 되는 껍데기 테스트 코드임을 완벽하게 짚었습니다.

### 🟡 보완할 점 (면접 합격률을 높이는 Deep-Dive)
- **DTO의 Parameter Tampering(매개변수 변조) 보안 취약점**:
  - `priority` 필드는 메모가 `URGENT`일 때 서버가 연산해서 지정해야 하는 서버 결정값입니다.
  - 하지만 이 필드가 외부 요청 DTO(`DeliveryAddressRequest`)에 노출되어 있어, 외부 클라이언트가 직접 JSON에 우선순위 값을 실어 보낼 경우 서버 규칙을 우회해 우선순위를 강제로 조작할 수 있는 **보안 취약점**이 됨을 면접에서 언급하면 가산점을 크게 받습니다.
- **빈약한 도메인 모델(Anemic Domain Model)과 캡슐화**:
  - Entity의 모든 필드가 `public`으로 선언되어 Service에서 필드값을 마구 대입하여 수정하고 있습니다. 
  - 이는 데이터만 가진 껍데기 객체로 취급하는 대표적인 안티패턴입니다. 필드를 `private`으로 묶고, 상태 변경은 Entity 내부의 비즈니스 메서드(예: `changeAddress()`)를 통해서만 통제하도록 변경해야 캡슐화가 유지됩니다.

## 수정된 코드 위치

수정 코드는 `review-answer.md` 예시가 아니라 실제 코드 파일에 반영했다.
각 파일에서 기존 문제 코드는 `// 기존 문제:` 주석으로 남기고, 수정된 코드를 활성 코드로 볼 수 있다.

- `src/main/java/com/example/delivery/DeliveryAddressController.java`
- `src/main/java/com/example/delivery/DeliveryAddressService.java`
- `src/main/java/com/example/delivery/DeliveryAddressRepository.java`
- `src/main/java/com/example/delivery/DeliveryAddress.java`
- `src/main/java/com/example/delivery/DeliveryAddressRequest.java`
- `src/main/java/com/example/delivery/DeliveryAddressException.java`
- `src/main/java/com/example/delivery/DeliveryAddressUtil.java`
- `src/test/java/com/example/delivery/DeliveryAddressServiceTest.java`

### 8. 면접에서 1분 답변으로 말한다면

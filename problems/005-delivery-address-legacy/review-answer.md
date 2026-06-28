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
- 

### 5. DTO/Entity


### 6. Exception


### 7. Test


### 8. 면접에서 1분 답변으로 말한다면

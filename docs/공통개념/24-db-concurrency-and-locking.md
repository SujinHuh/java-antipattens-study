# DB 동시성 제어와 락(Locking) 설계

다중 사용자가 동시에 동일한 데이터(재고, 포인트 등)를 변경할 때 발생하는
경쟁 상태(Race Condition)와 이를 제어하는 데이터베이스 락의 종류를 다룹니다.

---

## 1. 동시성 문제: 갱신 분실(Lost Update)

### ❌ 문제 상황 예시 (한정 수량 쿠폰 발급)
1. 쿠폰 재고가 `1`개 남아 있는 상황입니다.
2. 사용자 A와 사용자 B가 동시에 쿠폰 발급 요청을 보냅니다.
3. 스레드 A가 DB에서 재고 `1`을 조회합니다.
4. 스레드 B도 동시에 DB에서 재고 `1`을 조회합니다.
5. 스레드 A가 재고를 `0`으로 차감하고 DB에 저장합니다.
6. 스레드 B도 동시에 조회해 둔 재고 `1`을 기반으로 `0`으로 차감하고 저장합니다.
7. **결과**: 쿠폰은 2개 발급되었는데, DB 재고는 여전히 0입니다. (초과 발급 장애)

---

## 2. 해결책 1: 비관적 락 (Pessimistic Lock)

데이터 충돌이 자주 발생할 것이라 비관적으로 가정하고,
데이터를 조회할 때부터 트랜잭션 단위로 락을 획득하는 방식입니다.

쉽게 말하면 **"내가 이 row를 수정하는 동안 다른 요청은 기다려"** 입니다.
선착순 쿠폰, 좌석 예매, 재고 차감처럼 동시에 같은 row를 많이 건드리고 초과 처리가 절대 허용되지 않는 경우에 자주 검토합니다.

### ⚙️ 동작 방식
* SQL 조회 시 **`SELECT ... FOR UPDATE`** 구문을 사용합니다.
* 특정 행(Row)에 락이 걸려 있는 동안 다른 트랜잭션은 해당 행을 조회/수정하지
  못하고 대기해야 합니다.

```java
public interface StockCouponRepository extends JpaRepository<StockCoupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StockCoupon c where c.id = :id")
    Optional<StockCoupon> findByIdForUpdate(@Param("id") Long id);
}
```

* **장점**: 데이터 일관성을 강력하게 보장하며, 충돌이 잦은 환경에서 안전합니다.
* **단점**: 락 대기로 인해 커넥션 풀이 고갈되어 성능(Throughput)이 크게 저하될 수
  있으며, 데드락(Deadlock)이 발생할 위험이 있습니다.

흐름으로 보면 아래와 같습니다.

```text
초기 재고: 1

A 요청: 쿠폰 row 조회 + DB 락 획득
B 요청: 같은 쿠폰 row를 수정하려고 하지만 A가 끝날 때까지 대기

A 요청: 재고 1 -> 0 저장 후 커밋, 락 해제
B 요청: 이제 조회/수정 가능, 재고 0 확인 후 sold out 처리
```

면접에서 말할 때는 장단점을 같이 말해야 합니다.

```text
비관적 락은 충돌 가능성이 높은 자원에 먼저 DB row lock을 걸어 정합성을 강하게 지키는 방식입니다.
선착순 쿠폰처럼 초과 발급이 치명적인 경우 안전하지만, 트래픽이 몰리면 락 대기와 커넥션 점유로 처리량이 떨어질 수 있습니다.
```

---

## 3. 해결책 2: 낙관적 락 (Optimistic Lock)

데이터 충돌이 거의 발생하지 않을 것이라 낙관적으로 가정하고,
애플리케이션 단에서 버전을 통해 동시성을 제어하는 방식입니다.

쉽게 말하면 **"먼저 잠그지는 않고, 저장할 때 누가 먼저 바꿨는지 version으로 확인하자"** 입니다.
비관적 락처럼 기다리게 만들지는 않지만, 충돌이 나면 늦게 저장한 쪽을 실패시키고 재시도 정책을 태워야 합니다.

### ⚙️ 동작 방식
* 테이블에 `version` 필드를 추가합니다.
* 데이터를 수정할 때 `WHERE version = :current_version` 조건을 걸어 업데이트합니다.
* 동시 요청 중 먼저 커밋된 요청이 버전을 올리면, 늦은 요청은 조건이 맞지 않아
  수정에 실패하고 **`ObjectOptimisticLockingFailureException`**이 발생합니다.

```java
@Entity
public class StockCoupon {
    @Id @GeneratedValue
    private Long id;
    
    @Version // 👈 JPA가 버전을 자동 관리
    private Long version;
    
    private int remainingQuantity;
}
```

* **장점**: DB 실제 락을 잡지 않으므로 비관적 락보다 가볍고 동시 처리량이 높습니다.
* **단점**: 충돌이 발생하면 예외가 터지므로, 개발자가 애플리케이션 레벨에서
  **롤백 및 재시도(Retry) 로직**을 직접 구현해야 합니다.

흐름으로 보면 아래와 같습니다.

```text
초기 상태: version = 1, remainingQuantity = 1

A 요청: version 1 조회
B 요청: version 1 조회

A 요청: where id = ? and version = 1 조건으로 update 성공, version 2로 증가
B 요청: where id = ? and version = 1 조건으로 update 시도
       하지만 DB version은 이미 2라서 update row count = 0
       충돌로 판단하고 실패 또는 재시도 처리
```

비관적 락과 낙관적 락의 차이는 이렇게 정리할 수 있습니다.

| 구분 | 비관적 락 | 낙관적 락 |
|---|---|---|
| 기본 생각 | 충돌이 날 것 같으니 먼저 잠근다 | 충돌이 적을 것 같으니 저장 시점에 확인한다 |
| 방식 | `select ... for update`, `PESSIMISTIC_WRITE` | `@Version`, `where version = ?` |
| 다른 요청 | 락이 풀릴 때까지 대기 | 일단 진행하지만 저장 시 충돌나면 실패 |
| 장점 | 정합성이 강하고 이해하기 쉽다 | 대기가 적고 처리량이 좋을 수 있다 |
| 단점 | 락 대기, 데드락, 커넥션 점유 | 충돌 시 재시도/실패 처리 필요 |
| 어울리는 상황 | 충돌이 잦고 초과 처리가 치명적 | 충돌이 드물고 재시도가 가능한 경우 |

### `clientVersion` 비교만으로는 낙관적 락이 아니다

007번 코드에는 아래처럼 클라이언트가 보낸 version과 쿠폰 version을 비교하는 코드가 있습니다.

```java
if (request.clientVersion != null && request.clientVersion < coupon.version) {
    return new StockCouponResponse(coupon.id, request.userId, "STALE_REQUEST", coupon.remainingQuantity);
}

coupon.version = coupon.version + 1;
```

이 코드는 낙관적 락처럼 보이지만, 실제 낙관적 락으로 동작하지 않습니다.

낙관적 락의 핵심은 **DB update 시점에 현재 version이 기대한 version과 같은지 확인하는 것**입니다.

예를 들어 두 요청이 동시에 들어오면:

```text
초기 상태: coupon.version = 1, remainingQuantity = 1

요청 A: version 1 조회
요청 B: version 1 조회

요청 A: clientVersion 1, coupon.version 1 -> 통과
요청 B: clientVersion 1, coupon.version 1 -> 통과

요청 A: remainingQuantity 차감, version 2로 저장
요청 B: 이미 읽어둔 값 기준으로 remainingQuantity 차감, version 2로 저장
```

둘 다 같은 version을 보고 통과할 수 있습니다.
애플리케이션 코드에서 단순 비교만 하면 DB가 "누가 먼저 update했는지"를 막아주지 않습니다.
즉, Java 코드의 `if`는 현재 객체에 대해 한 번 판단할 뿐이고, 동시에 들어온 다른 트랜잭션의 update를 DB 레벨에서 막거나 감지하지 못합니다.

실제 낙관적 락은 아래처럼 동작해야 합니다.

```sql
update stock_coupon
   set remaining_quantity = remaining_quantity - 1,
       version = version + 1
 where id = ?
   and version = ?;
```

이때 먼저 성공한 요청이 version을 올리면, 늦게 온 요청은 `where version = ?` 조건이 맞지 않아 update row count가 0이 됩니다.
JPA에서는 `@Version`이 이 역할을 대신합니다.

```java
@Entity
public class StockCoupon {
    @Id
    private Long id;

    @Version
    private Long version;

    private int remainingQuantity;
}
```

007번 코드에서 봐야 할 판단 기준은 아래와 같습니다.

```text
1. version 필드가 있는가? -> 있음
2. 하지만 JPA @Version인가? -> 아님
3. update SQL에 where version = ? 조건이 있는가? -> 없음
4. 충돌 시 예외나 update count 0 처리가 있는가? -> 없음
5. 결론: version 숫자는 있지만 낙관적 락으로 작동하지 않는다.
```

`clientVersion`은 클라이언트가 알고 있던 버전입니다.
오래된 화면에서 요청했는지 확인하는 참고값으로는 쓸 수 있지만, 동시성 충돌을 막는 장치는 아닙니다.

```text
clientVersion = 클라이언트가 알고 있던 버전
@Version / where version = ? = DB update 시점에 충돌을 감지하는 장치
```

면접용 문장:

```text
clientVersion을 단순 비교하는 것은 낙관적 락이 아닙니다.
낙관적 락은 DB update 시점에 version 조건을 확인해 충돌을 감지해야 하고,
충돌 시 재시도하거나 사용자에게 재시도 응답을 내려야 합니다.
```

---

## 4. 코드 흐름으로 보는 비관적 락과 낙관적 락

아래 예시는 모두 "쿠폰 재고 1개가 남아 있고, A와 B가 동시에 발급 요청을 보낸다"는 상황입니다.
면접에서는 프레임워크 이름보다 **어디서 동시성 충돌을 막는지**를 먼저 봐야 합니다.

```text
비관적 락: 조회할 때 DB row를 잠근다.
낙관적 락: 저장할 때 version 조건으로 충돌을 감지한다.
조건부 update: 재고 차감을 DB update 한 문장으로 끝낸다.
분산 락: 여러 서버가 같은 작업에 동시에 들어오지 못하게 외부 락을 잡는다.
```

### JPA 비관적 락 흐름

Repository에서 `PESSIMISTIC_WRITE`를 사용하면 JPA가 DB에 `select ... for update` 계열의 쿼리를 날립니다.
핵심은 **조회 시점에 락을 잡고, 트랜잭션이 끝날 때까지 락을 유지한다**는 점입니다.

```java
public interface StockCouponJpaRepository extends JpaRepository<StockCoupon, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from StockCoupon c where c.id = :couponId")
    Optional<StockCoupon> findByIdForUpdate(Long couponId);
}
```

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    StockCoupon coupon = couponRepository.findByIdForUpdate(request.couponId())
        .orElseThrow(CouponNotFoundException::new);

    coupon.issue(request.requestedQuantity());
    issueHistoryRepository.save(CouponIssueHistory.of(coupon, request.userId()));

    return StockCouponResponse.issued(coupon);
}
```

동시 요청 흐름:

```text
A 요청: @Transactional 시작
A 요청: findByIdForUpdate -> coupon row lock 획득

B 요청: @Transactional 시작
B 요청: findByIdForUpdate -> 같은 row lock을 얻으려다 대기

A 요청: coupon.issue(1), history 저장, commit
A 요청: commit되면서 row lock 해제

B 요청: 이제 row 조회 가능
B 요청: remainingQuantity = 0 확인
B 요청: SoldOutException 또는 SOLD_OUT 응답
```

이 흐름에서는 B가 오래 기다릴 수 있습니다.
그래서 초과 발급은 막기 쉽지만, 트래픽이 몰리면 DB 커넥션 대기와 타임아웃이 문제가 됩니다.

### JPA 낙관적 락 흐름

JPA 낙관적 락은 Entity에 `@Version`을 둡니다.
조회할 때는 락을 잡지 않고, flush/commit 시점에 JPA가 version 조건을 포함한 update를 수행합니다.

```java
@Entity
public class StockCoupon {
    @Id
    private Long id;

    @Version
    private Long version;

    private int remainingQuantity;

    public void issue(int quantity) {
        if (remainingQuantity < quantity) {
            throw new SoldOutException();
        }
        this.remainingQuantity -= quantity;
    }
}
```

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    StockCoupon coupon = couponRepository.findById(request.couponId())
        .orElseThrow(CouponNotFoundException::new);

    coupon.issue(request.requestedQuantity());
    issueHistoryRepository.save(CouponIssueHistory.of(coupon, request.userId()));

    return StockCouponResponse.issued(coupon);
}
```

JPA가 내부적으로 의도하는 update는 아래와 비슷합니다.

```sql
update stock_coupon
   set remaining_quantity = ?,
       version = version + 1
 where id = ?
   and version = ?;
```

동시 요청 흐름:

```text
초기 상태: remainingQuantity = 1, version = 1

A 요청: coupon 조회, version 1
B 요청: coupon 조회, version 1

A 요청: issue(1), commit
A 요청: update where version = 1 성공, DB version = 2

B 요청: issue(1), commit
B 요청: update where version = 1 시도
B 요청: DB version은 이미 2라 update 실패
B 요청: ObjectOptimisticLockingFailureException 발생
```

낙관적 락에서는 예외가 정상적인 충돌 신호입니다.
그래서 Service나 상위 계층에서 재시도할지, 사용자에게 재시도 응답을 줄지 정책이 필요합니다.

```java
public StockCouponResponse issueWithRetry(StockCouponRequest request) {
    for (int i = 0; i < 3; i++) {
        try {
            return issue(request);
        } catch (ObjectOptimisticLockingFailureException e) {
            // 짧게 재시도하거나, 바로 재시도 응답을 줄 수 있다.
        }
    }
    throw new CouponConflictException("동시 요청이 많아 다시 시도해주세요.");
}
```

### MyBatis 비관적 락 흐름

MyBatis에서는 JPA 어노테이션 대신 SQL에 직접 `for update`를 씁니다.
Service의 `@Transactional` 안에서 이 select가 실행되어야 락이 트랜잭션 끝까지 유지됩니다.

```java
public interface StockCouponMapper {
    StockCoupon selectByIdForUpdate(long couponId);

    int updateQuantity(StockCoupon coupon);

    int insertIssueHistory(CouponIssueHistory history);
}
```

```xml
<select id="selectByIdForUpdate" resultType="StockCoupon">
    select id, remaining_quantity, version
      from stock_coupon
     where id = #{couponId}
       for update
</select>

<update id="updateQuantity">
    update stock_coupon
       set remaining_quantity = #{remainingQuantity},
           version = version + 1
     where id = #{id}
</update>
```

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    StockCoupon coupon = mapper.selectByIdForUpdate(request.couponId());

    if (coupon.getRemainingQuantity() < request.requestedQuantity()) {
        throw new SoldOutException();
    }

    coupon.decrease(request.requestedQuantity());
    mapper.updateQuantity(coupon);
    mapper.insertIssueHistory(CouponIssueHistory.of(coupon, request.userId()));

    return StockCouponResponse.issued(coupon);
}
```

여기서 중요한 것은 `selectByIdForUpdate`와 `updateQuantity`가 같은 트랜잭션 안에 있어야 한다는 점입니다.
트랜잭션 없이 `for update`만 쓰면 락 유지 범위가 기대와 다를 수 있습니다.

### MyBatis 낙관적 락 흐름

MyBatis 낙관적 락은 update SQL에 version 조건을 직접 넣고, update된 row 수를 확인합니다.
`updatedRows == 0`이면 누군가 먼저 수정했다는 뜻입니다.

```xml
<update id="decreaseQuantityWithVersion">
    update stock_coupon
       set remaining_quantity = remaining_quantity - #{quantity},
           version = version + 1
     where id = #{couponId}
       and version = #{version}
       and remaining_quantity >= #{quantity}
</update>
```

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    StockCoupon coupon = mapper.selectById(request.couponId());

    int updatedRows = mapper.decreaseQuantityWithVersion(
        coupon.getId(),
        coupon.getVersion(),
        request.requestedQuantity()
    );

    if (updatedRows == 0) {
        throw new CouponConflictException("재고가 부족하거나 동시에 수정되었습니다.");
    }

    mapper.insertIssueHistory(CouponIssueHistory.of(coupon, request.userId()));
    return StockCouponResponse.issued(coupon);
}
```

동시 요청 흐름:

```text
A 요청: selectById -> version 1
B 요청: selectById -> version 1

A 요청: update where version = 1 and remaining_quantity >= 1
A 요청: updatedRows = 1, 성공

B 요청: update where version = 1 and remaining_quantity >= 1
B 요청: 이미 version이 2라서 updatedRows = 0
B 요청: 충돌 또는 품절로 처리
```

MyBatis에서는 JPA처럼 예외가 자동으로 나는 구조가 아닐 수 있습니다.
그래서 **update count를 반드시 확인해야 합니다.**

### DB 조건부 update만으로 처리하는 흐름

재고 차감은 낙관적 락 없이도 DB 조건부 update 한 문장으로 안전하게 처리할 수 있습니다.
핵심은 "조회 후 차감"이 아니라 **차감 가능한 경우에만 DB가 바로 차감**하게 만드는 것입니다.

```sql
update stock_coupon
   set remaining_quantity = remaining_quantity - #{quantity}
 where id = #{couponId}
   and remaining_quantity >= #{quantity};
```

```java
@Transactional
public StockCouponResponse issue(StockCouponRequest request) {
    int updatedRows = mapper.decreaseIfEnough(
        request.couponId(),
        request.requestedQuantity()
    );

    if (updatedRows == 0) {
        throw new SoldOutException();
    }

    mapper.insertIssueHistory(CouponIssueHistory.of(request));
    return StockCouponResponse.issued(request.couponId(), request.userId());
}
```

이 방식은 재고 수량만 정확히 차감하면 되는 경우 단순하고 강력합니다.
다만 Entity의 복잡한 상태 전이, version 기반 충돌 구분, 상세한 도메인 이벤트가 필요하면 별도 설계가 필요합니다.

### MSA/분산 환경에서의 흐름

MSA에서는 서버 인스턴스가 여러 대라서 `synchronized` 같은 JVM 내부 락은 의미가 약합니다.
서버 A 인스턴스와 서버 B 인스턴스는 서로 다른 JVM이기 때문입니다.

```text
사용자 요청 A -> coupon-service-1
사용자 요청 B -> coupon-service-2

각 서버의 synchronized는 자기 JVM 안에서만 동작
DB row나 Redis 같은 공통 자원으로 제어하지 않으면 동시 차감 가능
```

MSA에서 선택지는 보통 아래처럼 나뉩니다.

| 방식 | 흐름 | 주의점 |
|---|---|---|
| DB 비관적 락 | 여러 인스턴스가 같은 DB row lock을 경쟁 | 정합성은 강하지만 DB 대기 증가 |
| DB 낙관적 락 | 여러 인스턴스가 version update 경쟁 | 충돌 예외/재시도 정책 필요 |
| DB 조건부 update | `remaining_quantity >= quantity` 조건으로 원자적 차감 | update count 확인 필수 |
| Redis 분산 락 | `coupon:{id}` 키로 외부 락 획득 후 DB 처리 | 락 해제와 DB commit 순서 주의 |
| 메시지 큐 직렬화 | 쿠폰 발급 요청을 큐에 넣고 단일 소비자가 순서대로 처리 | 응답 지연, 비동기 상태 관리 필요 |

Redis 분산 락을 쓰는 경우 흐름은 아래와 같습니다.

```java
public StockCouponResponse issue(StockCouponRequest request) {
    RLock lock = redissonClient.getLock("coupon:" + request.couponId());

    boolean locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
    if (!locked) {
        throw new CouponBusyException();
    }

    try {
        return stockCouponTxService.issueInTransaction(request);
    } finally {
        lock.unlock();
    }
}
```

```java
@Transactional
public StockCouponResponse issueInTransaction(StockCouponRequest request) {
    StockCoupon coupon = couponRepository.findById(request.couponId())
        .orElseThrow(CouponNotFoundException::new);

    coupon.issue(request.requestedQuantity());
    issueHistoryRepository.save(CouponIssueHistory.of(coupon, request.userId()));

    return StockCouponResponse.issued(coupon);
}
```

주의할 점은 분산 락을 잡았다고 DB 트랜잭션 문제가 자동으로 해결되는 것은 아니라는 점입니다.
특히 락을 먼저 풀고 DB commit이 늦게 일어나면, 다음 요청이 아직 커밋되지 않은 상태를 보고 들어올 수 있습니다.
그래서 분산 락을 쓸 때도 트랜잭션 범위, 락 해제 시점, DB 조건부 update 또는 unique constraint를 같이 봐야 합니다.

면접용 문장:

```text
MSA 환경에서는 JVM 내부 락이 인스턴스 간에 공유되지 않기 때문에 동시성 제어를 DB row lock, DB 조건부 update, Redis 분산 락, 메시지 큐 같은 공통 자원 기준으로 설계해야 합니다.
분산 락을 쓰더라도 DB commit 시점과 락 해제 시점이 어긋나면 정합성 문제가 남을 수 있어, 트랜잭션 범위와 DB 제약조건을 함께 확인하겠습니다.
```

### 다음 문제에서도 반복해서 볼 체크리스트

동시성 문제가 보이면 아래 순서로 코드를 봅니다.

```text
1. 같은 데이터를 여러 요청이 동시에 수정할 수 있는가?
2. 조회 후 수정(read-modify-write) 구조인가?
3. 재고 검증과 차감이 한 DB 연산으로 묶여 있는가?
4. 비관적 락이면 select ... for update가 트랜잭션 안에서 실행되는가?
5. 낙관적 락이면 @Version 또는 where version = ?가 실제 update에 들어가는가?
6. MyBatis라면 update count == 0을 충돌/품절로 처리하는가?
7. MSA라면 JVM 내부 락이 아니라 DB/Redis/Queue 같은 공통 자원으로 막는가?
8. 중복 요청 재시도를 idempotencyKey나 unique constraint로 막는가?
9. 충돌 발생 시 재시도, 품절, 재요청 중 어떤 응답을 줄지 정책이 있는가?
```

---

## 5. 수량 검증과 원자적 차감

재고 차감에서는 `remainingQuantity <= 0`만 보면 부족합니다.
요청 수량이 남은 재고보다 큰지도 확인해야 합니다.

```java
if (coupon.remainingQuantity <= 0) {
    throw new StockCouponException("sold out");
}

int quantity = request.requestedQuantity <= 0 ? 1 : request.requestedQuantity;
coupon.remainingQuantity = coupon.remainingQuantity - quantity;
```

위 코드는 `remainingQuantity = 3`, `requestedQuantity = 10`일 때 재고가 `-7`이 될 수 있습니다.

더 안전한 흐름:

```java
if (quantity <= 0) {
    throw new InvalidQuantityException();
}

if (coupon.remainingQuantity < quantity) {
    throw new SoldOutException();
}

coupon.issue(quantity);
```

동시성까지 고려하면 검증과 차감이 분리되어 있으면 안 됩니다.
DB 조건 업데이트나 락으로 "검증 + 차감"을 원자적으로 처리해야 합니다.

```sql
update stock_coupon
   set remaining_quantity = remaining_quantity - :quantity
 where id = :couponId
   and remaining_quantity >= :quantity;
```

면접용 문장:

```text
재고가 0보다 큰지만 보면 요청 수량보다 충분한지 검증하지 못합니다.
재고 검증과 차감은 하나의 도메인 메서드나 DB 조건 업데이트로 원자적으로 처리해야 합니다.
```

---

## 6. 해결책 3: 분산 락 (Distributed Lock)

여러 대의 애플리케이션 서버 분산 환경에서 특정 공유 자원에 대한 동기화를
DB 외부(Redis, ZooKeeper 등)의 공통 분산 메모리를 이용해 제어하는 방식입니다.

* 대표적으로 Redis의 **Redisson** 라이브러리를 활용해 Pub/Sub 기반의 락을 구현합니다.
* DB 커넥션을 점유하지 않고 메모리 상에서 빠르게 락을 획득/반환하므로 효율적입니다.

---

---

## 7. 멱등성 키와 중복 요청

쿠폰 발급처럼 재시도가 가능한 API에서는 `idempotencyKey`가 중요합니다.

문제 코드에는 `idempotencyKey` 필드와 `existsByIdempotencyKey` 메서드가 있지만, 실제 Service 발급 흐름에서는 사용하지 않습니다.

```java
public boolean existsByIdempotencyKey(String idempotencyKey) {
    // ...
}

// Service에서는 userId + couponId 중복만 확인
if (issueHistoryRepository.existsByUserIdAndCouponId(request.userId, request.couponId)) {
    return new StockCouponResponse(request.couponId, request.userId, "ALREADY_ISSUED", 0);
}
```

idempotency key는 "같은 요청이 재시도되었을 때 같은 결과를 반환하기 위한 키"입니다.

```text
1. 클라이언트가 쿠폰 발급 요청 전송
2. 서버는 발급 성공, 그러나 네트워크 타임아웃으로 클라이언트는 응답을 못 받음
3. 클라이언트가 같은 idempotencyKey로 재시도
4. 서버는 새 발급을 하지 않고 이전 처리 결과를 반환해야 함
```

개선 방향:

- `idempotencyKey`를 필수값으로 받는다.
- `idempotencyKey`에 unique constraint를 둔다.
- 같은 key가 이미 처리되었으면 이전 결과를 반환한다.
- 처리 중인 key라면 `PROCESSING` 또는 재시도 응답 기준을 정한다.

면접용 문장:

```text
idempotencyKey가 DTO와 Repository에는 있지만 Service 흐름에서 사용되지 않으면 재시도 안전성이 없습니다.
같은 idempotencyKey에 대해서는 같은 결과를 반환하고, DB unique constraint로 중복 처리를 막아야 합니다.
```

---

## 8. 면접 답변 템플릿

### Q. 선착순 재고 차감 시 동시성 문제를 어떻게 해결하시겠습니까?

> "선착순 쿠폰 발급처럼 충돌이 빈번하게 발생하는 시나리오에서는
> 동시성 제어 장치가 필수적입니다.
> 
> 첫째, 충돌 빈도가 매우 높고 재고 초과가 절대 허용되지 않는 비즈니스라면
> DB 레벨의 **비관적 락(`SELECT ... FOR UPDATE`)**을 사용해 정합성을 지키겠습니다.
> 
> 둘째, 트래픽 처리량이 중요하고 충돌이 드문 구조라면 대기 없는 **낙관적 락**과
> 재시도 로직을 구현하여 성능을 개선할 수 있습니다.
> 
> 셋째, 분산 환경에서 DB 커넥션 병목을 줄여 성능을 대폭 끌어올려야 한다면,
> Redis를 활용한 **분산 락**을 적용하여 임계 구역을 제어하는 설계를 고려하겠습니다."

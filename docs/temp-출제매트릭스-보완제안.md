# 개념별 출제 확장 매트릭스 보완 제안 (임시 저장)

## 💬 수진님의 핵심 질문 및 논의 이력

학습 과정에서 수진님이 직접 던지신 주요 설계/문법 질문들과 그 요약입니다. 이 질문들은 다음 회차 문제 출제 시 수진님의 이해도를 검증하기 위해 적극적으로 반영될 예정입니다.

1. **DTO와 Entity의 생성 패턴**
   - *질문*: "PointRefundRequest가 DTO 역할을 하는 거네? @AllArgsConstructor 이걸 사용하는 거야? Entity인 PointPayment에서는 @Builder를 사용하는 거 맞지? @Builder가 뭔지 잘 몰라 설명해줘."
2. **DTO ➜ Entity 변환 위치**
   - *질문*: "Controller에서 DTO를 받고, 이걸 Entity로 변환해서 Service로 넘겨주는 게 맞지?" (※ Service 계층에서 변환하는 것이 SRP 및 결합도 측면에서 올바른 방향임을 확인)
3. **Controller 반환 타입과 ResponseEntity**
   - *질문*: "Controller 반환 타입이 ResponseEntity<PointRefundResponse> DTO여야 하는 거 맞지? 무조건 ResponseEntity로 감싸서 반환해야 해? 리팩토링 코드에서는 그냥 DTO만 반환하기도 하던데?"
4. **N+1 및 중복 단건 조회**
   - *질문*: "getRefundHistory에서 findAll을 한 뒤에 루프 안에서 findById를 또 호출한다는 거야? 그래서 N+1이 발생한다는 거지?" (※ 이미 리스트를 다 가져와놓고 중복 조회하는 비효율 포착)
5. **Optional과 static List 동시성**
   - *질문*: "NPE 예방을 위해 Optional로 변경하는 건 어떻게 쓰는 거야? static final List의 동시성 문제와 정합성 문제는 어떻게 해결할 수 있어?"
6. **@Component와 static 메서드 충돌**
   - *질문*: "Util에 @Component라고 등록을 해뒀는데, 내부 메서드가 static인 건 왜 안 돼? static 대신 public으로 쓰고 생성자 주입으로 Service에서 받아서 쓰면 안 되는 거야?"
7. **Exception 설계와 에러 코드**
   - *질문*: "RuntimeException을 그대로 상속받아서 예외를 뱉으면 어떤 종류인지 모르니까 문자열을 직접 비교해야 하는 거 아냐? 각각의 HTTP 상태 코드를 매핑해야 한다는 거지?"
8. **JUnit 테스트 코드 부실**
   - *질문*: "테스트 코드에 @Test 어노테이션도 없고, assert 값 비교도 없고, JUnit도 없는 것 같은데?"

---

이 문서는 `23. 개념별 출제 확장 매트릭스.md`를 한 단계 더 높은 난이도(실무 기술 면접 Deep-Dive)로 끌어올리기 위한 서브에이전트의 검수 결과 및 보완 제안 사항을 임시로 기록해둔 파일입니다.

---

## 💡 5대 핵심 영역 보완 제안 (난이도 고도화)

### 1. JPA & 영속성 컨텍스트 심화 (Repository / Entity 책임)
*   **추가할 아이디어**: JPQL 벌크 연산(`@Modifying`) 후 영속성 컨텍스트 초기화(`clearAutomatically`) 누락으로 인한 데이터 정합성 깨짐 문제.
*   **면접용 꼬리질문 포인트**: 
    - Dirty Checking과 `save()`의 내부 동작 방식(새 엔티티 판별 로직)의 차이는 무엇인가?
    - 벌크 연산 시 왜 `clearAutomatically = true` 옵션이 필요한가? 영속성 컨텍스트의 생명주기와 DB 트랜잭션의 관계는 무엇인가?

### 2. 동시성(Concurrency) 제어 고도화 (동시성 파트)
*   **추가할 아이디어**: 단일 JVM의 동시성(`synchronized`) ➜ DB 락(낙관적/비관적 락) ➜ **Redis 분산 락(Redisson) 도입 시 `@Transactional`과의 트랜잭션 타이밍 정합성 이슈** (락은 풀렸는데 트랜잭션 커밋이 아직 완료되지 않아 그 미세한 틈새에 동시성 정합성이 깨지는 실무 장애 문제).
*   **면접용 꼬리질문 포인트**:
    - 낙관적 락과 비관적 락의 성능적 트레이드오프와 데드락 가능성은 어떠한가?
    - Redisson 분산 락을 걸 때 `@Transactional`의 AOP 적용 순서가 왜 중요한가? (락 획득/해제는 트랜잭션 범위 밖에서 일어나야 함)

### 3. Spring AOP 프록시 & 트랜잭션 전파 (Transaction / AOP 파트)
*   **추가할 아이디어**: Checked Exception(예: `IOException`, 사용자 정의 비즈니스 예외) 발생 시 Spring 트랜잭션이 기본적으로 롤백되지 않고 그냥 커밋되는 예외 설계 오류.
*   **면접용 꼬리질문 포인트**:
    - Spring 프록시(JDK Dynamic Proxy vs CGLIB Proxy)의 내부 호출(Self-Invocation) 한계와 이를 극복하는 구조적 대안은 무엇인가?
    - Checked Exception과 Unchecked Exception 발생 시 Spring 트랜잭션의 기본 롤백 정책 차이와 그 이유.

### 4. 외부 API 연동 & 장애 전파 방지 (Util / Service 파트)
*   **추가할 아이디어**: 외부 API(날씨, 카카오 알림톡 등) 연동 시 Timeout 설정 누락으로 외부 장애가 당사 시스템 전체의 **Thread Pool 고갈(Thread Starvation)**로 이어져 서버 전체가 다운되는 장애 전파 시나리오.
*   **면접용 꼬리질문 포인트**:
    - Connect Timeout과 Read Timeout의 차이 및 적절한 설정 기준은 무엇인가?
    - 장애 전파를 차단하기 위한 서킷 브레이커(Resilience4j) 패턴과 동작 원리.

### 5. 글로벌 시간대 (Timezone) 검증 (시간 / 상태 변경 파트)
*   **추가할 아이디어**: `LocalDateTime.now()` 사용 시 서버 환경(UTC)과 로컬 환경(KST) 간 타임존 불일치로 비즈니스 마감/취소 시간이 어긋나는 문제.
*   **면접용 꼬리질문 포인트**:
    - 글로벌 서비스 개발 시 `ZonedDateTime`, `OffsetDateTime`, `Instant`를 각각 어떤 상황에 써야 하며, DB 저장 시 타임존 관리는 어떻게 해야 하는가?

---

## 📌 향후 반영 계획
이 보완 제안은 현재 임시 저장 상태입니다. 수진님과 함께 내용을 보완하고 조율한 뒤, 확정이 되면 `23. 개념별 출제 확장 매트릭스.md`에 최종 반영할 예정입니다.

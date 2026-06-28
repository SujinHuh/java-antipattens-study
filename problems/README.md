# Problems

회차별 코드 리뷰 문제를 저장하는 공간이다.

문제는 헤렌 백엔드 1차 기술면접 대비 기준을 따른다.

- 짧은 Spring Boot 레거시 코드 템플릿을 우선한다.
- 기본 구조는 Controller -> Service -> Repository -> Util 흐름으로 만든다.
- 코드 양은 40분 안에 리뷰할 수 있는 정도로 제한한다.
- 실제 회사 레거시 코드처럼 그럴듯하게 작성하되, 잘못된 코드 습관을 여러 개 숨긴다.
- Spring Boot 계층을 빠르게 파악할 수 있도록 필요한 어노테이션은 주석으로 표기한다.
- 풀이 전에는 정답, 문제점 목록, 개선 코드를 보여주지 않는다.

권장 구조:

```text
problems/
└── 001-payment-legacy/
    ├── README.md
    ├── review-answer.md
    ├── feedback.md
    ├── refactoring-plan.md
    ├── til.md
    ├── src/main/java/com/example/payment/
    │   ├── PaymentController.java
    │   ├── PaymentService.java
    │   ├── PaymentRepository.java
    │   ├── PaymentUtil.java
    │   ├── Payment.java
    │   ├── PaymentRequest.java
    │   └── PaymentException.java
    └── solution/
        ├── README.md
        └── src/main/java/com/example/payment/
```

문제를 낼 때는 IntelliJ나 VSCode에서 바로 열어볼 수 있도록 Java 파일을 계층별로 나눈다.
풀이 전에는 `src/`의 레거시 문제 코드만 보고, `refactoring-plan.md`와 `solution/`은 답변 제출과 채점 이후에만 본다.

## 풀이 후 리팩터링 자료

풀이가 끝난 문제에는 비교 학습을 위해 아래 자료를 추가한다.

- `refactoring-plan.md`: 개선 방향, 우선순위, 유지할 동작, 테스트 전략, 트레이드오프
- `solution/README.md`: 원본 대비 변경 요약과 "가능한 개선 예시"라는 전제
- `solution/src/`: 개선 코드 예시

`solution/`은 유일한 정답이 아니라 가능한 개선 예시다. 실제 설계는 도메인 정책, 팀 컨벤션, 사용 기술에 따라 달라질 수 있음을 명시한다.
원본 코드와 IntelliJ에서 구분하기 쉽도록 개선 예시의 주요 타입은 `기존이름_Refactored` 형식으로 작성한다.
예: `ReservationRepository` -> `ReservationRepository_Refactored`
개선 코드에는 원본 대비 수정 이유가 보이도록 핵심 변경 지점에만 짧은 주석을 단다.
주석은 `기존 코드:`와 `수정 코드:` 형식을 사용해 비교 학습이 가능하게 한다.

## 문제 다양화 원칙

새 문제를 만들 때는 도메인명만 바꾼 비슷한 코드 반복을 피한다.

- 문제마다 코드 모양과 요청 흐름을 다르게 만든다.
- 생성 API만 반복하지 말고 조회, 수정, 취소, 상태 전이, 권한 확인, 정산/쿠폰/포인트 같은 서비스성 로직을 섞는다.
- 어떤 문제는 Controller가 거의 정상이고 Service, Repository, Util에 핵심 문제가 있게 만든다.
- 어떤 문제는 명백히 나쁜 코드가 아니라 트레이드오프를 설명해야 하는 코드로 만든다.
- 이전에 틀린 개념은 다시 넣되 같은 코드 패턴으로 반복하지 않는다.
- README에는 특정 개념 힌트를 과하게 노출하지 않고, 사용자가 코드만 보고 문제를 찾게 한다.

## IntelliJ 설정

새 문제 폴더를 만들 때는 IntelliJ에서 Ctrl+B, 클래스 이동, 자동완성이 바로 동작하도록 source root를 함께 등록한다.

문제 폴더마다 아래 파일을 만든다.

```text
problems/
└── 002-reservation-legacy/
    └── src/
        ├── main/
        │   ├── main.iml
        │   └── java/
        └── test/
            ├── test.iml
            └── java/
```

`src/main/main.iml`에는 `src/main/java`를 production source root로 등록한다.
`src/test/test.iml`에는 `src/test/java`를 test source root로 등록한다.

`.idea/modules.xml`에도 새 문제의 두 module을 추가한다.

```xml
<module fileurl="file://$PROJECT_DIR$/problems/002-reservation-legacy/src/main/main.iml" filepath="$PROJECT_DIR$/problems/002-reservation-legacy/src/main/main.iml" />
<module fileurl="file://$PROJECT_DIR$/problems/002-reservation-legacy/src/test/test.iml" filepath="$PROJECT_DIR$/problems/002-reservation-legacy/src/test/test.iml" />
```

이 설정을 누락하면 IntelliJ가 해당 문제 코드를 인덱싱하지 못해 Ctrl+B 이동이 안 될 수 있다.

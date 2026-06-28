# 002. Reservation Legacy Review

## 학습 목표

Controller -> Service -> Repository -> DTO/Entity -> Exception -> Test 순서로 코드를 읽으며 안티패턴을 찾는다.

README는 풀이 전 안내만 제공한다. 먼저 코드만 보고 `review-answer.md`에 답변을 작성한다.

## 읽는 순서

1. `src/main/java/com/example/reservation/ReservationController.java`
2. `src/main/java/com/example/reservation/ReservationService.java`
3. `src/main/java/com/example/reservation/ReservationRepository.java`
4. `src/main/java/com/example/reservation/ReservationRequest.java`
5. `src/main/java/com/example/reservation/Reservation.java`
6. `src/main/java/com/example/reservation/ReservationException.java`
7. `src/test/java/com/example/reservation/ReservationServiceTest.java`

## 문제

아래 관점으로 코드 리뷰 답변을 작성하세요.

1. 각 계층에서 보이는 안티패턴을 찾으세요.
2. 왜 문제가 되는지 설명하세요.
3. 리팩터링 방향을 제안하세요.
4. 기존 동작을 보존하려면 어떤 테스트가 필요한지 말하세요.
5. 면접에서 설명한다면 어떤 표현으로 말할지 작성하세요.

## 풀이 후 자료

- `review-answer.md`: 내 답변, 채점, 보완 답변
- `feedback.md`: 놓친 부분과 다음 문제 보강점
- `til.md`: 이번 회차 개념 정리
- `refactoring-plan.md`: 풀이 후 리팩터링 계획
- `solution/`: 풀이 후 비교할 개선 코드 예시

`refactoring-plan.md`와 `solution/`은 답변 제출과 채점 이후에 본다.

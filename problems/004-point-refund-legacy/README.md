# 004. Point Refund Legacy Review

## 상황

포인트 결제 건을 환불 처리하고, 환불 결과를 내려주는 짧은 Spring Boot 레거시 코드입니다.

## 읽는 순서

1. `src/main/java/com/example/point/PointRefundController.java`
2. `src/main/java/com/example/point/PointRefundService.java`
3. `src/main/java/com/example/point/PointPaymentRepository.java`
4. `src/main/java/com/example/point/PointRefundUtil.java`
5. `src/main/java/com/example/point/PointPayment.java`
6. `src/main/java/com/example/point/PointRefundRequest.java`
7. `src/main/java/com/example/point/PointRefundException.java`
8. `src/test/java/com/example/point/PointRefundServiceTest.java`

## 작성 위치

코드 리뷰하듯이 문제점을 찾아 `review-answer.md`에 작성하세요.

답변에는 아래 내용을 포함하세요.

1. 이 코드에서 문제가 될 수 있는 부분
2. 왜 문제인지
3. 실무에서 어떤 장애나 유지보수 문제가 생길 수 있는지
4. 어떻게 개선할지
5. 면접에서 1분 답변으로 말한다면 어떻게 말할지

정답, 힌트, 문제점 목록은 아직 보지 않습니다.

## 채점 후 볼 자료

답변 제출과 채점이 끝난 뒤에는 아래 순서로 봅니다.

1. `review-answer.md`
   - 핵심 문제 요약, 채점, 반드시 잡았어야 하는 문제를 먼저 봅니다.

2. `refactoring-plan.md`
   - 어떤 순서로 고칠지 정리한 문서입니다.

3. `solution/README.md`
   - 리팩토링된 코드의 변경 요약입니다.

4. `solution/src/main/java/com/example/point/refactoring/`
   - `_Refactored` 이름이 붙은 개선 코드 예시입니다.

5. `solution/src/test/java/com/example/point/refactoring/PointRefundService_RefactoredTest.java`
   - 리팩토링된 코드 기준 테스트 예시입니다.

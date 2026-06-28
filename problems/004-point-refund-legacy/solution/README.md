# 004. Point Refund Refactored Solution

이 코드는 유일한 정답이 아니라 면접에서 설명 가능한 개선 예시입니다.

## 주요 변경

1. `PointPayment`를 Request/Response로 직접 쓰지 않고 `PointRefundRequest_Refactored`, `PointRefundResponse_Refactored`로 분리했습니다.
2. Controller와 Service의 직접 `new` 생성을 제거하고 생성자 주입 형태로 바꿨습니다.
3. Service가 클라이언트 입력 상태를 신뢰하지 않고 Repository에서 기존 결제를 조회합니다.
4. Repository의 `findAll` 후 반복 `findById` 호출을 조건 조회 메서드로 바꿨습니다.
5. `RuntimeException` 문자열 대신 `PointRefundErrorCode_Refactored`를 가진 예외를 사용합니다.
6. static Util에 있던 환불 정책과 알림 책임을 `PointRefundPolicy_Refactored`, `RefundNotifier_Refactored`로 분리했습니다.

## 주요 파일

- `PointRefundController_Refactored.java`
- `PointRefundService_Refactored.java`
- `PointPaymentRepository_Refactored.java`
- `InMemoryPointPaymentRepository_Refactored.java`
- `PointRefundPolicy_Refactored.java`
- `RefundNotifier_Refactored.java`
- `PointRefundException_Refactored.java`
- `PointRefundService_RefactoredTest.java`

## 주석 규칙

핵심 변경 지점에는 `기존 코드:` / `수정 코드:` 비교 주석을 남겼습니다.

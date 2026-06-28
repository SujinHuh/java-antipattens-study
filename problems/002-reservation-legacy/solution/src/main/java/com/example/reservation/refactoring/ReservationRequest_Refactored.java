package com.example.reservation.refactoring;

public record ReservationRequest_Refactored(
        long userId,
        long roomId,
        int startHour,
        int endHour
) {
    public ReservationRequest_Refactored {
        // 기존 코드: Controller가 if문으로 시간 검증을 직접 수행했다.
        // 수정 코드: 요청 객체 생성 시 기본 입력 조건을 검증한다.
        if (startHour < 0 || endHour > 24 || startHour >= endHour) {
            throw new ReservationException_Refactored(ErrorCode_Refactored.INVALID_TIME_RANGE, "Invalid reservation time range");
        }
    }
}

package com.example.reservation.refactoring;

public class ReservationException_Refactored extends RuntimeException {
    // 기존 코드: "invalid", "already reserved" 같은 메시지만 있었다.
    // 수정 코드: 에러 코드를 함께 보관해 예외 원인을 분류할 수 있게 했다.
    private final ErrorCode_Refactored errorCode;

    public ReservationException_Refactored(ErrorCode_Refactored errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode_Refactored getErrorCode() {
        return errorCode;
    }
}

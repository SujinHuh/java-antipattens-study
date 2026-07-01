package com.example.delivery;

public class DeliveryAddressException extends RuntimeException {

    // 기존 문제: 메시지만 가진 커스텀 예외라 실패 분류와 응답 매핑 기준이 약하다.
    // public DeliveryAddressException(String message) {
    //     super(message);
    // }
    private final DeliveryAddressErrorCode errorCode;

    public DeliveryAddressException(DeliveryAddressErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public DeliveryAddressErrorCode errorCode() {
        return errorCode;
    }
}

enum DeliveryAddressErrorCode {
    INVALID_REQUEST,
    NOT_FOUND,
    NOT_OWNER,
    ALREADY_DELIVERING,
    BLOCKED_ADDRESS
}

class DeliveryAddressNotFoundException extends RuntimeException {

    DeliveryAddressNotFoundException(Long orderId) {
        super("delivery address not found. orderId=" + orderId);
    }
}

class InvalidDeliveryAddressRequestException extends RuntimeException {

    InvalidDeliveryAddressRequestException(String message) {
        super(message);
    }
}

record ErrorResponse(String code, String message) {
}

// @RestControllerAdvice
class DeliveryAddressExceptionHandler {

    // @ExceptionHandler(InvalidDeliveryAddressRequestException.class)
    ErrorResponse handleInvalidRequest(InvalidDeliveryAddressRequestException e) {
        return new ErrorResponse("INVALID_DELIVERY_ADDRESS_REQUEST", e.getMessage());
    }

    // @ExceptionHandler(DeliveryAddressNotFoundException.class)
    ErrorResponse handleNotFound(DeliveryAddressNotFoundException e) {
        return new ErrorResponse("DELIVERY_ADDRESS_NOT_FOUND", e.getMessage());
    }

    // @ExceptionHandler(DeliveryAddressException.class)
    ErrorResponse handleDeliveryAddress(DeliveryAddressException e) {
        return new ErrorResponse(e.errorCode().name(), "delivery address request failed");
    }
}

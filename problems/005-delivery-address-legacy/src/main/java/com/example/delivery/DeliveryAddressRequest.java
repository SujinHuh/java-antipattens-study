package com.example.delivery;

// 기존 문제: public field DTO라 Controller/Service에서 요청 객체를 쉽게 변경할 수 있다.
// public class DeliveryAddressRequest {
//     public Long orderId;
//     public Long userId;
//     public String zipCode;
//     public String address;
//     public String memo;
//     public int priority;
// }

public record DeliveryAddressRequest(
    /* @NotNull */ Long orderId,
    /* @NotNull */ Long userId,
    /* @NotBlank */ String zipCode,
    /* @NotBlank */ String address,
    String memo
) {
}

record DeliveryAddressResponse(
    Long orderId,
    String zipCode,
    String address,
    String status
) {

    static DeliveryAddressResponse from(DeliveryAddress address) {
        return new DeliveryAddressResponse(
            address.orderId(),
            address.zipCode(),
            address.address(),
            address.status().name()
        );
    }
}

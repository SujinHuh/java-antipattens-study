package com.example.delivery;

public class DeliveryAddress {

    // 기존 문제: public field라 어디서든 상태를 직접 변경할 수 있다.
    // public Long orderId;
    // public Long userId;
    // public String zipCode;
    // public String address;
    // public String status;
    // public String memo;
    // public boolean priorityAddress;
    private final Long orderId;
    private final Long userId;
    private String zipCode;
    private String address;
    private DeliveryAddressStatus status;
    private String memo;나
    private boolean priorityAddress;

    public DeliveryAddress(
        Long orderId,
        Long userId,
        String zipCode,
        String address,
        DeliveryAddressStatus status,
        boolean priorityAddress
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.zipCode = zipCode;
        this.address = address;
        this.status = status;
        this.priorityAddress = priorityAddress;
    }

    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean isDelivering() {
        return status == DeliveryAddressStatus.DELIVERING;
    }

    public boolean isAddressChanged() {
        return status == DeliveryAddressStatus.ADDRESS_CHANGED;
    }

    public void changeAddress(String zipCode, String address, String memo) {
        if (isDelivering()) {
            throw new DeliveryAddressException(DeliveryAddressErrorCode.ALREADY_DELIVERING);
        }

        this.zipCode = zipCode;
        this.address = address;
        this.memo = memo;
        this.status = DeliveryAddressStatus.ADDRESS_CHANGED;
    }

    public Long orderId() {
        return orderId;
    }

    public Long userId() {
        return userId;
    }

    public String zipCode() {
        return zipCode;
    }

    public String address() {
        return address;
    }

    public DeliveryAddressStatus status() {
        return status;
    }
}

enum DeliveryAddressStatus {
    READY,
    DELIVERING,
    ADDRESS_CHANGED
}

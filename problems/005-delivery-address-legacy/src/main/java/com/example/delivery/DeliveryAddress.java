package com.example.delivery;

public class DeliveryAddress {

    public Long orderId;
    public Long userId;
    public String zipCode;
    public String address;
    public String status;
    public String memo;
    public boolean priorityAddress;

    public DeliveryAddress(Long orderId, Long userId, String zipCode, String address, String status, boolean priorityAddress) {
        this.orderId = orderId;
        this.userId = userId;
        this.zipCode = zipCode;
        this.address = address;
        this.status = status;
        this.priorityAddress = priorityAddress;
    }
}

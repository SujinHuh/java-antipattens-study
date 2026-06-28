package com.example.delivery;

public class DeliveryAddressServiceTest {

    public void changeAddress() {
        DeliveryAddressService service = new DeliveryAddressService();

        DeliveryAddressRequest request = new DeliveryAddressRequest();
        request.orderId = 100L;
        request.userId = 1L;
        request.zipCode = "06234";
        request.address = "Seoul Seocho";
        request.memo = "home";

        service.changeAddress(request);
    }
}

package com.example.order;

public class OrderServiceTest {
    public void vipCustomerGetsDiscount() {
        OrderService orderService = new OrderService();

        String result = orderService.processOrder("VIP", 2, 10000);

        if (!result.contains("final price: 18000")) {
            throw new AssertionError("VIP discount failed");
        }
    }
}


package com.example.order;

public class OrderController {
    private final OrderService orderService = new OrderService();

    public String createOrder(String customerType, String itemCount, String pricePerItem) {
        int count = Integer.parseInt(itemCount);
        int price = Integer.parseInt(pricePerItem);

        if (count <= 0 || price <= 0) {
            return "400 BAD_REQUEST";
        }

        try {
            return orderService.processOrder(customerType, count, price);
        } catch (Exception e) {
            return "500 INTERNAL_SERVER_ERROR";
        }
    }
}

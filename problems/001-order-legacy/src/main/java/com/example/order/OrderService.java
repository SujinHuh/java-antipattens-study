package com.example.order;

import java.time.LocalDateTime;

public class OrderService {
    private final OrderRepository orderRepository = new OrderRepository();

    public String processOrder(String customerType, int itemCount, int pricePerItem) {
        int total = itemCount * pricePerItem;

        if (customerType.equals("VIP")) {
            total = total - (total / 10);
        } else if (customerType.equals("EMPLOYEE")) {
            total = total - (total / 5);
        }

        if (itemCount >= 10) {
            total = total - 500;
        }

        if (total < 0) {
            total = 0;
        }

        Order order = new Order();
        order.id = System.currentTimeMillis();
        order.customerType = customerType;
        order.itemCount = itemCount;
        order.pricePerItem = pricePerItem;
        order.finalPrice = total;
        order.createdAt = LocalDateTime.now().toString();

        orderRepository.save(order);

        return "Customer type: " + customerType
                + ", item count: " + itemCount
                + ", final price: " + total;
    }
}

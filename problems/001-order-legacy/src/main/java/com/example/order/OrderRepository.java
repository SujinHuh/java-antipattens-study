package com.example.order;

import java.util.ArrayList;
import java.util.List;

public class OrderRepository {
    private static final List<Order> ORDERS = new ArrayList<>();

    public void save(Order order) {
        if (order.finalPrice == 0) {
            throw new OrderException("invalid order");
        }

        ORDERS.add(order);
        System.out.println("saved order: " + order.id);
    }

    public Order findById(long id) {
        for (Order order : ORDERS) {
            if (order.id == id) {
                return order;
            }
        }

        return null;
    }
}

package com.example.order;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// @Repository
public class OrderRepository {

    private static final List<Order> orders = new ArrayList<>();

    static {
        orders.add(new Order(1L, 10L, 50000, "ACTIVE", "NORMAL", LocalDateTime.now().minusMinutes(10)));
        orders.add(new Order(2L, 10L, 100000, "ACTIVE", "PREORDER", LocalDateTime.now().minusMinutes(40)));
        orders.add(new Order(3L, 20L, 15000, "ACTIVE", "DIGITAL", LocalDateTime.now().minusMinutes(5)));
    }

    public Optional<Order> findById(Long id) {
        for (Order order : orders) {
            if (order.id.equals(id)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    public List<Order> findAll() {
        return orders;
    }

    public void save(Order order) {
        orders.add(order);
    }
}

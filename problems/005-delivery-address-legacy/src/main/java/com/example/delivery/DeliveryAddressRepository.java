package com.example.delivery;

import java.util.ArrayList;
import java.util.List;

// @Repository
public class DeliveryAddressRepository {

    private static final List<DeliveryAddress> addresses = new ArrayList<>();

    static {
        addresses.add(new DeliveryAddress(100L, 1L, "06123", "Seoul Gangnam", "READY", false));
        addresses.add(new DeliveryAddress(101L, 1L, "04524", "Seoul Junggu", "DELIVERING", false));
        addresses.add(new DeliveryAddress(102L, 2L, "13494", "Seongnam Bundang", "ADDRESS_CHANGED", true));
    }

    public DeliveryAddress findByOrderId(Long orderId) {
        for (DeliveryAddress address : addresses) {
            if (address.orderId.equals(orderId)) {
                return address;
            }
        }
        return null;
    }

    public List<DeliveryAddress> findAll() {
        return addresses;
    } // 방어적 복사르 하기

    public void save(DeliveryAddress address) {
        addresses.add(address);
    }
}

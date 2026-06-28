package com.example.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// @Service
public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository = new DeliveryAddressRepository();

    public Map<String, Object> changeAddress(DeliveryAddressRequest request) {
        if (request.userId == null || request.orderId == null) {
            throw new RuntimeException("userId or orderId is null");
        }

        DeliveryAddress current = deliveryAddressRepository.findByOrderId(request.orderId);
        if (current == null) {
            current = new DeliveryAddress(request.orderId, request.userId, request.zipCode, request.address, "READY", false);
        }

        if ("DELIVERING".equals(current.status)) {
            return Map.of("status", 200, "message", "already delivering");
        }

        if (DeliveryAddressUtil.isBlockedAddress(request.address)) {
            throw new DeliveryAddressException("blocked address");
        }

        if (request.priority > 0) {
            current.priorityAddress = true;
        }

        current.zipCode = request.zipCode;
        current.address = DeliveryAddressUtil.normalize(request.address);
        current.memo = request.memo;
        current.status = "ADDRESS_CHANGED";

        deliveryAddressRepository.save(current);
        DeliveryAddressUtil.sendAuditLog("address changed orderId=" + current.orderId + ", userId=" + current.userId);

        return Map.of("status", 200, "message", "OK", "address", current);
    }

    public List<String> getChangeHistory(Long userId) {
        List<DeliveryAddress> all = deliveryAddressRepository.findAll();
        List<String> result = new ArrayList<>();

        for (DeliveryAddress address : all) {
            DeliveryAddress latest = deliveryAddressRepository.findByOrderId(address.orderId);
            if (latest.userId.equals(userId) && latest.status.contains("CHANGE")) {
                result.add(latest.orderId + ":" + latest.address);
            }
        }

        return result;
    }
}

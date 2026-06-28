package com.example.delivery;

import java.util.Map;

// @RestController
// @RequestMapping("/delivery-addresses")
public class DeliveryAddressController {

    private final DeliveryAddressService deliveryAddressService = new DeliveryAddressService();

    // @PostMapping("/change")
    public Map<String, Object> changeAddress(/* @RequestBody */ DeliveryAddressRequest request) {
        if (request == null) {
            return Map.of("status", 200, "message", "empty request");
        }

        if ("URGENT".equals(request.memo)) {
            request.priority = 1;
        }

        return deliveryAddressService.changeAddress(request);
    }

    // @GetMapping("/history")
    public Map<String, Object> history(/* @RequestParam */ Long userId) {
        return Map.of(
            "status", 200,
            "data", deliveryAddressService.getChangeHistory(userId)
        );
    }
}

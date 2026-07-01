package com.example.delivery;

import java.util.List;

// @RestController
// @RequestMapping("/delivery-addresses")
public class DeliveryAddressController {

    // 기존 문제: Controller가 Service를 직접 생성해서 DI, 테스트 대체, AOP 적용이 어려워진다.
    // private final DeliveryAddressService deliveryAddressService = new DeliveryAddressService();
    // 수정 방향: 생성자 주입으로 Service를 받는다.
    private final DeliveryAddressService deliveryAddressService;

    public DeliveryAddressController(DeliveryAddressService deliveryAddressService) {
        this.deliveryAddressService = deliveryAddressService;
    }

    // @PostMapping("/change")
    public ApiResponse<DeliveryAddressResponse> changeAddress(/* @Valid @RequestBody */ DeliveryAddressRequest request) {
        // 기존 문제: null 요청을 성공처럼 200 Map으로 감싼다.
        // if (request == null) {
        //     return Map.of("status", 200, "message", "empty request");
        // }
        if (request == null) {
            throw new InvalidDeliveryAddressRequestException("request is required");
        }

        // 기존 문제: Controller가 요청 DTO를 직접 수정하고 비즈니스 판단을 수행한다.
        // if ("URGENT".equals(request.memo)) {
        //     request.priority = 1;
        // }

        DeliveryAddressResponse response = deliveryAddressService.changeAddress(request);
        return ApiResponse.ok(response);
    }

    // @GetMapping("/history")
    public ApiResponse<List<String>> history(/* @RequestParam */ Long userId) {
        return ApiResponse.ok(deliveryAddressService.getChangeHistory(userId));
    }
}

record ApiResponse<T>(int status, T data) {

    static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, data);
    }
}

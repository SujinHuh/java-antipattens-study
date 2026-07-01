package com.example.delivery;

import java.util.List;

// @Service
public class DeliveryAddressService {

    // 기존 문제: Service가 Repository를 직접 생성해서 테스트 대체와 DI가 어렵다.
    // private final DeliveryAddressRepository deliveryAddressRepository = new DeliveryAddressRepository();
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final BlockedAddressPolicy blockedAddressPolicy;
    private final DeliveryAuditLogger deliveryAuditLogger;

    public DeliveryAddressService(
        DeliveryAddressRepository deliveryAddressRepository,
        BlockedAddressPolicy blockedAddressPolicy,
        DeliveryAuditLogger deliveryAuditLogger
    ) {
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.blockedAddressPolicy = blockedAddressPolicy;
        this.deliveryAuditLogger = deliveryAuditLogger;
    }

    // @Transactional
    public DeliveryAddressResponse changeAddress(DeliveryAddressRequest request) {
        // 기존 문제: RuntimeException 직접 사용으로 실패 원인과 응답 정책이 드러나지 않는다.
        // if (request.userId == null || request.orderId == null) {
        //     throw new RuntimeException("userId or orderId is null");
        // }
        if (request == null || request.userId() == null || request.orderId() == null) {
            throw new InvalidDeliveryAddressRequestException("userId and orderId are required");
        }

        // 기존 문제: 조회 실패를 요청값 기반 새 Entity 생성으로 숨긴다.
        // DeliveryAddress current = deliveryAddressRepository.findByOrderId(request.orderId);
        // if (current == null) {
        //     current = new DeliveryAddress(request.orderId, request.userId, request.zipCode, request.address, "READY", false);
        // }
        DeliveryAddress current = deliveryAddressRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new DeliveryAddressNotFoundException(request.orderId()));

        if (!current.isOwnedBy(request.userId())) {
            throw new DeliveryAddressException(DeliveryAddressErrorCode.NOT_OWNER);
        }

        // 기존 문제: 배송중 상태를 200 성공 메시지로 반환해서 실패/충돌을 숨긴다.
        // if ("DELIVERING".equals(current.status)) {
        //     return Map.of("status", 200, "message", "already delivering");
        // }
        if (current.isDelivering()) {
            throw new DeliveryAddressException(DeliveryAddressErrorCode.ALREADY_DELIVERING);
        }

        // 기존 문제: static Util이 도메인 정책을 판단한다.
        // if (DeliveryAddressUtil.isBlockedAddress(request.address)) {
        //     throw new DeliveryAddressException("blocked address");
        // }
        if (blockedAddressPolicy.isBlocked(request.address())) {
            throw new DeliveryAddressException(DeliveryAddressErrorCode.BLOCKED_ADDRESS);
        }

        // 기존 문제: 요청 priority를 신뢰해서 Entity 상태를 직접 바꾼다.
        // if (request.priority > 0) {
        //     current.priorityAddress = true;
        // }

        // 기존 문제: public field를 직접 수정하고 문자열 상태값을 대입한다.
        // current.zipCode = request.zipCode;
        // current.address = DeliveryAddressUtil.normalize(request.address);
        // current.memo = request.memo;
        // current.status = "ADDRESS_CHANGED";
        current.changeAddress(
            request.zipCode(),
            DeliveryAddressNormalizer.normalize(request.address()),
            request.memo()
        );

        DeliveryAddress saved = deliveryAddressRepository.save(current);

        // 기존 문제: static Util로 감사 로그를 출력한다.
        // DeliveryAddressUtil.sendAuditLog("address changed orderId=" + current.orderId + ", userId=" + current.userId);
        deliveryAuditLogger.addressChanged(saved.orderId(), saved.userId());

        // 기존 문제: Service가 HTTP 응답처럼 status/message/address Map을 만든다.
        // return Map.of("status", 200, "message", "OK", "address", current);
        return DeliveryAddressResponse.from(saved);
    }

    public List<String> getChangeHistory(Long userId) {
        // 기존 문제: findAll 후 반복문 안에서 findByOrderId를 다시 호출한다.
        // List<DeliveryAddress> all = deliveryAddressRepository.findAll();
        // for (DeliveryAddress address : all) {
        //     DeliveryAddress latest = deliveryAddressRepository.findByOrderId(address.orderId);
        //     ...
        // }
        return deliveryAddressRepository.findChangedAddressesByUserId(userId)
            .stream()
            .map(address -> address.orderId() + ":" + address.address())
            .toList();
    }
}

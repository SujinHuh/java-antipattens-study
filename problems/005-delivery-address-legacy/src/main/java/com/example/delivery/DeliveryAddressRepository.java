package com.example.delivery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// @Repository
public class DeliveryAddressRepository {

    // 기존 문제: static final List는 참조 재할당만 막고 내부 변경은 가능해서 테스트 격리와 동시성에 취약하다.
    // private static final List<DeliveryAddress> addresses = new ArrayList<>();
    // 수정 방향: 예제에서는 orderId 기준 Map으로 저장하고, 실제 DB라면 조건 조회와 인덱스를 고려한다.
    private final Map<Long, DeliveryAddress> addresses = new HashMap<>();

    public DeliveryAddressRepository() {
        save(new DeliveryAddress(100L, 1L, "06123", "Seoul Gangnam", DeliveryAddressStatus.READY, false));
        save(new DeliveryAddress(101L, 1L, "04524", "Seoul Junggu", DeliveryAddressStatus.DELIVERING, false));
        save(new DeliveryAddress(102L, 2L, "13494", "Seongnam Bundang", DeliveryAddressStatus.ADDRESS_CHANGED, true));
    }

    public Optional<DeliveryAddress> findByOrderId(Long orderId) {
        // 기존 문제: 조회 실패 시 null 반환.
        // return null;
        return Optional.ofNullable(addresses.get(orderId));
    }

    public List<DeliveryAddress> findChangedAddressesByUserId(Long userId) {
        // 기존 문제: findAll()로 전체 저장소를 노출하고 Service에서 필터링한다.
        // public List<DeliveryAddress> findAll() {
        //     return addresses;
        // }
        return addresses.values().stream()
            .filter(address -> address.isOwnedBy(userId))
            .filter(DeliveryAddress::isAddressChanged)
            .toList();
    }

    public DeliveryAddress save(DeliveryAddress address) {
        // 기존 문제: 변경 유스케이스인데 항상 add만 해서 중복 데이터가 쌓일 수 있다.
        // addresses.add(address);
        addresses.put(address.orderId(), address);
        return address;
    }
}

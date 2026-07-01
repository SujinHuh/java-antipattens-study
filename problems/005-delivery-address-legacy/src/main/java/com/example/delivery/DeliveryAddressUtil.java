package com.example.delivery;

public final class DeliveryAddressUtil {

    private DeliveryAddressUtil() {
    }

    public static String normalize(String address) {
        return DeliveryAddressNormalizer.normalize(address);
    }

    // 기존 문제: static Util이 도메인 정책을 판단한다.
    // public static boolean isBlockedAddress(String address) {
    //     return address.contains("TEST") || address.contains("BLOCK");
    // }

    // 기존 문제: System.out.println으로 감사 로그를 남긴다.
    // public static void sendAuditLog(String message) {
    //     System.out.println("[DELIVERY] " + message);
    // }
}

final class DeliveryAddressNormalizer {

    private DeliveryAddressNormalizer() {
    }

    static String normalize(String address) {
        if (address == null) {
            return "";
        }

        return address.trim().replace("  ", " ");
    }
}

// @Component
class BlockedAddressPolicy {

    boolean isBlocked(String address) {
        return address != null && (address.contains("TEST") || address.contains("BLOCK"));
    }
}

// @Component
class DeliveryAuditLogger {

    void addressChanged(Long orderId, Long userId) {
        // 기존 문제: System.out.println("[DELIVERY] " + message);
        // 수정 방향: 실제 서비스에서는 Logger를 사용해 레벨, 포맷, 추적 정보를 관리한다.
        // log.info("delivery address changed. orderId={}, userId={}", orderId, userId);
    }
}

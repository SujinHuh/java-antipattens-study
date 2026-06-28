package com.example.delivery;

// @Component
public class DeliveryAddressUtil {

    public static String normalize(String address) {
        if (address == null) {
            return "";
        }

        return address.trim().replace("  ", " ");
    }

    public static boolean isBlockedAddress(String address) {
        if (address == null) {
            return false;
        }

        return address.contains("TEST") || address.contains("BLOCK");
    }

    public static void sendAuditLog(String message) {
        System.out.println("[DELIVERY] " + message);
    }
}

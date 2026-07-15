package com.notifymesh.vendor;

/**
 * Outcome of a single vendor delivery attempt.
 */
public record DeliveryResult(
        boolean success,
        String vendorName,
        String providerMessageId,
        String errorMessage,
        long latencyMs
) {

    public static DeliveryResult success(String vendorName, String providerMessageId, long latencyMs) {
        return new DeliveryResult(true, vendorName, providerMessageId, null, latencyMs);
    }

    public static DeliveryResult failure(String vendorName, String errorMessage, long latencyMs) {
        return new DeliveryResult(false, vendorName, null, errorMessage, latencyMs);
    }
}

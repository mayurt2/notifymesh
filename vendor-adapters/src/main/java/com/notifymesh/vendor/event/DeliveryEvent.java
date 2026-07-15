package com.notifymesh.vendor.event;

import com.notifymesh.vendor.NotificationChannel;

import java.time.Instant;

/**
 * Published to {@code notification.delivered} / {@code notification.failed} once the router
 * has a final outcome for a request (after exhausting the failover chain, if needed). This is
 * the audit trail schema: audit-service indexes it as-is into Elasticsearch.
 *
 * @param vendor           the vendor that ultimately delivered it, or the last one tried if failed
 * @param failoverOccurred true if more than one vendor was attempted for this request
 * @param errorMessage     set only when status is FAILED
 */
public record DeliveryEvent(
        String requestId,
        NotificationChannel channel,
        String vendor,
        DeliveryStatus status,
        long latencyMs,
        Instant timestamp,
        boolean failoverOccurred,
        String errorMessage
) {

    public static DeliveryEvent delivered(String requestId, NotificationChannel channel, String vendor,
                                           long latencyMs, boolean failoverOccurred) {
        return new DeliveryEvent(requestId, channel, vendor, DeliveryStatus.DELIVERED, latencyMs,
                Instant.now(), failoverOccurred, null);
    }

    public static DeliveryEvent failed(String requestId, NotificationChannel channel, String lastVendor,
                                        long latencyMs, boolean failoverOccurred, String errorMessage) {
        return new DeliveryEvent(requestId, channel, lastVendor, DeliveryStatus.FAILED, latencyMs,
                Instant.now(), failoverOccurred, errorMessage);
    }
}

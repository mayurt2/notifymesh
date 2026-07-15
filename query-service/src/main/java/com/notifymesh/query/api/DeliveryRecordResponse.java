package com.notifymesh.query.api;

import com.notifymesh.query.document.DeliveryDocument;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;

import java.time.Instant;

public record DeliveryRecordResponse(
        String requestId,
        NotificationChannel channel,
        String vendor,
        DeliveryStatus status,
        long latencyMs,
        Instant timestamp,
        boolean failoverOccurred,
        String errorMessage
) {

    public static DeliveryRecordResponse from(DeliveryDocument document) {
        return new DeliveryRecordResponse(
                document.getRequestId(),
                document.getChannel(),
                document.getVendor(),
                document.getStatus(),
                document.getLatencyMs(),
                document.getTimestamp(),
                document.isFailoverOccurred(),
                document.getErrorMessage());
    }
}

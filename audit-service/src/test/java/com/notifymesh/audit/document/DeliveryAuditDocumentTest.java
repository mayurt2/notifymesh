package com.notifymesh.audit.document;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeliveryAuditDocumentTest {

    @Test
    void mapsDeliveredEventFieldsOneToOne() {
        DeliveryEvent event = DeliveryEvent.delivered("req-1", NotificationChannel.SMS, "mock-sms-vendor-b", 123, true);

        DeliveryAuditDocument document = DeliveryAuditDocument.from(event);

        assertEquals("req-1", document.getRequestId());
        assertEquals(NotificationChannel.SMS, document.getChannel());
        assertEquals("mock-sms-vendor-b", document.getVendor());
        assertEquals(DeliveryStatus.DELIVERED, document.getStatus());
        assertEquals(123, document.getLatencyMs());
        assertEquals(event.timestamp(), document.getTimestamp());
        assertEquals(true, document.isFailoverOccurred());
    }

    @Test
    void mapsFailedEventWithErrorMessage() {
        DeliveryEvent event = DeliveryEvent.failed("req-2", NotificationChannel.EMAIL, "mock-email-vendor", 55, false, "vendor down");

        DeliveryAuditDocument document = DeliveryAuditDocument.from(event);

        assertEquals(DeliveryStatus.FAILED, document.getStatus());
        assertEquals("vendor down", document.getErrorMessage());
        assertFalse(document.isFailoverOccurred());
    }
}

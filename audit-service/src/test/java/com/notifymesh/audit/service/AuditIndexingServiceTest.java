package com.notifymesh.audit.service;

import com.notifymesh.audit.document.DeliveryAuditDocument;
import com.notifymesh.audit.repository.DeliveryAuditRepository;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditIndexingServiceTest {

    @Mock
    private DeliveryAuditRepository repository;

    @Test
    void savesMappedDocumentForEvent() {
        DeliveryEvent event = DeliveryEvent.delivered("req-1", NotificationChannel.SMS, "mock-sms-vendor-a", 42, false);

        new AuditIndexingService(repository).index(event);

        ArgumentCaptor<DeliveryAuditDocument> captor = ArgumentCaptor.forClass(DeliveryAuditDocument.class);
        verify(repository).save(captor.capture());
        assertEquals("req-1", captor.getValue().getRequestId());
        assertEquals("mock-sms-vendor-a", captor.getValue().getVendor());
    }
}

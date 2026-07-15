package com.notifymesh.router.delivery;

import com.notifymesh.router.kafka.DeliveryEventPublisher;
import com.notifymesh.vendor.DeliveryResult;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorException;
import com.notifymesh.vendor.VendorRegistry;
import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private VendorRegistry vendorRegistry;

    @Mock
    private VendorAdapter primaryVendor;

    @Mock
    private VendorAdapter fallbackVendor;

    @Mock
    private DeliveryEventPublisher eventPublisher;

    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        RetryConfig fastRetryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(1))
                .build();
        deliveryService = new DeliveryService(
                vendorRegistry,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.of(fastRetryConfig),
                eventPublisher,
                new SimpleMeterRegistry());

        lenient().when(primaryVendor.getVendorName()).thenReturn("vendor-a");
        lenient().when(fallbackVendor.getVendorName()).thenReturn("vendor-b");
    }

    @Test
    void deliversViaTopVendorWithoutFailoverOnSuccess() {
        NotificationRequest request = new NotificationRequest("req-1", NotificationChannel.SMS, "+1", null, "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.SMS)).thenReturn(List.of(primaryVendor));
        when(primaryVendor.send(request)).thenReturn(DeliveryResult.success("vendor-a", "msg-1", 10));

        deliveryService.deliver(request);

        DeliveryEvent event = captureEvent();
        assertEquals(DeliveryStatus.DELIVERED, event.status());
        assertEquals("vendor-a", event.vendor());
        assertFalse(event.failoverOccurred());
    }

    @Test
    void failsOverToNextVendorWhenPrimaryExhaustsRetries() {
        NotificationRequest request = new NotificationRequest("req-2", NotificationChannel.SMS, "+1", null, "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.SMS)).thenReturn(List.of(primaryVendor, fallbackVendor));
        when(primaryVendor.send(request)).thenThrow(new VendorException("simulated failure"));
        when(fallbackVendor.send(request)).thenReturn(DeliveryResult.success("vendor-b", "msg-2", 20));

        deliveryService.deliver(request);

        DeliveryEvent event = captureEvent();
        assertEquals(DeliveryStatus.DELIVERED, event.status());
        assertEquals("vendor-b", event.vendor());
        assertTrue(event.failoverOccurred());
    }

    @Test
    void publishesFailedEventAfterExhaustingWholeChain() {
        NotificationRequest request = new NotificationRequest("req-3", NotificationChannel.SMS, "+1", null, "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.SMS)).thenReturn(List.of(primaryVendor, fallbackVendor));
        when(primaryVendor.send(request)).thenThrow(new VendorException("primary down"));
        when(fallbackVendor.send(request)).thenThrow(new VendorException("fallback down"));

        deliveryService.deliver(request);

        DeliveryEvent event = captureEvent();
        assertEquals(DeliveryStatus.FAILED, event.status());
        assertEquals("vendor-b", event.vendor());
        assertTrue(event.failoverOccurred());
        assertEquals("fallback down", event.errorMessage());
    }

    @Test
    void publishesFailedEventWithoutFailoverWhenOnlyOneVendorConfigured() {
        NotificationRequest request = new NotificationRequest("req-4", NotificationChannel.EMAIL, "a@example.com", "s", "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.EMAIL)).thenReturn(List.of(primaryVendor));
        when(primaryVendor.send(request)).thenThrow(new VendorException("down"));

        deliveryService.deliver(request);

        assertFalse(captureEvent().failoverOccurred());
    }

    private DeliveryEvent captureEvent() {
        ArgumentCaptor<DeliveryEvent> captor = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }
}

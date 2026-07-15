package com.notifymesh.router.delivery;

import com.notifymesh.vendor.DeliveryResult;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorException;
import com.notifymesh.vendor.VendorRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private VendorRegistry vendorRegistry;

    @Mock
    private VendorAdapter primaryVendor;

    @Test
    void deliversUsingTopPriorityVendorOnSuccess() {
        NotificationRequest request = new NotificationRequest("req-1", NotificationChannel.SMS, "+10000000000", null, "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.SMS)).thenReturn(List.of(primaryVendor));
        when(primaryVendor.send(request)).thenReturn(DeliveryResult.success("mock-sms-vendor-a", "msg-1", 42));

        new DeliveryService(vendorRegistry).deliver(request);

        verify(primaryVendor, times(1)).send(request);
    }

    @Test
    void swallowsVendorExceptionWithoutPropagating() {
        NotificationRequest request = new NotificationRequest("req-2", NotificationChannel.SMS, "+10000000000", null, "hi");
        when(vendorRegistry.getFailoverChain(NotificationChannel.SMS)).thenReturn(List.of(primaryVendor));
        when(primaryVendor.send(request)).thenThrow(new VendorException("simulated failure"));

        assertDoesNotThrow(() -> new DeliveryService(vendorRegistry).deliver(request));

        verify(primaryVendor, times(1)).send(request);
    }
}

package com.notifymesh.vendor;

import com.notifymesh.vendor.mock.MockSmsVendorA;
import com.notifymesh.vendor.mock.MockSmsVendorB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendorRegistryTest {

    @Test
    void returnsRegisteredVendorsInPriorityOrder() {
        VendorRegistry registry = new VendorRegistry();
        VendorAdapter primary = new MockSmsVendorA(0.0);
        VendorAdapter fallback = new MockSmsVendorB(0.0);

        registry.register(NotificationChannel.SMS, List.of(primary, fallback));

        List<VendorAdapter> chain = registry.getFailoverChain(NotificationChannel.SMS);
        assertEquals(2, chain.size());
        assertEquals("mock-sms-vendor-a", chain.get(0).getVendorName());
        assertEquals("mock-sms-vendor-b", chain.get(1).getVendorName());
    }

    @Test
    void throwsWhenNoVendorsRegisteredForChannel() {
        VendorRegistry registry = new VendorRegistry();
        assertThrows(IllegalStateException.class, () -> registry.getFailoverChain(NotificationChannel.EMAIL));
    }

    @Test
    void mockVendorWithZeroFailureRateAlwaysSucceeds() {
        VendorAdapter vendor = new MockSmsVendorA(0.0);
        NotificationRequest request = new NotificationRequest("req-1", NotificationChannel.SMS, "+10000000000", null, "hello");

        DeliveryResult result = vendor.send(request);

        assertEquals(true, result.success());
        assertEquals("mock-sms-vendor-a", result.vendorName());
    }
}

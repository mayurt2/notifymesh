package com.notifymesh.router.config;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendorRegistryConfigTest {

    @Test
    void registersSmsVendorsInPrimaryThenFallbackOrder() {
        VendorProperties properties = new VendorProperties();
        properties.getSms().setPrimaryFailureRate(0.15);
        properties.getSms().setFallbackFailureRate(0.03);
        properties.getEmail().setFailureRate(0.05);
        properties.getWhatsapp().setFailureRate(0.10);

        VendorRegistry registry = new VendorRegistryConfig().vendorRegistry(properties);

        List<VendorAdapter> smsChain = registry.getFailoverChain(NotificationChannel.SMS);
        assertEquals(2, smsChain.size());
        assertEquals("mock-sms-vendor-a", smsChain.get(0).getVendorName());
        assertEquals("mock-sms-vendor-b", smsChain.get(1).getVendorName());

        assertEquals(1, registry.getFailoverChain(NotificationChannel.EMAIL).size());
        assertEquals(1, registry.getFailoverChain(NotificationChannel.WHATSAPP).size());
    }
}

package com.notifymesh.router.config;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.VendorRegistry;
import com.notifymesh.vendor.mock.MockEmailVendor;
import com.notifymesh.vendor.mock.MockSmsVendorA;
import com.notifymesh.vendor.mock.MockSmsVendorB;
import com.notifymesh.vendor.mock.MockWhatsAppVendor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Wires the mock vendor implementations into a {@link VendorRegistry}, in priority order per
 * channel. This is the only place that knows about concrete vendor classes — the router's
 * dispatch logic only ever talks to {@link VendorRegistry} and the {@code VendorAdapter}
 * interface, so adding a real vendor later means adding it here, not touching the router.
 */
@Configuration
@EnableConfigurationProperties(VendorProperties.class)
public class VendorRegistryConfig {

    @Bean
    public VendorRegistry vendorRegistry(VendorProperties properties) {
        VendorRegistry registry = new VendorRegistry();

        registry.register(NotificationChannel.SMS, List.of(
                new MockSmsVendorA(properties.getSms().getPrimaryFailureRate()),
                new MockSmsVendorB(properties.getSms().getFallbackFailureRate())
        ));
        registry.register(NotificationChannel.EMAIL, List.of(
                new MockEmailVendor(properties.getEmail().getFailureRate())
        ));
        registry.register(NotificationChannel.WHATSAPP, List.of(
                new MockWhatsAppVendor(properties.getWhatsapp().getFailureRate())
        ));

        return registry;
    }
}

package com.notifymesh.vendor.mock;

import com.notifymesh.vendor.NotificationChannel;

/** Mock WhatsApp Business API vendor (default 10% failure rate). */
public class MockWhatsAppVendor extends AbstractMockVendor {

    public MockWhatsAppVendor(double failureRate) {
        super("mock-whatsapp-vendor", NotificationChannel.WHATSAPP, failureRate, 100, 300);
    }

    public MockWhatsAppVendor() {
        this(0.10);
    }
}

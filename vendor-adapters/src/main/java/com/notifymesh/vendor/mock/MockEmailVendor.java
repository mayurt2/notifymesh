package com.notifymesh.vendor.mock;

import com.notifymesh.vendor.NotificationChannel;

/** Mock email vendor, modeled on SendGrid/SES-style latency (default 5% failure rate). */
public class MockEmailVendor extends AbstractMockVendor {

    public MockEmailVendor(String vendorName, double failureRate) {
        super(vendorName, NotificationChannel.EMAIL, failureRate, 50, 200);
    }

    public MockEmailVendor(double failureRate) {
        this("mock-email-vendor", failureRate);
    }

    public MockEmailVendor() {
        this("mock-email-vendor", 0.05);
    }
}

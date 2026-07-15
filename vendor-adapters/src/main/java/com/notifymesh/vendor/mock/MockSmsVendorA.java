package com.notifymesh.vendor.mock;

import com.notifymesh.vendor.NotificationChannel;

/** Primary SMS vendor: fast, occasionally flaky (default 15% failure rate). */
public class MockSmsVendorA extends AbstractMockVendor {

    public MockSmsVendorA(double failureRate) {
        super("mock-sms-vendor-a", NotificationChannel.SMS, failureRate, 20, 120);
    }

    public MockSmsVendorA() {
        this(0.15);
    }
}

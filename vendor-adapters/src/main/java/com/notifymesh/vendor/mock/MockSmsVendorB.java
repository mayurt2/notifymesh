package com.notifymesh.vendor.mock;

import com.notifymesh.vendor.NotificationChannel;

/** Fallback SMS vendor: slower but more reliable (default 3% failure rate). */
public class MockSmsVendorB extends AbstractMockVendor {

    public MockSmsVendorB(double failureRate) {
        super("mock-sms-vendor-b", NotificationChannel.SMS, failureRate, 80, 250);
    }

    public MockSmsVendorB() {
        this(0.03);
    }
}

package com.notifymesh.vendor.mock;

import com.notifymesh.vendor.DeliveryResult;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorException;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base class for mock vendors: simulates network latency and a configurable failure rate
 * so the router's circuit-breaker/failover path has something real to react to.
 */
public abstract class AbstractMockVendor implements VendorAdapter {

    private final String vendorName;
    private final NotificationChannel channel;
    private final double failureRate;
    private final long minLatencyMs;
    private final long maxLatencyMs;

    protected AbstractMockVendor(String vendorName, NotificationChannel channel,
                                  double failureRate, long minLatencyMs, long maxLatencyMs) {
        this.vendorName = vendorName;
        this.channel = channel;
        this.failureRate = failureRate;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public NotificationChannel getChannel() {
        return channel;
    }

    @Override
    public DeliveryResult send(NotificationRequest request) throws VendorException {
        long latency = ThreadLocalRandom.current().nextLong(minLatencyMs, maxLatencyMs + 1);
        simulateLatency(latency);

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new VendorException(vendorName + " simulated delivery failure for request " + request.requestId());
        }

        return DeliveryResult.success(vendorName, vendorName + "-" + request.requestId(), latency);
    }

    private void simulateLatency(long latencyMs) {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VendorException("Interrupted while simulating vendor latency", e);
        }
    }
}

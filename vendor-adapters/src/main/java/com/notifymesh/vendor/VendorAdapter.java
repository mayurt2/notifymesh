package com.notifymesh.vendor;

/**
 * A single notification vendor/provider integration.
 * <p>
 * Real implementations would wrap a vendor SDK or HTTP client (Twilio, SendGrid, SES, etc).
 * This project ships mock implementations that simulate configurable failure rates so the
 * router's failover path is actually exercised.
 */
public interface VendorAdapter {

    /**
     * Unique, stable name used in routing config, metrics, and audit records.
     */
    String getVendorName();

    /**
     * The channel this adapter delivers on.
     */
    NotificationChannel getChannel();

    /**
     * Attempt delivery. Implementations should throw {@link VendorException} on failure
     * rather than returning a failed {@link DeliveryResult}, so Resilience4j can treat it
     * as a recorded failure for the circuit breaker.
     */
    DeliveryResult send(NotificationRequest request) throws VendorException;
}

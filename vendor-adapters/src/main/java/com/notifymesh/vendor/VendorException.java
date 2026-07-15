package com.notifymesh.vendor;

/**
 * Raised by a {@link VendorAdapter} when a delivery attempt fails.
 * Callers (the router) treat this as a signal to fail over to the next vendor.
 */
public class VendorException extends RuntimeException {

    public VendorException(String message) {
        super(message);
    }

    public VendorException(String message, Throwable cause) {
        super(message, cause);
    }
}

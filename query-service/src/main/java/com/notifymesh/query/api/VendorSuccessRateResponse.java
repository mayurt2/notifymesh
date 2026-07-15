package com.notifymesh.query.api;

public record VendorSuccessRateResponse(String vendor, long total, long delivered, long failed, double successRate) {
}

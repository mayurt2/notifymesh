package com.notifymesh.ingestion.api;

public record NotificationResponse(String requestId, Status status) {

    public enum Status {
        ACCEPTED,
        DUPLICATE
    }

    public static NotificationResponse accepted(String requestId) {
        return new NotificationResponse(requestId, Status.ACCEPTED);
    }

    public static NotificationResponse duplicate(String requestId) {
        return new NotificationResponse(requestId, Status.DUPLICATE);
    }
}

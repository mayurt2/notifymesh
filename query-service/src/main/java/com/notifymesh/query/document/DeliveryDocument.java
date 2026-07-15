package com.notifymesh.query.document;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * query-service's own read model for the {@code notifymesh-deliveries} index, mapped
 * independently from audit-service's write-side document — each service owns its own view of
 * the shared index rather than sharing an entity class across the service boundary.
 */
public class DeliveryDocument {

    @Id
    private String requestId;

    @Field(type = FieldType.Keyword)
    private NotificationChannel channel;

    @Field(type = FieldType.Keyword)
    private String vendor;

    @Field(type = FieldType.Keyword)
    private DeliveryStatus status;

    private long latencyMs;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant timestamp;

    private boolean failoverOccurred;

    private String errorMessage;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFailoverOccurred() {
        return failoverOccurred;
    }

    public void setFailoverOccurred(boolean failoverOccurred) {
        this.failoverOccurred = failoverOccurred;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

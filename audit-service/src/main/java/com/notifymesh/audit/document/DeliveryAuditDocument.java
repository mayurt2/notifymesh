package com.notifymesh.audit.document;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Elasticsearch document for a single delivery outcome. Indexed with {@code requestId} as the
 * document id so a redelivered Kafka event (e.g. after a consumer rebalance) overwrites the
 * existing document instead of creating a duplicate.
 */
@Document(indexName = "#{@auditIndexName}")
public class DeliveryAuditDocument {

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

    public DeliveryAuditDocument() {
    }

    public DeliveryAuditDocument(String requestId, NotificationChannel channel, String vendor,
                                  DeliveryStatus status, long latencyMs, Instant timestamp,
                                  boolean failoverOccurred, String errorMessage) {
        this.requestId = requestId;
        this.channel = channel;
        this.vendor = vendor;
        this.status = status;
        this.latencyMs = latencyMs;
        this.timestamp = timestamp;
        this.failoverOccurred = failoverOccurred;
        this.errorMessage = errorMessage;
    }

    public static DeliveryAuditDocument from(DeliveryEvent event) {
        return new DeliveryAuditDocument(
                event.requestId(),
                event.channel(),
                event.vendor(),
                event.status(),
                event.latencyMs(),
                event.timestamp(),
                event.failoverOccurred(),
                event.errorMessage());
    }

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

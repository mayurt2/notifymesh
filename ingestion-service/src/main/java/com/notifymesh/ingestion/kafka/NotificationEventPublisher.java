package com.notifymesh.ingestion.kafka;

import com.notifymesh.vendor.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;
    private final String topic;

    public NotificationEventPublisher(KafkaTemplate<String, NotificationRequest> kafkaTemplate,
                                       @Value("${notifymesh.kafka.topic.notification-requested}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publishes keyed by requestId so all events for the same notification land on the same
     * partition, preserving order if it's ever retried or amended.
     * <p>
     * {@code send()} is async; without this callback a broker-side failure (e.g. the topic not
     * yet fully propagated right after a fresh broker/consumer start) would be silently dropped
     * instead of surfacing anywhere.
     */
    public void publish(NotificationRequest request) {
        kafkaTemplate.send(topic, request.requestId(), request).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("failed to publish notification.requested requestId={}", request.requestId(), ex);
            }
        });
    }
}

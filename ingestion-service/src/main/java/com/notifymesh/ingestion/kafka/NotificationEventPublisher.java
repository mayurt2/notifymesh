package com.notifymesh.ingestion.kafka;

import com.notifymesh.vendor.NotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventPublisher {

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
     */
    public void publish(NotificationRequest request) {
        kafkaTemplate.send(topic, request.requestId(), request);
    }
}

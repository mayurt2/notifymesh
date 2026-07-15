package com.notifymesh.router.kafka;

import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisher.class);

    private final KafkaTemplate<String, DeliveryEvent> kafkaTemplate;
    private final String deliveredTopic;
    private final String failedTopic;

    public DeliveryEventPublisher(KafkaTemplate<String, DeliveryEvent> kafkaTemplate,
                                   @Value("${notifymesh.kafka.topic.notification-delivered}") String deliveredTopic,
                                   @Value("${notifymesh.kafka.topic.notification-failed}") String failedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deliveredTopic = deliveredTopic;
        this.failedTopic = failedTopic;
    }

    /**
     * {@code send()} is async; without this callback a broker-side failure would be silently
     * dropped instead of surfacing anywhere (observed in practice right after a fresh
     * broker/consumer start, before the topic is fully propagated).
     */
    public void publish(DeliveryEvent event) {
        String topic = event.status() == DeliveryStatus.DELIVERED ? deliveredTopic : failedTopic;
        kafkaTemplate.send(topic, event.requestId(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("failed to publish {} requestId={}", event.status(), event.requestId(), ex);
            }
        });
    }
}

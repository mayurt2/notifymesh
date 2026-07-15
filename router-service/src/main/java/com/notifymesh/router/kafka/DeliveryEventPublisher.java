package com.notifymesh.router.kafka;

import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {

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

    public void publish(DeliveryEvent event) {
        String topic = event.status() == DeliveryStatus.DELIVERED ? deliveredTopic : failedTopic;
        kafkaTemplate.send(topic, event.requestId(), event);
    }
}

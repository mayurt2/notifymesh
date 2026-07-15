package com.notifymesh.audit.kafka;

import com.notifymesh.audit.service.AuditIndexingService;
import com.notifymesh.vendor.event.DeliveryEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventListener {

    private final AuditIndexingService indexingService;

    public DeliveryEventListener(AuditIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @KafkaListener(
            topics = {
                    "${notifymesh.kafka.topic.notification-delivered}",
                    "${notifymesh.kafka.topic.notification-failed}"
            },
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onDeliveryEvent(DeliveryEvent event) {
        indexingService.index(event);
    }
}

package com.notifymesh.router.kafka;

import com.notifymesh.router.delivery.DeliveryService;
import com.notifymesh.vendor.NotificationRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationRequestListener {

    private final DeliveryService deliveryService;

    public NotificationRequestListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
            topics = "${notifymesh.kafka.topic.notification-requested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onNotificationRequested(NotificationRequest request) {
        deliveryService.deliver(request);
    }
}

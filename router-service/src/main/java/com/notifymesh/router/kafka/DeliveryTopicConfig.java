package com.notifymesh.router.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class DeliveryTopicConfig {

    @Bean
    public NewTopic notificationDeliveredTopic(
            @Value("${notifymesh.kafka.topic.notification-delivered}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationFailedTopic(
            @Value("${notifymesh.kafka.topic.notification-failed}") String topicName) {
        return TopicBuilder.name(topicName).partitions(3).replicas(1).build();
    }
}

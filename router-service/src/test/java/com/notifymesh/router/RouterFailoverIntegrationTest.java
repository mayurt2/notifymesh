package com.notifymesh.router;

import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.event.DeliveryEvent;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real failover path against a real Kafka broker: publishes a
 * notification.requested event, lets the actual router-service context (real
 * KafkaListener, real VendorRegistry, real Resilience4j circuit breaker/retry) process it,
 * and reads back the resulting notification.delivered event. The primary SMS vendor's
 * failure rate is forced to 100% and the fallback's to 0% so the failover is deterministic
 * rather than relying on the mock vendors' random failure simulation.
 */
@SpringBootTest(classes = RouterServiceApplication.class)
@Testcontainers
class RouterFailoverIntegrationTest {

    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @BeforeAll
    static void startKafka() {
        KAFKA.start();
    }

    @AfterAll
    static void stopKafka() {
        KAFKA.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("notifymesh.vendors.sms.primary-failure-rate", () -> "1.0");
        registry.add("notifymesh.vendors.sms.fallback-failure-rate", () -> "0.0");
    }

    @Autowired
    private KafkaTemplate<String, NotificationRequest> requestPublisher;

    @Test
    void routerFailsOverToFallbackVendorAndPublishesDeliveredEvent() throws Exception {
        NotificationRequest request = new NotificationRequest(
                "it-req-failover-1", NotificationChannel.SMS, "+10000000000", null, "integration test");
        requestPublisher.send("notification.requested", request.requestId(), request).get();

        DeliveryEvent event = consumeUntilKeyMatches("notification.delivered", request.requestId(), Duration.ofSeconds(30));

        assertNotNull(event, "expected a notification.delivered event for " + request.requestId());
        assertEquals(DeliveryStatus.DELIVERED, event.status());
        assertEquals("mock-sms-vendor-b", event.vendor());
        assertTrue(event.failoverOccurred());
    }

    private DeliveryEvent consumeUntilKeyMatches(String topic, String expectedKey, Duration timeout) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "router-it-consumer-" + System.identityHashCode(this));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, DeliveryEvent> consumer = new KafkaConsumer<>(
                props, new StringDeserializer(), new JsonDeserializer<>(DeliveryEvent.class, false))) {
            consumer.subscribe(Collections.singletonList(topic));
            long deadline = System.currentTimeMillis() + timeout.toMillis();

            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, DeliveryEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, DeliveryEvent> record : records) {
                    if (expectedKey.equals(record.key())) {
                        return record.value();
                    }
                }
            }
        }
        return null;
    }
}

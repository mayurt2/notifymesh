package com.notifymesh.audit.repository;

import com.notifymesh.audit.config.ElasticsearchIndexConfig;
import com.notifymesh.audit.document.DeliveryAuditDocument;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the repository against a real Elasticsearch instance rather than mocking it, so the
 * {@code @Document}/{@code @Field} mapping (including the SpEL-driven index name and the
 * requestId-as-id overwrite semantics) is actually exercised.
 */
@DataElasticsearchTest
@Import(ElasticsearchIndexConfig.class)
@Testcontainers
class DeliveryAuditRepositoryIT {

    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.0"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    @BeforeAll
    static void startContainer() {
        ELASTICSEARCH.start();
    }

    @AfterAll
    static void stopContainer() {
        ELASTICSEARCH.stop();
    }

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + ELASTICSEARCH.getHttpHostAddress());
    }

    @Autowired
    private DeliveryAuditRepository repository;

    @Test
    void savesAndReadsBackByRequestId() {
        DeliveryAuditDocument document = new DeliveryAuditDocument(
                "req-1", NotificationChannel.SMS, "mock-sms-vendor-a", DeliveryStatus.DELIVERED,
                42, Instant.now(), false, null);

        repository.save(document);

        Optional<DeliveryAuditDocument> found = repository.findById("req-1");
        assertTrue(found.isPresent());
        assertEquals("mock-sms-vendor-a", found.get().getVendor());
        assertEquals(DeliveryStatus.DELIVERED, found.get().getStatus());
    }

    @Test
    void savingTheSameRequestIdTwiceOverwritesRatherThanDuplicating() {
        DeliveryAuditDocument first = new DeliveryAuditDocument(
                "req-2", NotificationChannel.EMAIL, "mock-email-vendor", DeliveryStatus.FAILED,
                10, Instant.now(), false, "first failure");
        DeliveryAuditDocument retried = new DeliveryAuditDocument(
                "req-2", NotificationChannel.EMAIL, "mock-email-vendor", DeliveryStatus.DELIVERED,
                15, Instant.now(), false, null);

        repository.save(first);
        repository.save(retried);

        Optional<DeliveryAuditDocument> found = repository.findById("req-2");
        assertTrue(found.isPresent());
        assertEquals(DeliveryStatus.DELIVERED, found.get().getStatus());
    }
}

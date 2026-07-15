package com.notifymesh.query.service;

import com.notifymesh.query.document.DeliveryDocument;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the query API's filters (and the vendor-success-rate aggregation) against a real
 * Elasticsearch instance seeded with known data, rather than mocking ElasticsearchOperations.
 */
@SpringBootTest
@Testcontainers
class DeliveryQueryFilterIntegrationTest {

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
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private DeliveryQueryService deliveryQueryService;

    @Autowired
    private VendorStatsService vendorStatsService;

    @Value("${notifymesh.elasticsearch.index.deliveries}")
    private String indexName;

    private static boolean seeded = false;

    @Autowired
    void seedOnce(ElasticsearchOperations ops) {
        if (seeded) {
            return;
        }
        IndexCoordinates index = IndexCoordinates.of(indexName);
        var indexOps = ops.indexOps(index);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(DeliveryDocument.class));
        }

        Instant now = Instant.parse("2026-07-15T10:00:00Z");
        ops.save(doc("req-1", NotificationChannel.SMS, "mock-sms-vendor-a", DeliveryStatus.DELIVERED,
                now.minus(3, ChronoUnit.HOURS), false, null), index);
        ops.save(doc("req-2", NotificationChannel.SMS, "mock-sms-vendor-b", DeliveryStatus.DELIVERED,
                now.minus(2, ChronoUnit.HOURS), true, null), index);
        ops.save(doc("req-3", NotificationChannel.EMAIL, "mock-email-vendor", DeliveryStatus.DELIVERED,
                now.minus(1, ChronoUnit.HOURS), false, null), index);
        ops.save(doc("req-4", NotificationChannel.WHATSAPP, "mock-whatsapp-vendor", DeliveryStatus.FAILED,
                now, false, "simulated failure"), index);

        indexOps.refresh();
        seeded = true;
    }

    private static DeliveryDocument doc(String requestId, NotificationChannel channel, String vendor,
                                         DeliveryStatus status, Instant timestamp, boolean failoverOccurred,
                                         String errorMessage) {
        DeliveryDocument document = new DeliveryDocument();
        document.setRequestId(requestId);
        document.setChannel(channel);
        document.setVendor(vendor);
        document.setStatus(status);
        document.setLatencyMs(50);
        document.setTimestamp(timestamp);
        document.setFailoverOccurred(failoverOccurred);
        document.setErrorMessage(errorMessage);
        return document;
    }

    @Test
    void filtersByChannel() {
        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(
                NotificationChannel.SMS, null, null, null, null, 0, 50);
        assertEquals(2, hits.getTotalHits());
    }

    @Test
    void filtersByStatus() {
        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(
                null, DeliveryStatus.FAILED, null, null, null, 0, 50);
        assertEquals(1, hits.getTotalHits());
        assertEquals("req-4", hits.getSearchHits().get(0).getContent().getRequestId());
    }

    @Test
    void filtersByVendor() {
        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(
                null, null, "mock-sms-vendor-b", null, null, 0, 50);
        assertEquals(1, hits.getTotalHits());
        assertEquals("req-2", hits.getSearchHits().get(0).getContent().getRequestId());
    }

    @Test
    void filtersByTimeRange() {
        // req-2 is at now - 2h (08:00Z); bracket it tightly so req-1 (07:00Z) and req-3 (09:00Z) fall outside.
        Instant from = Instant.parse("2026-07-15T07:30:00Z");
        Instant to = Instant.parse("2026-07-15T08:30:00Z");

        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(null, null, null, from, to, 0, 50);

        assertEquals(1, hits.getTotalHits());
        assertEquals("req-2", hits.getSearchHits().get(0).getContent().getRequestId());
    }

    @Test
    void combinesMultipleFilters() {
        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(
                NotificationChannel.SMS, DeliveryStatus.DELIVERED, "mock-sms-vendor-a", null, null, 0, 50);
        assertEquals(1, hits.getTotalHits());
        assertEquals("req-1", hits.getSearchHits().get(0).getContent().getRequestId());
    }

    @Test
    void returnsNoResultsWhenFiltersDontMatchAnything() {
        SearchHits<DeliveryDocument> hits = deliveryQueryService.search(
                NotificationChannel.EMAIL, DeliveryStatus.FAILED, null, null, null, 0, 50);
        assertEquals(0, hits.getTotalHits());
    }

    @Test
    void vendorSuccessRateReflectsSeededData() {
        List<VendorSuccessRateResponsePair> stats = vendorStatsService.vendorSuccessRates().stream()
                .map(r -> new VendorSuccessRateResponsePair(r.vendor(), r.total(), r.delivered(), r.failed()))
                .toList();

        assertTrue(stats.stream().anyMatch(s ->
                s.vendor().equals("mock-whatsapp-vendor") && s.total() == 1 && s.delivered() == 0 && s.failed() == 1));
        assertTrue(stats.stream().anyMatch(s ->
                s.vendor().equals("mock-sms-vendor-a") && s.total() == 1 && s.delivered() == 1 && s.failed() == 0));
    }

    private record VendorSuccessRateResponsePair(String vendor, long total, long delivered, long failed) {
    }
}

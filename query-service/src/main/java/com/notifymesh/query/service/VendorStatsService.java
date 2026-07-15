package com.notifymesh.query.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.notifymesh.query.api.VendorSuccessRateResponse;
import com.notifymesh.query.document.DeliveryDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes a per-vendor delivered/failed breakdown via a terms aggregation on {@code vendor}
 * with a status sub-aggregation, rather than fetching and grouping documents in application
 * code — this is the "query over the audit index" muscle the query-service is meant to show.
 */
@Service
public class VendorStatsService {

    private static final String VENDOR_AGG = "by_vendor";
    private static final String STATUS_AGG = "by_status";

    private final ElasticsearchOperations elasticsearchOperations;
    private final IndexCoordinates index;

    public VendorStatsService(ElasticsearchOperations elasticsearchOperations,
                               @Value("${notifymesh.elasticsearch.index.deliveries}") String indexName) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.index = IndexCoordinates.of(indexName);
    }

    public List<VendorSuccessRateResponse> vendorSuccessRates() {
        NativeQuery query = NativeQuery.builder()
                .withMaxResults(0)
                .withAggregation(VENDOR_AGG, Aggregation.of(a -> a
                        .terms(t -> t.field("vendor").size(100))
                        .aggregations(STATUS_AGG, sub -> sub.terms(t -> t.field("status").size(10)))))
                .build();

        SearchHits<DeliveryDocument> hits = elasticsearchOperations.search(query, DeliveryDocument.class, index);
        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();
        if (aggregations == null) {
            return List.of();
        }

        ElasticsearchAggregation byVendor = aggregations.get(VENDOR_AGG);
        List<StringTermsBucket> vendorBuckets = byVendor.aggregation().getAggregate().sterms().buckets().array();

        List<VendorSuccessRateResponse> results = new ArrayList<>();
        for (StringTermsBucket vendorBucket : vendorBuckets) {
            long total = vendorBucket.docCount();
            long delivered = 0;
            long failed = 0;

            Aggregate statusAggregate = vendorBucket.aggregations().get(STATUS_AGG);
            if (statusAggregate != null) {
                for (StringTermsBucket statusBucket : statusAggregate.sterms().buckets().array()) {
                    String statusKey = statusBucket.key().stringValue();
                    if ("DELIVERED".equals(statusKey)) {
                        delivered = statusBucket.docCount();
                    } else if ("FAILED".equals(statusKey)) {
                        failed = statusBucket.docCount();
                    }
                }
            }

            double successRate = total == 0 ? 0.0 : (double) delivered / total;
            results.add(new VendorSuccessRateResponse(vendorBucket.key().stringValue(), total, delivered, failed, successRate));
        }

        return results;
    }
}

package com.notifymesh.query.service;

import com.notifymesh.query.document.DeliveryDocument;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeliveryQueryService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final IndexCoordinates index;

    public DeliveryQueryService(ElasticsearchOperations elasticsearchOperations,
                                 @Value("${notifymesh.elasticsearch.index.deliveries}") String indexName) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.index = IndexCoordinates.of(indexName);
    }

    public SearchHits<DeliveryDocument> search(NotificationChannel channel, DeliveryStatus status, String vendor,
                                                Instant from, Instant to, int page, int size) {
        Criteria criteria = null;
        criteria = and(criteria, channel != null ? Criteria.where("channel").is(channel.name()) : null);
        criteria = and(criteria, status != null ? Criteria.where("status").is(status.name()) : null);
        criteria = and(criteria, vendor != null ? Criteria.where("vendor").is(vendor) : null);
        criteria = and(criteria, from != null ? Criteria.where("timestamp").greaterThanEqual(from) : null);
        criteria = and(criteria, to != null ? Criteria.where("timestamp").lessThanEqual(to) : null);

        CriteriaQuery query = new CriteriaQuery(criteria != null ? criteria : new Criteria());
        query.addSort(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.setPageable(PageRequest.of(page, size));

        return elasticsearchOperations.search(query, DeliveryDocument.class, index);
    }

    private static Criteria and(Criteria existing, Criteria next) {
        if (next == null) {
            return existing;
        }
        return existing == null ? next : existing.and(next);
    }
}

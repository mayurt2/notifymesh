package com.notifymesh.audit.repository;

import com.notifymesh.audit.document.DeliveryAuditDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DeliveryAuditRepository extends ElasticsearchRepository<DeliveryAuditDocument, String> {
}

package com.notifymesh.audit.service;

import com.notifymesh.audit.document.DeliveryAuditDocument;
import com.notifymesh.audit.repository.DeliveryAuditRepository;
import com.notifymesh.vendor.event.DeliveryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditIndexingService {

    private static final Logger log = LoggerFactory.getLogger(AuditIndexingService.class);

    private final DeliveryAuditRepository repository;

    public AuditIndexingService(DeliveryAuditRepository repository) {
        this.repository = repository;
    }

    public void index(DeliveryEvent event) {
        DeliveryAuditDocument document = DeliveryAuditDocument.from(event);
        repository.save(document);
        log.info("indexed requestId={} channel={} vendor={} status={} failoverOccurred={}",
                document.getRequestId(), document.getChannel(), document.getVendor(),
                document.getStatus(), document.isFailoverOccurred());
    }
}

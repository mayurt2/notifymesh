package com.notifymesh.query.api;

import com.notifymesh.query.document.DeliveryDocument;
import com.notifymesh.query.service.DeliveryQueryService;
import com.notifymesh.query.service.VendorStatsService;
import com.notifymesh.vendor.NotificationChannel;
import com.notifymesh.vendor.event.DeliveryStatus;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryQueryService queryService;
    private final VendorStatsService statsService;

    public DeliveryController(DeliveryQueryService queryService, VendorStatsService statsService) {
        this.queryService = queryService;
        this.statsService = statsService;
    }

    @GetMapping
    public DeliveryQueryResponse search(
            @RequestParam(required = false) NotificationChannel channel,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        SearchHits<DeliveryDocument> hits = queryService.search(channel, status, vendor, from, to, page, size);
        List<DeliveryRecordResponse> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(DeliveryRecordResponse::from)
                .toList();

        return new DeliveryQueryResponse(hits.getTotalHits(), results);
    }

    @GetMapping("/stats/vendor-success-rate")
    public List<VendorSuccessRateResponse> vendorSuccessRate() {
        return statsService.vendorSuccessRates();
    }
}

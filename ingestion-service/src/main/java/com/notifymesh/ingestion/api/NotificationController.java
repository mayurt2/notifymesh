package com.notifymesh.ingestion.api;

import com.notifymesh.ingestion.idempotency.IdempotencyService;
import com.notifymesh.ingestion.kafka.NotificationEventPublisher;
import com.notifymesh.vendor.NotificationRequest;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final IdempotencyService idempotencyService;
    private final NotificationEventPublisher publisher;
    private final MeterRegistry meterRegistry;

    public NotificationController(IdempotencyService idempotencyService, NotificationEventPublisher publisher,
                                   MeterRegistry meterRegistry) {
        this.idempotencyService = idempotencyService;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> submit(@Valid @RequestBody NotificationRequestDto dto) {
        String requestId = (dto.requestId() == null || dto.requestId().isBlank())
                ? UUID.randomUUID().toString()
                : dto.requestId();

        if (!idempotencyService.tryClaim(requestId)) {
            countIngested(dto.channel().name(), "duplicate");
            return ResponseEntity.ok(NotificationResponse.duplicate(requestId));
        }

        NotificationRequest event = new NotificationRequest(
                requestId, dto.channel(), dto.recipient(), dto.subject(), dto.body());
        publisher.publish(event);
        countIngested(dto.channel().name(), "accepted");

        return ResponseEntity.accepted().body(NotificationResponse.accepted(requestId));
    }

    private void countIngested(String channel, String outcome) {
        meterRegistry.counter("notifymesh.notifications.ingested", "channel", channel, "outcome", outcome)
                .increment();
    }
}

package com.notifymesh.ingestion.api;

import com.notifymesh.ingestion.idempotency.IdempotencyService;
import com.notifymesh.ingestion.kafka.NotificationEventPublisher;
import com.notifymesh.vendor.NotificationRequest;
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

    public NotificationController(IdempotencyService idempotencyService, NotificationEventPublisher publisher) {
        this.idempotencyService = idempotencyService;
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> submit(@Valid @RequestBody NotificationRequestDto dto) {
        String requestId = (dto.requestId() == null || dto.requestId().isBlank())
                ? UUID.randomUUID().toString()
                : dto.requestId();

        if (!idempotencyService.tryClaim(requestId)) {
            return ResponseEntity.ok(NotificationResponse.duplicate(requestId));
        }

        NotificationRequest event = new NotificationRequest(
                requestId, dto.channel(), dto.recipient(), dto.subject(), dto.body());
        publisher.publish(event);

        return ResponseEntity.accepted().body(NotificationResponse.accepted(requestId));
    }
}

package com.notifymesh.ingestion.api;

import com.notifymesh.vendor.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Wire format for {@code POST /api/v1/notifications}.
 *
 * @param requestId caller-supplied idempotency key; if omitted, one is generated and every
 *                  retry with the same key returns the original outcome instead of re-sending.
 */
public record NotificationRequestDto(
        String requestId,
        @NotNull(message = "channel is required") NotificationChannel channel,
        @NotBlank(message = "recipient is required") String recipient,
        String subject,
        @NotBlank(message = "body is required") String body
) {
}

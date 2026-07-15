package com.notifymesh.vendor;

/**
 * A single notification to be delivered through one channel.
 *
 * @param requestId  caller-supplied or generated idempotency key
 * @param channel    delivery channel (SMS/EMAIL/WHATSAPP)
 * @param recipient  phone number, email address, or WhatsApp ID depending on channel
 * @param subject    optional, used by EMAIL only
 * @param body       message content
 */
public record NotificationRequest(
        String requestId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String body
) {
}

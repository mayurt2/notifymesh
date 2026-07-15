package com.notifymesh.ingestion.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Prevents the same notification request from being published to Kafka twice, e.g. when a
 * client retries a POST after a timeout without knowing whether the first attempt landed.
 * <p>
 * Backed by Redis {@code SETNX} so the claim check is atomic across ingestion-service instances.
 */
@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:notification:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyService(StringRedisTemplate redisTemplate,
                               @Value("${notifymesh.idempotency.ttl-seconds}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /**
     * @return true if this requestId has not been seen before within the TTL window (caller
     *         should proceed with publishing), false if it's a duplicate.
     */
    public boolean tryClaim(String requestId) {
        Boolean firstClaim = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + requestId, Instant.now().toString(), ttl);
        return Boolean.TRUE.equals(firstClaim);
    }
}

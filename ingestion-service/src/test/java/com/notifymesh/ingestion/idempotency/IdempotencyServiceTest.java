package com.notifymesh.ingestion.idempotency;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class IdempotencyServiceTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static IdempotencyService idempotencyService;

    @BeforeAll
    static void startRedisAndConnect() {
        REDIS.start();

        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        idempotencyService = new IdempotencyService(redisTemplate, 86400L);
    }

    @AfterAll
    static void stopRedis() {
        connectionFactory.destroy();
        REDIS.stop();
    }

    @Test
    void firstClaimForARequestIdSucceeds() {
        assertTrue(idempotencyService.tryClaim("req-first"));
    }

    @Test
    void secondClaimForTheSameRequestIdFails() {
        assertTrue(idempotencyService.tryClaim("req-repeat"));
        assertFalse(idempotencyService.tryClaim("req-repeat"));
    }

    @Test
    void differentRequestIdsClaimIndependently() {
        assertTrue(idempotencyService.tryClaim("req-a"));
        assertTrue(idempotencyService.tryClaim("req-b"));
    }
}

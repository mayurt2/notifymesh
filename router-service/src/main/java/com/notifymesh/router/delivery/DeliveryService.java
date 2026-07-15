package com.notifymesh.router.delivery;

import com.notifymesh.router.kafka.DeliveryEventPublisher;
import com.notifymesh.vendor.DeliveryResult;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorRegistry;
import com.notifymesh.vendor.event.DeliveryEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * Attempts delivery through the channel's failover chain in priority order. Each vendor call is
 * wrapped in a per-vendor Resilience4j retry (for transient single-call flakiness) and circuit
 * breaker (for sustained vendor outages, tracked independently per vendor by name); when a
 * vendor's breaker is open or its retries are exhausted, the router moves on to the next vendor
 * in the chain instead of failing the request outright.
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final VendorRegistry vendorRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final DeliveryEventPublisher eventPublisher;

    public DeliveryService(VendorRegistry vendorRegistry,
                            CircuitBreakerRegistry circuitBreakerRegistry,
                            RetryRegistry retryRegistry,
                            DeliveryEventPublisher eventPublisher) {
        this.vendorRegistry = vendorRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.eventPublisher = eventPublisher;
    }

    public void deliver(NotificationRequest request) {
        List<VendorAdapter> chain = vendorRegistry.getFailoverChain(request.channel());
        long chainStart = System.currentTimeMillis();
        String lastError = null;

        for (int attemptIndex = 0; attemptIndex < chain.size(); attemptIndex++) {
            VendorAdapter vendor = chain.get(attemptIndex);
            boolean failoverOccurred = attemptIndex > 0;
            long attemptStart = System.currentTimeMillis();

            try {
                DeliveryResult result = callWithResilience(vendor, request);
                long elapsed = System.currentTimeMillis() - attemptStart;
                log.info("delivered requestId={} channel={} vendor={} attempt={} failoverOccurred={} latencyMs={}",
                        request.requestId(), request.channel(), result.vendorName(), attemptIndex + 1,
                        failoverOccurred, elapsed);
                eventPublisher.publish(DeliveryEvent.delivered(
                        request.requestId(), request.channel(), result.vendorName(), elapsed, failoverOccurred));
                return;
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("vendor attempt failed requestId={} channel={} vendor={} attempt={} reason={}",
                        request.requestId(), request.channel(), vendor.getVendorName(), attemptIndex + 1, lastError);
            }
        }

        long totalElapsed = System.currentTimeMillis() - chainStart;
        String lastVendorTried = chain.get(chain.size() - 1).getVendorName();
        log.error("delivery failed after exhausting failover chain requestId={} channel={} vendorsTried={} latencyMs={}",
                request.requestId(), request.channel(), chain.size(), totalElapsed);
        eventPublisher.publish(DeliveryEvent.failed(
                request.requestId(), request.channel(), lastVendorTried, totalElapsed, chain.size() > 1, lastError));
    }

    private DeliveryResult callWithResilience(VendorAdapter vendor, NotificationRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(vendor.getVendorName());
        Retry retry = retryRegistry.retry(vendor.getVendorName());

        Supplier<DeliveryResult> decorated = Decorators.ofSupplier(() -> vendor.send(request))
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .decorate();

        return decorated.get();
    }
}

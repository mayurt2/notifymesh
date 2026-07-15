package com.notifymesh.router.delivery;

import com.notifymesh.vendor.DeliveryResult;
import com.notifymesh.vendor.NotificationRequest;
import com.notifymesh.vendor.VendorAdapter;
import com.notifymesh.vendor.VendorException;
import com.notifymesh.vendor.VendorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Picks a vendor for a request and attempts delivery.
 * <p>
 * For now this only tries the top-priority vendor in the channel's failover chain and logs the
 * outcome. Milestone 4 wraps this call in a Resilience4j circuit breaker + retry and walks the
 * rest of the chain on failure, emitting notification.delivered / notification.failed events.
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final VendorRegistry vendorRegistry;

    public DeliveryService(VendorRegistry vendorRegistry) {
        this.vendorRegistry = vendorRegistry;
    }

    public void deliver(NotificationRequest request) {
        List<VendorAdapter> failoverChain = vendorRegistry.getFailoverChain(request.channel());
        VendorAdapter vendor = failoverChain.get(0);

        try {
            DeliveryResult result = vendor.send(request);
            log.info("delivered requestId={} channel={} vendor={} latencyMs={}",
                    request.requestId(), request.channel(), result.vendorName(), result.latencyMs());
        } catch (VendorException e) {
            log.warn("delivery failed requestId={} channel={} vendor={} reason={}",
                    request.requestId(), request.channel(), vendor.getVendorName(), e.getMessage());
        }
    }
}

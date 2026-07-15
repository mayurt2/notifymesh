package com.notifymesh.vendor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a priority-ordered list of {@link VendorAdapter}s per channel.
 * <p>
 * The router asks this registry for the ordered failover chain for a channel rather than
 * hardcoding vendor selection — new vendors are added by registering another adapter here,
 * not by editing routing logic.
 */
public class VendorRegistry {

    private final Map<NotificationChannel, List<VendorAdapter>> vendorsByChannel = new ConcurrentHashMap<>();

    public void register(NotificationChannel channel, List<VendorAdapter> orderedVendors) {
        vendorsByChannel.put(channel, List.copyOf(orderedVendors));
    }

    /**
     * Returns vendors for the given channel in priority order (first = try first).
     */
    public List<VendorAdapter> getFailoverChain(NotificationChannel channel) {
        List<VendorAdapter> vendors = vendorsByChannel.get(channel);
        if (vendors == null || vendors.isEmpty()) {
            throw new IllegalStateException("No vendors registered for channel " + channel);
        }
        return vendors;
    }

    public boolean hasChannel(NotificationChannel channel) {
        return vendorsByChannel.containsKey(channel) && !vendorsByChannel.get(channel).isEmpty();
    }
}

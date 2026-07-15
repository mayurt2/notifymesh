package com.notifymesh.query.api;

import java.util.List;

public record DeliveryQueryResponse(long total, List<DeliveryRecordResponse> results) {
}

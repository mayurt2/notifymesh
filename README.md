# NotifyMesh

Multi-vendor notification delivery platform with automatic vendor failover, a Kafka-based
event pipeline, and an Elasticsearch-backed delivery audit trail.

> Work in progress — built milestone by milestone. This README will be filled out (pitch,
> architecture diagram, quick start, API reference, design decisions) once the core pipeline
> is in place.

## Status

- [x] Milestone 1 — Skeleton: multi-module Maven project, health-check endpoints on all
      services, `docker-compose up` brings up Kafka (KRaft), Elasticsearch, Kibana, Redis,
      and all four services.
- [x] Milestone 2 — Ingestion → Kafka with Redis idempotency: `POST /api/v1/notifications`
      validates the request, publishes a `notification.requested` event keyed by requestId,
      and dedupes retries via a Redis `SETNX` claim (TTL-bound) so a repeated requestId
      returns `DUPLICATE` without re-publishing.
- [x] Milestone 3 — Router + vendor adapters: `router-service` consumes `notification.requested`,
      picks the top-priority vendor for the channel from a `VendorRegistry` (SMS: primary →
      fallback, Email/WhatsApp: single vendor), and attempts delivery through the mock
      adapters. No failover across vendors yet — that's Milestone 4.
- [x] Milestone 4 — Failover logic: each vendor call is wrapped in a per-vendor Resilience4j
      circuit breaker + retry; when a vendor's breaker trips, the router re-routes to the next
      vendor in the failover chain instead of failing the request, and publishes
      `notification.delivered` / `notification.failed` with a `failoverOccurred` flag.
- [x] Milestone 5 — Audit pipeline to Elasticsearch: `audit-service` consumes
      `notification.delivered`/`notification.failed` and indexes each into the
      `notifymesh-deliveries` index, keyed by `requestId` (so redelivery overwrites rather than
      duplicates), with `channel`/`status`/`vendor` mapped as `keyword` and `timestamp` as `date`.
- [x] Milestone 6 — Query API: `GET /api/v1/deliveries?channel=&status=&vendor=&from=&to=`
      (dynamic Criteria query, paginated) and `GET /api/v1/deliveries/stats/vendor-success-rate`
      (a real Elasticsearch terms aggregation with a status sub-aggregation, not an in-app
      groupBy). `from`/`to` take ISO-8601 instants (e.g. `2026-07-15T10:00:00Z`).
- [x] Milestone 7 — Testcontainers integration tests: `RouterFailoverIntegrationTest` runs the
      real router-service Spring context against a real Testcontainers Kafka broker and asserts
      an actual cross-vendor failover; `DeliveryQueryFilterIntegrationTest` seeds real
      Elasticsearch data and exercises every `/deliveries` filter plus the vendor-success-rate
      aggregation. 28 tests pass across all five modules.
- [ ] Milestone 8 — Observability (Kibana, Micrometer)
- [ ] Milestone 9 — CI (GitHub Actions)
- [ ] Milestone 10 — Docs + polish

## Quick start

```bash
docker compose up -d --build
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health

# submit a notification (retry with the same requestId is a no-op — see idempotency below)
curl -X POST http://localhost:8081/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{"requestId":"demo-req-1","channel":"SMS","recipient":"+919999999999","body":"hello from notifymesh"}'
```

## Modules

| Module | Port | Purpose |
|---|---|---|
| `ingestion-service` | 8081 | REST API, publishes to Kafka |
| `router-service` | 8082 | Kafka consumer, vendor selection + failover |
| `vendor-adapters` | — | Shared library: adapter interface + mock implementations |
| `audit-service` | 8083 | Kafka consumer → Elasticsearch |
| `query-service` | 8084 | REST API over Elasticsearch |

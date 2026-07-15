# Architecture

## Event flow

```
Client
  │  POST /api/v1/notifications
  ▼
┌────────────────────┐
│  ingestion-service  │  validates request, claims requestId in Redis (SETNX, TTL),
│                      │  publishes NotificationRequest
└──────────┬───────────┘
           │ Kafka: notification.requested
           ▼
┌────────────────────┐        ┌──────────────────────────────────┐
│   router-service     │──────▶│ vendor-adapters (shared library)  │
│                      │       │  VendorRegistry: channel → ordered│
│  for each vendor in  │       │  failover chain of VendorAdapter  │
│  the failover chain: │       │  Mock SMS/Email/WhatsApp vendors  │
│   Retry(CircuitBreaker(vendor.send))                             │
│   on failure → next vendor in chain                              │
└──────────┬───────────┘       └──────────────────────────────────┘
           │ Kafka: notification.delivered / notification.failed
           ▼
┌────────────────────┐
│   audit-service      │  indexes DeliveryEvent into Elasticsearch,
│                      │  keyed by requestId (redelivery overwrites, not duplicates)
└──────────┬───────────┘
           │
           ▼
   notifymesh-deliveries (Elasticsearch index)
           ▲
           │ dynamic Criteria query + terms aggregation
┌────────────────────┐
│   query-service      │  GET /api/v1/deliveries (filters)
│                      │  GET /api/v1/deliveries/stats/vendor-success-rate
└────────────────────┘
```

## Component responsibilities

| Service | Responsibility | Depends on |
|---|---|---|
| `ingestion-service` | Validate, dedupe, publish | Kafka, Redis |
| `router-service` | Vendor selection, failover, resilience | Kafka, `vendor-adapters` |
| `vendor-adapters` | Adapter interface + registry + mock implementations | — (plain library) |
| `audit-service` | Index delivery outcomes | Kafka, Elasticsearch |
| `query-service` | Search + aggregate delivery history | Elasticsearch |

## Why one shared library, not one shared "domain" module

`vendor-adapters` is intentionally the *only* thing shared across service boundaries — it holds
`NotificationRequest`, `VendorAdapter`/`VendorRegistry`, and the `DeliveryEvent` wire format that
both `router-service` (producer) and `audit-service` (consumer) agree on. Everything else is
private to its own service: `audit-service` and `query-service` each define their *own*
Elasticsearch document class for the same index, rather than sharing one entity across the
service boundary. That's deliberate — it's the same reasoning that keeps microservices
independently deployable: a schema change on the read side (`query-service`) shouldn't require
redeploying the write side (`audit-service`), and vice versa.

## Idempotency and ordering

- **Idempotency**: `ingestion-service` claims `requestId` in Redis via `SETNX` before publishing.
  A retried request with the same `requestId` gets `DUPLICATE` back without a second Kafka
  publish. `audit-service` additionally uses `requestId` as the Elasticsearch document ID, so
  even a redelivered Kafka message (e.g. after a consumer rebalance) overwrites the same
  document instead of creating a duplicate audit record.
- **Ordering**: every topic is keyed by `requestId`, so all events for one notification land on
  the same partition and are processed in order relative to each other.

## Failover policy

Each channel has a priority-ordered list of vendors in `VendorRegistry` (e.g. SMS: primary →
fallback). The router tries vendors in order; each individual call is wrapped in a
per-vendor Resilience4j `Retry` (handles a single transient blip) inside a per-vendor
`CircuitBreaker` (handles a sustained outage). See the README's "Design decisions" section for
why this is a circuit breaker + retry combination rather than retry alone.

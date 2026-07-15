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
- [ ] Milestone 2 — Ingestion → Kafka with Redis idempotency
- [ ] Milestone 3 — Router + vendor adapters
- [ ] Milestone 4 — Failover logic (Resilience4j)
- [ ] Milestone 5 — Audit pipeline to Elasticsearch
- [ ] Milestone 6 — Query API
- [ ] Milestone 7 — Testcontainers integration tests
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
```

## Modules

| Module | Port | Purpose |
|---|---|---|
| `ingestion-service` | 8081 | REST API, publishes to Kafka |
| `router-service` | 8082 | Kafka consumer, vendor selection + failover |
| `vendor-adapters` | — | Shared library: adapter interface + mock implementations |
| `audit-service` | 8083 | Kafka consumer → Elasticsearch |
| `query-service` | 8084 | REST API over Elasticsearch |

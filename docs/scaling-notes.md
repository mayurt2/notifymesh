# Scaling notes: autoscaling router-service on Kafka consumer lag

`router-service` is the component that actually needs to scale with traffic — it does the
resilience-wrapped vendor calls, which are the slowest and most failure-prone step in the
pipeline (mock vendors here simulate 20-300ms of latency per call; real vendor APIs are often
slower and less predictable). `ingestion-service` is comparatively cheap (validate + publish),
and `audit-service`/`query-service` scale independently of send volume. So the scaling question
that actually matters is: **how many router-service instances do we need, and when do we add
or remove one?**

## Why consumer lag, not CPU

Autoscaling `router-service` on CPU utilization is the wrong signal here, for the same reason it
was wrong for the EC2 auto-scaling groups behind our old SMS notification service: a router
instance blocked waiting on a slow vendor API is *not* CPU-bound. It can sit at low CPU while
still falling behind, because the bottleneck is I/O latency and per-vendor circuit breaker
wait states, not compute. The signal that actually reflects "are we keeping up with demand" is
**consumer lag** — the difference between the latest offset on `notification.requested` and the
router consumer group's committed offset. Lag growing means requests are piling up faster than
the fleet can process them, regardless of what CPU graphs show.

This is the same shift in thinking that drove re-architecting the EC2 auto-scaling policy from a
CPU-percentage trigger to a custom CloudWatch metric derived from queue depth: the fleet needed
to scale on "is work piling up," not "are the boxes busy."

## How this maps onto Kubernetes

If `router-service` were deployed on Kubernetes rather than `docker-compose`, the natural
mechanism is the **Horizontal Pod Autoscaler (HPA) driven by an external metric**, via
[KEDA](https://keda.sh)'s Kafka scaler:

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: router-service-scaler
spec:
  scaleTargetRef:
    name: router-service
  minReplicaCount: 2
  maxReplicaCount: 20
  triggers:
    - type: kafka
      metadata:
        bootstrapServers: kafka:9092
        consumerGroup: router-service
        topic: notification.requested
        lagThreshold: "50"        # target: no more than ~50 unconsumed messages per partition
        activationLagThreshold: "10"
```

KEDA polls the consumer group's lag per partition and feeds it to the HPA as an external metric;
the HPA adds/removes pods to keep lag near the threshold, the same closed loop CloudWatch alarms
+ a custom metric provided for the EC2 ASG, just with Kafka lag as the signal instead of queue
depth.

Two things that matter for this to actually work:

1. **Partition count is the real ceiling.** `notification.requested` is created with 3
   partitions (see `KafkaTopicConfig` in `ingestion-service`); a consumer group can't usefully
   run more instances than partitions; extra replicas would sit idle. Scaling the topic's
   partition count and scaling the router fleet's max replicas have to move together.
2. **Per-vendor circuit breakers are shared state per JVM, not per partition.** Adding a replica
   doesn't just add throughput — it also means that replica starts with a fresh, closed circuit
   breaker for every vendor. That's fine (each replica independently learns a vendor is down
   within a few calls), but it does mean the "vendor is unhealthy" signal isn't instantly
   shared across the fleet. At real scale, this would push toward a shared circuit breaker
   state store (e.g. Redis) if converging on "avoid the dead vendor" needed to be
   fleet-instant rather than eventually-consistent across replicas.

## What this project does *not* implement

There's no HPA/KEDA manifest in this repo — the mock vendors respond in milliseconds and this
runs as a handful of `docker-compose` containers, so there's no real load to autoscale against.
This document is the design; wiring it up for real would start with exporting consumer lag as a
Prometheus metric (`kafka_consumergroup_lag`, via the Kafka Exporter or Burrow) and pointing
KEDA's Prometheus scaler at it instead of talking to Kafka directly, which decouples the
autoscaler from needing broker credentials.

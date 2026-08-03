# Monitoring Guide

**Purpose.** To explain how Integrity Pro is observed and alerted on: which tools own metrics,
logs, and traces; what is monitored; how to read the dashboards; and how to add new alerts.

> Architecture context: `architecture/monitoring.md`. This is the *how-to*.

## 1. The stack at a glance

| Pillar | Tool | Data source | Where it runs |
|---|---|---|---|
| Metrics | **Prometheus** + **Grafana** | `/actuator/prometheus` per service, kube-state-metrics, node-exporter, Kafka exporter | in-cluster (`monitoring` Terraform module) |
| Logs | **CloudWatch Logs** | fluent-bit/CloudWatch agent shipping pod stdout | AWS (per env) |
| Traces | **OpenTelemetry** → **Jaeger** (dev) / **ADOT → X-Ray** (prod) | OTel SDK in services, Kafka headers | in-cluster / AWS |
| Alerts | **Alertmanager** (Prometheus) + **CloudWatch Alarms** | Prometheus rules + CloudWatch metric filters | in-cluster / AWS |

## 2. Metrics

### What every service exports

Every Spring Boot service exposes Prometheus metrics at `/actuator/prometheus`
(`micrometer-registry-prometheus`). Key series:

| Metric | Meaning | Alert-worthy |
|---|---|---|
| `http_server_requests_seconds_count/…_max/…_sum` | HTTP requests, latency | 5xx rate, p95 |
| `jvm_memory_used_bytes` | Heap/non-heap | Heap > 85% for 10m |
| `hikaricp_connections_active/max` | Connection pool | active >= 90% max |
| `kafka_consumer_*` | Consumer lag/rates | lag spikes |
| `process_cpu_usage`, `system_cpu_usage` | CPU | sustained high |
| `integrity_*` (custom) | Business: telemetry received, policy evaluations, violations | throughput collapse |

### Scrape configuration

Prometheus discovers pods by annotation:

```yaml
# in the Helm chart deployment template
prometheus.io/scrape: "true"
prometheus.io/port: "8080"
prometheus.io/path: "/actuator/prometheus"
```

Check targets:

```bash
kubectl -n monitoring port-forward svc/prometheus-server 9090:9090 &
open http://localhost:9090/targets
# every service should be UP
```

### Useful PromQL

```text
# Error rate over 5 minutes
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m]))

# p95 latency
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# Kafka consumer lag (per group)
kafka_consumergroup_lag

# HPA signal (average CPU per pod)
sum(rate(container_cpu_usage_seconds_total{namespace="integrity"}[5m])) by (pod)
```

## 3. Logs

### Where logs go

Pods write JSON to stdout → the CloudWatch agent/fluent-bit ships them to
`/aws/eks/<cluster>/integrity` log group.

### Querying with CloudWatch Logs Insights

```bash
# Find all 5xx errors from the gateway in the last 15 minutes
fields @timestamp, @message
| filter @logStream like "api-gateway"
| filter @message like /"status":5\d\d/
| sort @timestamp desc
| limit 100

# Errors across all services
fields @timestamp, @logStream, @message
| filter @message like /(ERROR|Exception)/
| sort @timestamp desc
| limit 200
```

### Local equivalent

`scripts/run-services.sh logs` tails `build/logs/*.log`; each file is one service.

## 4. Traces

Distributed traces follow a request across services and Kafka:

```bash
# dev: Jaeger UI
kubectl -n monitoring port-forward svc/jaeger-query 16686:16686 &
open http://localhost:16686
```

- Service: pick `api-gateway` → Find Traces.
- Every span carries `trace_id`; the same trace shows gateway → identity → DB → (optional) Kafka
  produce/consume spans.

**Why traces matter here:** telemetry path performance (client → gateway → telemetry-service →
Kafka → policy-engine) is the platform's most latency-sensitive chain. A trace tells you where
the milliseconds go.

## 5. Dashboards

| Dashboard | What to look at |
|---|---|
| Platform overview | 19 services up/down, request rate, 5xx, p95 |
| Per-service | JVM heap, Hikari pool, HTTP metrics, health |
| Kafka | Broker health, topic throughput, consumer lag, DLQ size |
| PostgreSQL | Connections, cache hit, replication lag |
| Node/cluster | Node CPU/mem, pod distribution |

Access via Grafana (provisioned by the `monitoring` module):

```bash
kubectl -n monitoring port-forward svc/grafana 3000:3000 &
open http://localhost:3000
```

Login uses the identity provider configured by the module (never the default admin/admin).

## 6. Alerts

### Alertmanager rules (Prometheus) — examples that ship with the stack

```yaml
groups:
  - name: integrity.rules
    rules:
      - alert: ServiceDown
        expr: kube_deployment_status_replicas_available < kube_deployment_spec_replicas
        for: 5m
        labels: { severity: critical }
        annotations:
          summary: "{{ $labels.deployment }} has unavailable replicas"
      - alert: HighErrorRate
        expr: (sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
               / sum(rate(http_server_requests_seconds_count[5m]))) > 0.05
        for: 10m
        labels: { severity: critical }
      - alert: KafkaConsumerLag
        expr: kafka_consumergroup_lag > 10000
        for: 10m
        labels: { severity: critical }
      - alert: CertExpiring
        expr: certmanager_certificate_expiration_timestamp_seconds - time() < 1209600
        labels: { severity: warning }
```

### CloudWatch alarms (AWS side)

The `cloudwatch` Terraform module creates alarms for things Prometheus can't see natively:

- RDS `DatabaseConnections`, `FreeStorageSpace`, `ReplicaLag`
- ElastiCache `CacheCPU`, `Evictions`
- MSK metrics per broker
- Log-group metric filters → "too many 5xx" alarms

### Adding a new alert

1. Write the PromQL and validate in the Prometheus UI (`/query`).
2. Add the rule to the rule files in the `monitoring` module (or a ConfigMap).
3. Re-apply; test with `promtool check rules rules.yaml`.
4. Fire it deliberately once (e.g. scale a service to 0 briefly in dev) to prove routing works.

## 7. On-call routing

```text
critical -> PagerDuty/email to on-call
warning  -> Slack/email channel
```

Test monthly with a synthetic alert (the "pager drill"). Every alert must point at a runbook:

| Alert | Runbook |
|---|---|
| ServiceDown / CrashLoop | `runbooks/pod-crash.md` |
| HighErrorRate | `runbooks/service-restart.md` + logs |
| DB alerts | `runbooks/database-failure.md` |
| KafkaConsumerLag | `runbooks/kafka-failure.md` |
| Redis alerts | `runbooks/redis-failure.md` |
| Node down | `runbooks/node-failure.md` |
| CertExpiring | `runbooks/certificate-renewal.md` |
| S3/Object storage | `runbooks/backup-restore.md` |

## 8. SLO example

A practical starting SLO for the platform:

| SLI | Target |
|---|---|
| Gateway availability (`200/503` ratio) | 99.9% / 30 days |
| API p95 latency | < 500 ms / 30 days |
| Telemetry ingestion success | 99.5% of messages within 1 min |
| Report generation (interview end → report) | 95% within 5 min |

Compute availability from Prometheus error-budget queries and alert when the burn rate exceeds
the budget.

## 9. Security notes

- Prometheus/Grafana are never exposed to the internet (port-forward only, or scoped ingress via
  the identity provider).
- Scrape endpoints are cluster-internal; network policies keep `/actuator/prometheus` off the
  public path.
- CloudWatch log groups and alarm topics are KMS-encrypted and access-scoped via IAM.
- Never log tokens/passwords; the logging pipeline drops known-secret fields if any appear.

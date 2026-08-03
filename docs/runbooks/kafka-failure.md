# Runbook: Kafka Failure

**Symptom.** Events stop flowing: consumer lag grows, topics missing, brokers unreachable,
`LEADER_NOT_AVAILABLE`, SASL auth failures, or messages on the DLQ.

**Severity.** P1 if the telemetry/policy/report pipeline is stalled; P2 otherwise.

**Impact.** Interviews still run, but telemetry, policy evaluation, reports, notifications, and
audit events are delayed or lost.

## Prerequisites

- `kubectl`, `aws` CLI (for MSK), Terraform outputs.
- 30 minutes.

## Diagnosis

### 1. Broker state

```bash
# dev/local (Strimzi)
kubectl -n kafka get pods
kubectl -n kafka get kafka kafka -o yaml   # status.conditions
kubectl -n kafka describe kafka kafka

# qa/uat/prod (MSK)
aws kafka describe-cluster --cluster-arn <arn> --query 'ClusterInfo.State'
# ACTIVE | CREATING | UPDATE_IN_PROGRESS | BROKER_NOT_AVAILABLE
```

### 2. Topic health

```bash
# dev/local
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
# expect the 13 topics from kafka.md

# lag / consumer state
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group <service>
```

### 3. Service-side errors

```bash
kubectl -n integrity logs deploy/policy-engine-service --tail=50 | grep -i kafka
kubectl -n integrity logs deploy/telemetry-service --tail=50 | grep -iE 'kafka|timeout'
```

## Resolution by cause

### A — Broker(s) down / MSK not ACTIVE

```bash
# Wait for MSK ACTIVE (it self-heals under AWS management):
aws kafka describe-cluster --cluster-arn <arn> --query 'ClusterInfo.State'

# dev: restart the Strimzi broker
kubectl -n kafka delete pod kafka-kafka-0     # StatefulSet recreates it
kubectl -n kafka rollout status statefulset/kafka-kafka
```

### B — Consumer lag growing (pipeline stall)

```bash
# Identify the lagging consumer
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group <lagging-service>
# LAG column growing = consumer can't keep up

# Scale the consumer (this platform's consumers are stateless)
kubectl -n integrity scale deployment/<consumer-service> --replicas=3
```

If lag never drains, check for a stuck processing loop:

```bash
kubectl -n integrity logs deploy/<consumer-service> --tail=100 | grep -iE 'error|exception'
```

### C — Topic missing / `LEADER_NOT_AVAILABLE`

```bash
# dev: topics are CRs; re-apply them
kubectl apply -f infra/k8s/kafka-strimzi.yaml
kubectl -n kafka get kafkatopics

# MSK: topics are declared in Terraform; re-assert:
cd terraform/environments/<env>
terraform apply -target=module.kafka -auto-approve
```

### D — SASL/SCRAM auth failure (prod)

```bash
# Check the SCRAM secret value is current
aws secretsmanager get-secret-value --secret-id integrity/<env>/kafka-scram
# If it was rotated, consumers must re-read it:
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

### E — Messages landing on the DLQ (`telemetry-received-dlq`)

```bash
# dev: inspect the poison message
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic telemetry-received-dlq --from-beginning --max-messages 1
```

DLQ presence = producer emitted an unparseable/invalid message. Fix the producer (serialization
bug) or the client version, then either discard the DLQ (dev) or replay sanitized messages.

## Verification

```bash
# Lag returns to ~0
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group <consumer>

# A test event flows end-to-end: produce on telemetry-received, watch policy-engine consume
# (see deployment/12-deploy-kafka.md Step "Verification steps" for the smoke test)
```

## Rollback

- A `terraform -target=module.kafka` apply that broke topics: re-apply the previous Terraform
  state, or restore topic CRs (dev).
- Consumer scaling: `kubectl scale deployment/<consumer> --replicas=<original>`.

## Prevention

- Alerts: `KafkaConsumerLag > 10000` (10 min), broker health, DLQ size > 0.
- Never disable `auto.create.topics.enable` toggling mid-flight; topics are code.
- Test SCRAM rotation in qa before prod (`secret-rotation.md`).

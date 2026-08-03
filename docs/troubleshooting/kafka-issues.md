# Troubleshooting: Kafka Issues

**Symptom.** Events not flowing, consumer lag, `LEADER_NOT_AVAILABLE`, SASL errors, or messages
on the DLQ. See `runbooks/kafka-failure.md` for the response version; this guide is the deeper
diagnosis reference.

## 1. `LEADER_NOT_AVAILABLE` at producer/consumer start

**Cause:** the topic has no elected leader — broker still joining, or the topic was just created.

**Diagnose (dev):**

```bash
kubectl -n kafka get pods
kubectl -n kafka get kafka kafka -o jsonpath='{.status.conditions}'
```

**Fix:** wait for the broker `Ready`; if the topic CR exists but leader never appears, delete and
re-apply the `KafkaTopic` (dev only):

```bash
kubectl apply -f infra/k8s/kafka-strimzi.yaml
kubectl -n kafka rollout restart statefulset/kafka-kafka
```

## 2. `TimeoutException` connecting to MSK

**Cause:** broker unreachable (SG), wrong bootstrap endpoints, or AZ outage.

**Diagnose:**

```bash
cd terraform/environments/<env>
terraform output kafka_bootstrap_servers
# verify SG: MSK SG must allow the app SG on the listener ports (9092/9094/9096)
```

**Fix:** correct the bootstrap endpoints in the profile config; fix the SG; rollout.

## 3. `SASL authentication failed` (prod)

**Cause:** SCRAM credentials wrong or rotated while consumers held old ones.

**Diagnose:**

```bash
aws secretsmanager get-secret-value --secret-id integrity/<env>/kafka-scram
kubectl -n integrity logs deploy/<consumer> --tail=30 | grep -i sasl
```

**Fix:** re-sync the secret and restart consumers (`runbooks/secret-rotation.md`):
`kubectl -n integrity rollout restart deployment --all`.

## 4. Consumer lag growing / stuck at N

**Diagnose (dev):**

```bash
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group <service>
# LAG column increasing = consumer can't keep up or is stuck
```

**Fix:**

- Scale the consumer: `kubectl -n integrity scale deployment/<consumer> --replicas=3`.
- Check for a poison message causing a processing exception loop:
  ```bash
  kubectl -n integrity logs deploy/<consumer> --tail=100 | grep -iE 'error|exception'
  ```
- If a message blocks the whole group, the DLQ path is the escape valve — ensure
  `telemetry-received-dlq` is wired (it is) and the consumer moves poison messages there.

## 5. `UnknownTopicOrPartitionException`

**Cause:** auto-create is disabled and the topic doesn't exist.

**Diagnose:**

```bash
# dev
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | grep <topic>
# prod
# topics created by Terraform module.kafka
```

**Fix:** apply the topic (`infra/k8s/kafka-strimzi.yaml` for dev; `terraform -target=module.kafka`
for MSK). Never enable `auto.create.topics.enable`.

## 6. Duplicates or out-of-order events

**Cause:** consumer group offset commit behavior; the platform uses `acks=all` +
`enable.idempotence` and commits offsets after processing, so duplicates appear only on
reprocessing after a failure (at-least-once semantics).

**Fix:** consumers must be idempotent (the services are designed for this). Do not switch to
exactly-once unless you accept the performance cost and the transactional config churn.

## 7. Messages landing on `telemetry-received-dlq`

**Cause:** a producer sent an unserializable/invalid message (version skew between the client and
`libs/event-contracts`).

**Diagnose:**

```bash
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic telemetry-received-dlq --from-beginning --max-messages 1
# read the raw message; compare with the expected event-contracts version
```

**Fix:** fix the producer (usually the Rust client version or a stale event schema), redeploy,
and replay or discard the DLQ deliberately (dev) / after sanitization (prod).

## 8. Broker storage pressure

**Diagnose (dev):**

```bash
kubectl -n kafka exec kafka-kafka-0 -- df -h /var/lib/kafka
```

**Fix:** adjust retention (`log.retention.hours` in the Kafka CR / MSK config) and alert on
broker disk. MSK: monitor `kafka.server:type=KafkaRequestHandlerPool` / disk via CloudWatch.

## Prevention

- Alarms: consumer lag, broker disk, DLQ non-empty.
- Topic-as-code everywhere; never rely on auto-create.
- Rehearse SCRAM rotation in qa.

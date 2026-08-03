# 12 — Deploy Kafka (topics and authentication)

**Purpose.** To make Kafka live and reachable for the platform: the broker(s) from step 08/11
plus the topic topology (13 topics, auto-creation disabled) and the credentials the services use.

## Prerequisites

- **dev/local**: step 11 completed (Strimzi operator running).
- **qa/uat/prod**: step 08 completed (MSK provisioned; Terraform outputs contain
  `kafka_bootstrap_servers` and the SCRAM secret ARN).
- `kubectl` pointed at the target cluster.

## Estimated Time

20 minutes.

## Required AWS permissions

- Strimzi: cluster write via kubectl.
- MSK: the deploy role needs `kafka-cluster:*`-style actions or the bootstrap to be pre-created
  by Terraform (it is — the `kafka` Terraform module creates the cluster, brokers, and the SCRAM
  secret; topics are declared in code and applied through the pipeline).

## Which path applies to you

| Environment | Kafka | How topics are declared |
|---|---|---|
| `local` / `dev` | Strimzi (step 11) | `KafkaTopic` CRs in `infra/k8s/kafka-strimzi.yaml` |
| `qa` / `uat` / `prod` | Amazon MSK (step 08) | Terraform (`terraform/modules/kafka`) |
| All | — | `auto.create.topics.enable: false` is enforced |

## Step A — dev/local: apply the Kafka cluster and topics

```bash
# Apply the Kafka cluster custom resource and all 13 KafkaTopic CRs
kubectl apply -f infra/k8s/kafka-strimzi.yaml
```

**What this does:** the Strimzi operator reads the `Kafka` CR and provisions the broker pod(s),
then reads each `KafkaTopic` CR and creates the topic (partition/replication settings from the
manifest). Because `auto.create.topics.enable: false`, the topic CRs are the **only** way topics
appear — this is why the deploy pipeline must apply them before any producer starts.

### Wait for the broker

```bash
kubectl -n kafka rollout status statefulset/kafka-kafka
kubectl -n kafka get pods
```

The pod `kafka-kafka-0` must reach `1/1 Running`.

### Verify topics

```bash
kubectl -n kafka exec kafka-kafka-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Expected output (13 topics):

```text
candidate-registered
identity-email
identity-user-registered
identity-user-updated
interview-completed
interview-created
interview-scheduled
interview-started
organization-registered
policy-violation
report-generated
telemetry-received
telemetry-received-dlq
```

## Step B — qa/uat/prod: MSK (topics + SCRAM)

MSK was created by Terraform. What remains is:

### 1. Confirm the bootstrap servers

```bash
cd terraform/environments/<env>
terraform output kafka_bootstrap_servers
# b-1.integrity-<env>....amazonaws.com:9096,b-2...:9096,...
```

### 2. Confirm the SCRAM secret exists

```bash
aws secretsmanager describe-secret --secret-id integrity/<env>/kafka-scram
```

The secret holds the SCRAM username/password pair that the services use
(`spring.kafka.properties.sasl.*` — see `configuration-reference.md`).

### 3. Topics on MSK

Topics for MSK environments are created by Terraform as part of the `kafka` module (declared in
code, applied in step 08). To re-assert them after a partial failure:

```bash
cd terraform/environments/<env>
terraform apply -target=module.kafka -auto-approve
```

### 4. Restart services to pick up the new SCRAM secret (if rotated)

```bash
kubectl -n integrity rollout restart deployment --all
kubectl -n integrity rollout status deployment --all
```

**Why restart?** Services read the SCRAM credentials at startup; a rotated secret only takes
effect after the pods re-read it (the deploy pipeline handles this automatically on rotation).

## Expected output

- dev/local: the 13 topics listed above.
- MSK: `kafka_bootstrap_servers` prints `b-*:9096` endpoints; `aws kafka describe-cluster`
  shows `State: ACTIVE`.

## Verification steps (all environments)

1. **Topics exist**: the `--list` output above (Strimzi) or Terraform `plan` shows no drift
   (MSK).
2. **Services can connect**: check one consumer's log:

   ```bash
   kubectl -n integrity logs deploy/telemetry-service --tail=50 | grep -i kafka
   ```

   Look for `Connected to cluster`/`[Consumer clientId=...]` — or absence of
   `LEADER_NOT_AVAILABLE`/`TimeoutException` errors.
3. **End-to-end smoke**: produce and consume a test message:

   ```bash
   # dev/local
   kubectl -n kafka exec kafka-kafka-0 -- \
     bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
     --topic telemetry-received <<< '{"test":true}'
   ```

   Then confirm it appears with a console consumer (Ctrl-C to exit).

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `LEADER_NOT_AVAILABLE` | Topic exists but no leader yet | Wait; check `kubectl -n kafka get pods` for broker readiness |
| `TimeoutException` connecting to MSK | Security group blocks `9096` | Verify the MSK SG allows the app SG on `9096` (see `networking.md`) |
| `SASL authentication failed` | SCRAM secret wrong/rotated | Check the secret; restart consumers (`rollout restart`) |
| `Topic ... does not exist` from a producer | Auto-create disabled + topic not applied | Apply `kafka-strimzi.yaml` (dev) or re-apply the `kafka` module (MSK) |
| Strimzi CR stuck `NotReady` | Storage/version issue | `kubectl -n kafka describe kafka kafka` for operator events |

## Rollback procedure

- **dev/local**: delete the CRs to remove broker + topics:
  ```bash
  kubectl delete -f infra/k8s/kafka-strimzi.yaml
  ```
  (This also removes topic data — only do it for a throwaway dev cluster.)
- **MSK**: never `terraform destroy` MSK for rollback; instead point services at the previous
  bootstrap endpoints via config and restart. Topic deletion is `kubectl`-less; use
  `aws kafka`/MSK CLI carefully and only in dev.

## Best practices

- Treat topics as code (they are, in `infra/k8s/kafka-strimzi.yaml` and the `kafka` module).
- Keep `auto.create.topics.enable: false` — it prevents accidental topic sprawl from typos.
- Set consumer group names equal to the service name for readable lag metrics
  (the services already do this).

## Security notes

- MSK is SASL/SCRAM over TLS on port `9096` in prod — the SCRAM credentials live in Secrets
  Manager and are injected at deploy time; never log them.
- Strimzi's plaintext listener is dev-only. Do not create internet-facing listeners.
- The DLQ topic (`telemetry-received-dlq`) is monitored: messages landing there mean a serialization
  bug or a bad client — see `troubleshooting/README.md` → "Kafka issues".

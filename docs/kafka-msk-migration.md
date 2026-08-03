# Kafka → Amazon MSK Migration

The platform's Kafka access is a pure configuration switch. The same application
artifacts talk to in-cluster Strimzi Kafka (dev) or Amazon MSK (qa/uat/prod)
with **zero code changes**; only environment variables and one Secret differ.

## How It Works

| Runtime | Bootstrap servers | Security | Spring profile |
| --- | --- | --- | --- |
| Docker Compose | `kafka:9092` | none (PLAINTEXT) | `docker` |
| Kubernetes dev | `kafka-kafka-bootstrap.kafka.svc.cluster.local:9092` | none | `kubernetes` |
| EKS dev | `kafka-kafka-bootstrap.kafka.svc.cluster.local:9092` | none | `dev` |
| EKS qa/uat/prod | `<MSK bootstrap brokers>` | SASL/SCRAM (TLS) | `qa` / `uat` / `prod` |

The application reads `KAFKA_BOOTSTRAP_SERVERS` from the environment (defaults in
`infra/config/application-*.yml`). The MSK bootstrap brokers are injected by the
deployment pipeline from the `kafka_bootstrap_servers` Terraform output.

## MSK Authentication Choice

MSK offers three client auth modes:

- **IAM access control** — native AWS credentials, no Java dependency changes
  for the client, but requires the AWS MSK IAM Java library.
- **SASL/SCRAM** — uses `org.apache.kafka.common.security.scram.ScramLoginModule`
  shipped with `kafka-clients` (already on every service's classpath). No
  additional dependency.
- **Mutual TLS** — requires client certificates and custom truststores.

This platform uses **SASL/SCRAM** (`SCRAM-SHA-512`), the only mode that requires
no Java source or dependency changes.

## Terraform Wiring

`terraform/modules/kafka` (enabled for qa/uat/prod, disabled for dev):

- MSK cluster with `client.subnets`, `security_groups` (EKS node SG),
  `encryption_info` (TLS + KMS at-rest).
- `aws_msk_scram_secret_association` linking the SCRAM credentials Secret.
- Enhanced monitoring + CloudWatch log groups + Prometheus JMX/node exporters.
- Outputs: `bootstrap_brokers_sasl_scram` (comma-separated broker list),
  `scram_secret_arn`, `cluster_arn`.

## Runtime Injection (pipeline)

The deploy job passes to Helm:

```
--set global.dataPlane.kafka.bootstrapServers=<output kafka_bootstrap_servers>
--set global.dataPlane.kafka.saslEnabled=true
--set global.dataPlane.kafka.saslUsername=integrity-msk
```

and puts the SCRAM password into the `integrity-secrets` Secret
(`MSK_PASSWORD`) from the `MSK_PASSWORD` GitHub secret. The chart injects
`KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_SASL_ENABLED`, `KAFKA_SASL_USERNAME` as env;
`MSK_PASSWORD` comes through `envFrom`.

## Topics

Topic names are declared once in `libs/event/.../KafkaTopics.java`:

```
identity.user-registered.v1  identity.user-updated.v1
organization.registered.v1   interview.created.v1
interview.scheduled.v1       interview.started.v1
interview.completed.v1       telemetry.received.v1
telemetry.received.dlq.v1    policy.violation.v1
report.generated.v1          identity.email.v1
candidate.registered.v1
```

- Dev (Strimzi): topics are pre-provisioned as `KafkaTopic` CRs
  (`infra/k8s/kafka-strimzi.yaml`), `auto.create.topics.enable=false`.
- MSK (qa/uat/prod): `auto.create.topics.enable=true` is the default, so topics
  are created on first use; no CR or admin script is needed. If you prefer
  strict governance, disable auto-creation in the cluster config and create
  topics via a one-off job using the MSK bootstrap brokers.

## Validation Checklist

1. Terraform applied for the environment (`kafka_enabled=true`), outputs
   `kafka_bootstrap_servers` populated.
2. SCRAM Secret associated with the cluster; `MSK_PASSWORD` in GitHub secrets.
3. Helm values for the environment set `saslEnabled=true` + correct bootstrap.
4. A test producer/consumer can connect: `kafka-console-producer.sh` against
   the SCRAM listener with the `sasl.jaas.config` from the Secret.
5. Pods log successful metadata fetch (no `AuthenticationException`).
6. CloudWatch alarm `MSKBrokerCPU` is green; topics appear in the MSK console.

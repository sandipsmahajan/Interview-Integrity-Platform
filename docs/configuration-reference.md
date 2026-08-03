# Configuration Reference

All platform configuration is externalised. No application source change is
required to move between local, Docker Compose, Kubernetes and EKS — the same
jar reads its settings from Spring profiles plus environment variables.

## How Configuration Is Loaded

Each service ships a minimal `src/main/resources/application.yml` with safe
defaults that reference environment variables:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:identity_db}
    username: ${DB_USERNAME:integrity}
    password: ${DB_PASSWORD:integrity}
```

Environment variables override the defaults. Runtimes activate a Spring profile
(`SPRING_PROFILES_ACTIVE`) and point Spring at the externalised files
(`SPRING_CONFIG_LOCATION`):

| Runtime | `SPRING_PROFILES_ACTIVE` | `SPRING_CONFIG_LOCATION` |
| --- | --- | --- |
| Local | `local` | `infra/config/` (repo root) |
| Docker Compose | `docker` | `/etc/integrity/config/` (bind mount) |
| Kubernetes dev | `kubernetes` | `/etc/integrity/config/` (ConfigMap) |
| EKS dev/qa/uat/prod | `dev` / `qa` / `uat` / `prod` | `/etc/integrity/config/` (ConfigMap) |

The profile files live in `infra/config/`:
`application-{local,docker,kubernetes,dev,qa,uat,prod}.yml`. The deployment
pipeline turns that directory into the `integrity-config` ConfigMap.

## Environment Variables

`infra/config/.env.example` documents every key. The authoritative defaults are
in each service's `application.yml`. Key groups:

### Database
| Variable | Default | Description |
| --- | --- | --- |
| `DB_HOST` | `localhost` | postgres, StatefulSet DNS, or RDS endpoint |
| `DB_PORT` | `5432` | — |
| `DB_NAME` | per service (`*_db`) | set per deployment; 16 databases total |
| `DB_USERNAME` | `integrity` | — |
| `DB_PASSWORD` | `integrity` | from `integrity-secrets` in prod |
| `DB_POOL_INITIAL_SIZE` / `DB_POOL_MAX_SIZE` | `4` / `16` | connection pool sizing |

### Redis (api-gateway rate limiting)
| Variable | Default |
| --- | --- |
| `REDIS_HOST` | `localhost` / `redis` / `redis.integrity.svc.cluster.local` / ElastiCache endpoint |
| `REDIS_PORT` | `6379` |

### Kafka
| Variable | Default | Notes |
| --- | --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | `kafka:9092` (compose), `kafka-kafka-bootstrap.kafka.svc.cluster.local:9092` (Strimzi), MSK bootstrap (qa/uat/prod) |
| `KAFKA_PRODUCER_ACKS` | `all` | — |
| `KAFKA_CONSUMER_GROUP_ID` | `default` | — |
| `KAFKA_SASL_ENABLED` | `false` | `true` for MSK (SASL/SCRAM) |
| `KAFKA_SASL_USERNAME` | — | MSK SCRAM user |

### Eureka
| Variable | Default |
| --- | --- |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka` |
| `EUREKA_REGISTER_WITH_EUREKA` / `EUREKA_FETCH_REGISTRY` | `true` |

### Mail
| Variable | Default |
| --- | --- |
| `MAIL_HOST` | `localhost` / `mailpit` / SES SMTP endpoint |
| `MAIL_PORT` | `1025` (Mailpit) / `587` (SES) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | empty / SES SMTP creds |
| `MAIL_SMTP_AUTH` | `false` / `true` |
| `MAIL_SMTP_STARTTLS_ENABLE` | `false` / `true` |
| `MAIL_FROM` | `no-reply@integritypro.app` |

### Object storage
| Variable | Default |
| --- | --- |
| `PLATFORM_STORAGE_ENDPOINT` | `http://localhost:9000` (MinIO) / `https://s3.<region>.amazonaws.com` (S3) |
| `PLATFORM_STORAGE_REGION` | `us-east-1` |

### Security
| Variable | Default | Notes |
| --- | --- | --- |
| `JWT_SECRET` | dev default | HS256 signing secret; random per environment via Secrets Manager |
| `JWT_ISSUER` / `JWT_AUDIENCE` | `interview-integrity` / `interview-integrity-api` | — |
| `JWT_ACCESS_TOKEN_TTL` | `15m` | — |
| `JWT_PERMIT_ALL` | `/actuator/health/readiness,...` | paths exempt from auth |

### Identity behaviour
`APP_NAME`, `FRONTEND_BASE_URL`, `MFA_CHALLENGE_TTL`, `MFA_EMAIL_PURPOSE`,
`EXPOSE_RESET_TOKEN`, `RESET_REQUEST_INTERVAL`, `MAX_LOGIN_ATTEMPTS`,
`LOGIN_LOCKOUT`, `MAX_MFA_CHALLENGE_ATTEMPTS`.

### Platform
| Variable | Default |
| --- | --- |
| `APP_TIMEZONE` | `UTC` |
| `SERVER_PORT` | per service (8080–8097, 8761) |
| `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP` | `INFO` |

## Per-Service Databases

Each database-backed service uses its own PostgreSQL database on the shared
instance (created by `infra/docker/init-databases.sh` locally and by the
`postgres-init` ConfigMap in Kubernetes):

`identity_db organization_db recruiter_db candidate_db interview_db telemetry_db
policy_db report_db notification_db analytics_db audit_db storage_db
feature_flag_db scheduler_db integration_db configuration_db`

## Secrets

| Secret | Owned by | Rotated |
| --- | --- | --- |
| RDS master credentials | Secrets Manager (`integrity-<env>-rds-*`) | 90 days (Lambda) |
| Redis auth token | Secrets Manager | manual |
| MSK SCRAM credentials | Secrets Manager + AWS SecretAssociation | manual |
| JWT secret | Secrets Manager (random per env) | 90 days |
| SES SMTP credentials | GitHub environment secrets → `integrity-secrets` | manual |

On EKS the pods read non-secret values from the `integrity-config` ConfigMap
and credentials from the `integrity-secrets` Secret, both applied by the deploy
pipeline before `helm upgrade`.

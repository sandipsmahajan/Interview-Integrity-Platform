# -----------------------------------------------------------------------------
# Module: kafka (Amazon MSK)
#
# Provisioned MSK cluster with SASL/SCRAM authentication. SCRAM is native to
# Apache Kafka clients, so switching from the in-cluster Strimzi broker (dev)
# to MSK (qa/uat/prod) requires configuration only:
#
#   KAFKA_BOOTSTRAP_SERVERS=<bootstrap_brokers_sasl_scram>
#   KAFKA_SECURITY_PROTOCOL=SASL_SSL
#   KAFKA_SASL_MECHANISM=SCRAM-SHA-512
#   KAFKA_SASL_JAAS_CONFIG=...  (username/password from Secrets Manager)
#
# See docs/kafka-msk-migration.md for the full migration guide.
# -----------------------------------------------------------------------------

variable "name_prefix" {
  type        = string
  description = "Resource name prefix: <project>-<environment>."
}

variable "common_tags" {
  type        = map(string)
  description = "Common platform tags."
  default     = {}
}

variable "enabled" {
  type        = bool
  description = "Provision MSK (false = use in-cluster Strimzi, e.g. dev)."
  default     = true
}

variable "vpc_id" {
  type        = string
  description = "VPC id."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnets (3 AZs) hosting the brokers."
}

variable "allowed_security_group_ids" {
  type        = list(string)
  description = "Security groups allowed to connect (EKS node SG)."
  default     = []
}

variable "kms_key_arn" {
  type        = string
  description = "KMS key ARN for broker storage and secret encryption."
}

variable "broker_instance_type" {
  type        = string
  description = "MSK broker instance type."
  default     = "kafka.t3.small"
}

variable "broker_volume_gb" {
  type        = number
  description = "Per-broker EBS storage (GiB)."
  default     = 100
}

variable "number_of_broker_nodes" {
  type        = number
  description = "Number of broker nodes (3 recommended)."
  default     = 3
}

variable "kafka_version" {
  type        = string
  description = "Apache Kafka version supported by MSK."
  default     = "3.7.2"
}

variable "kafka_username" {
  type        = string
  description = "SCRAM username."
  default     = "integrity"
}

variable "cloudwatch_log_group_name" {
  type        = string
  description = "CloudWatch log group receiving MSK broker logs."
  default     = ""
}

locals {
  msk_enabled = var.enabled

  # Deterministic secret name used by the SCRAM user.
  scram_secret_name = "${var.name_prefix}/msk/scram-${var.kafka_username}"
}

# --- Security group ---------------------------------------------------------------

resource "aws_security_group" "this" {
  count = local.msk_enabled ? 1 : 0

  name        = "${var.name_prefix}-msk"
  description = "MSK broker access for ${var.name_prefix}"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-msk" })
}

resource "aws_security_group_rule" "ingress" {
  count = local.msk_enabled ? length(var.allowed_security_group_ids) : 0

  type                     = "ingress"
  security_group_id        = aws_security_group.this[0].id
  from_port                = 9092
  to_port                  = 9098
  protocol                 = "tcp"
  source_security_group_id = var.allowed_security_group_ids[count.index]
  description              = "Allow EKS workloads (9092-9098)"
}

resource "aws_security_group_rule" "egress" {
  count = local.msk_enabled ? 1 : 0

  type              = "egress"
  security_group_id = aws_security_group.this[0].id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

# --- SCRAM credentials --------------------------------------------------------------

resource "random_password" "scram" {
  count = local.msk_enabled ? 1 : 0

  length      = 32
  special     = false
  min_upper   = 1
  min_lower   = 1
  min_numeric = 1
}

resource "aws_secretsmanager_secret" "scram" {
  count = local.msk_enabled ? 1 : 0

  name                    = local.scram_secret_name
  kms_key_id              = var.kms_key_arn
  recovery_window_in_days = 7

  tags = var.common_tags
}

resource "aws_secretsmanager_secret_version" "scram" {
  count = local.msk_enabled ? 1 : 0

  secret_id     = aws_secretsmanager_secret.scram[0].id
  secret_string = jsonencode({ username = var.kafka_username, password = random_password.scram[0].result })
}

# --- Cluster configuration ------------------------------------------------------------

resource "aws_msk_configuration" "this" {
  count = local.msk_enabled ? 1 : 0

  kafka_versions = [var.kafka_version]
  name           = "${var.name_prefix}-msk-config"
  description    = "Integrity Pro MSK configuration"

  server_properties = <<-EOT
    auto.create.topics.enable=false
    default.replication.factor=3
    min.insync.replicas=2
    num.io.threads=8
    num.network.threads=5
    log.retention.hours=168
    log.retention.bytes=1073741824
    log.segment.bytes=1073741824
    log.retention.check.interval.ms=300000
    delete.topic.enable=false
    offsets.topic.replication.factor=3
    transaction.state.log.replication.factor=3
    transaction.state.log.min.isr=2
    group.initial.rebalance.delay.ms=0
    compression.type=producer
  EOT
}

resource "aws_cloudwatch_log_group" "msk" {
  count = (local.msk_enabled && var.cloudwatch_log_group_name == "") ? 1 : 0

  name              = "/aws/msk/${var.name_prefix}"
  retention_in_days = 30
  tags              = var.common_tags
}

resource "aws_msk_cluster" "this" {
  count = local.msk_enabled ? 1 : 0

  cluster_name           = "${var.name_prefix}-msk"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.number_of_broker_nodes

  broker_node_group_info {
    instance_type = var.broker_instance_type

    storage_info {
      ebs_storage_info {
        volume_size = var.broker_volume_gb
      }
    }

    client_subnets  = var.private_subnet_ids
    security_groups = [aws_security_group.this[0].id]

    connectivity_info {
      vpc_connectivity {
        client_authentication {
          sasl {
            scram = true
          }
          tls = false
        }
      }
    }
  }

  encryption_info {
    encryption_at_rest_kms_key_arn = var.kms_key_arn
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.this[0].arn
    revision = aws_msk_configuration.this[0].latest_revision
  }

  client_authentication {
    sasl {
      scram = true
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = var.cloudwatch_log_group_name != "" ? var.cloudwatch_log_group_name : aws_cloudwatch_log_group.msk[0].name
      }
      s3 {
        enabled = false
      }
    }
  }

  open_monitoring {
    prometheus {
      jmx_exporter {
        enabled_in_broker = true
      }
      node_exporter {
        enabled_in_broker = true
      }
    }
  }

  enhanced_monitoring = "PER_TOPIC_PER_PARTITION"

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-msk" })
}

resource "aws_msk_scram_secret_association" "this" {
  count = local.msk_enabled ? 1 : 0

  cluster_arn     = aws_msk_cluster.this[0].arn
  secret_arn_list = [aws_secretsmanager_secret.scram[0].arn]
}

output "bootstrap_brokers_plaintext" {
  description = "Plaintext bootstrap brokers (not exposed publicly)."
  value       = local.msk_enabled ? aws_msk_cluster.this[0].bootstrap_brokers : []
}

output "bootstrap_brokers_tls" {
  description = "TLS bootstrap brokers (list)."
  value       = local.msk_enabled ? split(",", aws_msk_cluster.this[0].bootstrap_brokers_tls) : []
}

output "bootstrap_brokers_sasl_scram" {
  description = "SASL/SCRAM bootstrap brokers (the value applications should use)."
  value       = local.msk_enabled ? split(",", aws_msk_cluster.this[0].bootstrap_brokers_sasl_scram) : []
}

output "zookeeper_connect_string" {
  description = "ZooKeeper connection string."
  value       = local.msk_enabled ? aws_msk_cluster.this[0].zookeeper_connect_string : ""
}

output "security_group_id" {
  value = local.msk_enabled ? aws_security_group.this[0].id : ""
}

output "scram_secret_arn" {
  description = "ARN of the SCRAM credential secret."
  value       = local.msk_enabled ? aws_secretsmanager_secret.scram[0].arn : ""
}

output "cluster_arn" {
  value = local.msk_enabled ? aws_msk_cluster.this[0].arn : ""
}

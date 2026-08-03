# -----------------------------------------------------------------------------
# Module: redis
#
# Amazon ElastiCache Redis. Cluster-mode-disabled (compatible with Spring Data
# Redis as used by the gateway rate limiter and session store):
#   * automatic failover with replicas (num_cache_clusters >= 2)
#   * in-transit (TLS) and at-rest encryption, KMS encrypted
#   * auth token stored in Secrets Manager
#   * daily snapshots for backup
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

variable "vpc_id" {
  type        = string
  description = "VPC id."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnets for the cache subnet group."
}

variable "allowed_security_group_ids" {
  type        = list(string)
  description = "Security groups allowed to connect on 6379."
  default     = []
}

variable "kms_key_arn" {
  type        = string
  description = "KMS key ARN for at-rest encryption."
}

variable "node_type" {
  type        = string
  description = "Cache node type."
  default     = "cache.t4g.micro"
}

variable "num_cache_clusters" {
  type        = number
  description = "Total clusters (1 = standalone, 2+ = primary + replicas with auto-failover)."
  default     = 1
}

variable "engine_version" {
  type        = string
  description = "Redis engine version."
  default     = "7.1"
}

variable "parameter_group_family" {
  type        = string
  description = "ElastiCache parameter group family."
  default     = "redis7"
}

variable "snapshot_retention_days" {
  type        = number
  description = "Daily snapshot retention (0 disables)."
  default     = 7
}

variable "auth_token" {
  type        = string
  description = "Redis AUTH token (random when empty)."
  default     = ""
  sensitive   = true
}

variable "apply_immediately" {
  type        = bool
  description = "Apply changes immediately (dev only)."
  default     = false
}

resource "random_password" "auth_token" {
  count = var.auth_token == "" ? 1 : 0

  length  = 32
  special = false
}

locals {
  redis_auth = var.auth_token != "" ? var.auth_token : random_password.auth_token[0].result
}

resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name_prefix}-redis"
  subnet_ids = var.private_subnet_ids

  tags = var.common_tags
}

resource "aws_security_group" "this" {
  name        = "${var.name_prefix}-redis"
  description = "ElastiCache Redis access for ${var.name_prefix}"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-redis" })
}

resource "aws_security_group_rule" "ingress" {
  count = length(var.allowed_security_group_ids)

  type                     = "ingress"
  security_group_id        = aws_security_group.this.id
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = var.allowed_security_group_ids[count.index]
  description              = "Allow EKS workloads"
}

resource "aws_security_group_rule" "egress" {
  type              = "egress"
  security_group_id = aws_security_group.this.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_elasticache_parameter_group" "this" {
  name   = "${var.name_prefix}-redis-pg"
  family = var.parameter_group_family

  tags = var.common_tags
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id       = "${var.name_prefix}-redis"
  description                = "Integrity Pro Redis for ${var.name_prefix}"
  engine                     = "redis"
  engine_version             = var.engine_version
  node_type                  = var.node_type
  num_cache_clusters         = var.num_cache_clusters
  automatic_failover_enabled = var.num_cache_clusters > 1
  multi_az_enabled           = var.num_cache_clusters > 1
  parameter_group_name       = aws_elasticache_parameter_group.this.name
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.this.id]
  auth_token                 = local.redis_auth
  transit_encryption_enabled = true
  at_rest_encryption_enabled = true
  kms_key_id                 = var.kms_key_arn
  snapshot_retention_limit   = var.snapshot_retention_days
  snapshot_window            = "02:00-04:00"
  maintenance_window         = "sun:05:00-sun:06:00"
  apply_immediately          = var.apply_immediately
  auto_minor_version_upgrade = true

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-redis" })
}

resource "aws_secretsmanager_secret" "auth" {
  name                    = "${var.name_prefix}/redis/auth"
  kms_key_id              = var.kms_key_arn
  recovery_window_in_days = 7

  tags = var.common_tags
}

resource "aws_secretsmanager_secret_version" "auth" {
  secret_id = aws_secretsmanager_secret.auth.id
  secret_string = jsonencode({
    host = aws_elasticache_replication_group.this.primary_endpoint_address
    port = "6379"
    auth = local.redis_auth
  })
}

output "primary_endpoint" {
  description = "Redis primary endpoint (host:port)."
  value       = "${aws_elasticache_replication_group.this.primary_endpoint_address}:${aws_elasticache_replication_group.this.port}"
}

output "reader_endpoint" {
  description = "Redis reader endpoint (host:port) for replicas."
  value       = "${aws_elasticache_replication_group.this.reader_endpoint_address}:${aws_elasticache_replication_group.this.port}"
}

output "security_group_id" {
  value = aws_security_group.this.id
}

output "auth_secret_arn" {
  value = aws_secretsmanager_secret.auth.arn
}

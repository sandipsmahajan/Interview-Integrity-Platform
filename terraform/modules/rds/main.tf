# -----------------------------------------------------------------------------
# Module: rds
#
# Amazon RDS PostgreSQL. Encrypted, private-subnet only, with:
#   * automated backups + point-in-time recovery
#   * multi-AZ support (failover)
#   * parameter group (connection pool tuning documented in docs/database-strategy.md)
#   * master credentials stored in Secrets Manager with optional rotation
#   * deletion protection + monitoring
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
  description = "Private subnets for the DB subnet group."
}

variable "allowed_security_group_ids" {
  type        = list(string)
  description = "Security groups allowed to connect on port 5432 (EKS node SG)."
  default     = []
}

variable "kms_key_arn" {
  type        = string
  description = "KMS key ARN encrypting the storage."
}

variable "database_name" {
  type        = string
  description = "Initial database name."
  default     = "integrity"
}

variable "master_username" {
  type        = string
  description = "Master user name."
  default     = "integrity_admin"
}

variable "engine_version" {
  type        = string
  description = "PostgreSQL engine version."
  default     = "16.4"
}

variable "instance_class" {
  type        = string
  description = "DB instance class."
  default     = "db.t4g.medium"
}

variable "allocated_storage_gb" {
  type        = number
  description = "Allocated storage in GiB."
  default     = 100
}

variable "multi_az" {
  type        = bool
  description = "Multi-AZ deployment."
  default     = false
}

variable "backup_retention_days" {
  type        = number
  description = "Automated backup retention in days (PITR). 0 disables backups."
  default     = 14
}

variable "backup_window" {
  type        = string
  description = "Daily backup window (UTC)."
  default     = "02:00-03:00"
}

variable "maintenance_window" {
  type        = string
  description = "Maintenance window (UTC)."
  default     = "sun:04:00-sun:05:00"
}

variable "deletion_protection" {
  type        = bool
  description = "Protect the instance from deletion."
  default     = true
}

variable "apply_immediately" {
  type        = bool
  description = "Apply changes immediately (dev only)."
  default     = false
}

variable "enable_rotation" {
  type        = bool
  description = "Rotate master credentials via a Secrets Manager rotation Lambda."
  default     = false
}

variable "monitoring_interval" {
  type        = number
  description = "Enhanced Monitoring interval in seconds (0 disables)."
  default     = 60
}

variable "performance_insights" {
  type        = bool
  description = "Enable Performance Insights."
  default     = false
}

variable "parameter_overrides" {
  type        = map(string)
  description = "Custom PostgreSQL parameter group overrides."
  default     = {}
}

locals {
  # Connection-pool friendly defaults documented in docs/database-strategy.md.
  default_parameters = {
    shared_buffers                      = "1GB"
    max_connections                     = "200"
    statement_timeout                   = "30000"
    idle_in_transaction_session_timeout = "30000"
  }
  parameters = merge(local.default_parameters, var.parameter_overrides)
}

resource "aws_db_subnet_group" "this" {
  name        = "${var.name_prefix}-rds"
  description = "Private subnets for ${var.name_prefix} RDS"
  subnet_ids  = var.private_subnet_ids

  tags = var.common_tags
}

resource "aws_security_group" "this" {
  name        = "${var.name_prefix}-rds"
  description = "RDS PostgreSQL access for ${var.name_prefix}"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-rds" })
}

resource "aws_security_group_rule" "ingress" {
  count = length(var.allowed_security_group_ids)

  type                     = "ingress"
  security_group_id        = aws_security_group.this.id
  from_port                = 5432
  to_port                  = 5432
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

resource "aws_db_parameter_group" "this" {
  name   = "${var.name_prefix}-rds-pg"
  family = "postgres16"

  dynamic "parameter" {
    for_each = local.parameters
    content {
      name  = parameter.key
      value = parameter.value
    }
  }

  tags = var.common_tags
}

resource "random_password" "master" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}:?"
}

resource "aws_db_instance" "this" {
  identifier = "${var.name_prefix}-rds"

  engine                                = "postgres"
  engine_version                        = var.engine_version
  instance_class                        = var.instance_class
  allocated_storage                     = var.allocated_storage_gb
  storage_type                          = "gp3"
  storage_encrypted                     = true
  kms_key_id                            = var.kms_key_arn
  db_name                               = var.database_name
  username                              = var.master_username
  password                              = random_password.master.result
  db_subnet_group_name                  = aws_db_subnet_group.this.name
  vpc_security_group_ids                = [aws_security_group.this.id]
  parameter_group_name                  = aws_db_parameter_group.this.name
  multi_az                              = var.multi_az
  backup_retention_period               = var.backup_retention_days
  backup_window                         = var.backup_window
  maintenance_window                    = var.maintenance_window
  deletion_protection                   = var.deletion_protection
  apply_immediately                     = var.apply_immediately
  skip_final_snapshot                   = false
  final_snapshot_identifier             = "${var.name_prefix}-rds-final-${formatdate("YYYYMMDDHHmm", timestamp())}"
  auto_minor_version_upgrade            = true
  monitoring_interval                   = var.monitoring_interval
  monitoring_role_arn                   = var.monitoring_interval > 0 ? aws_iam_role.enhanced_monitoring[0].arn : null
  performance_insights_enabled          = var.performance_insights
  performance_insights_retention_period = var.performance_insights ? 7 : null

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-rds" })
}

# --- Enhanced monitoring ----------------------------------------------------------

resource "aws_iam_role" "enhanced_monitoring" {
  count = var.monitoring_interval > 0 ? 1 : 0

  name = "${var.name_prefix}-rds-monitoring"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "enhanced_monitoring" {
  count = var.monitoring_interval > 0 ? 1 : 0

  role       = aws_iam_role.enhanced_monitoring[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# --- Secrets Manager (master credentials) --------------------------------------------

resource "aws_secretsmanager_secret" "master" {
  name                    = "${var.name_prefix}/rds/master"
  kms_key_id              = var.kms_key_arn
  recovery_window_in_days = 7

  tags = var.common_tags
}

resource "aws_secretsmanager_secret_version" "master" {
  secret_id = aws_secretsmanager_secret.master.id
  secret_string = jsonencode({
    engine   = "postgres"
    host     = aws_db_instance.this.address
    port     = aws_db_instance.this.port
    dbname   = var.database_name
    username = var.master_username
    password = random_password.master.result
  })
}

# --- Rotation (optional) --------------------------------------------------------------

data "archive_file" "rotate" {
  count = var.enable_rotation ? 1 : 0

  type        = "zip"
  source_file = "${path.module}/rotate/rotate.py"
  output_path = "${path.module}/rotate/rotate.zip"
}

resource "aws_lambda_function" "rotate" {
  count = var.enable_rotation ? 1 : 0

  function_name = "${var.name_prefix}-rds-rotate"
  role          = aws_iam_role.rotation[0].arn
  handler       = "rotate.lambda_handler"
  runtime       = "python3.12"
  timeout       = 120
  publish       = true

  filename         = data.archive_file.rotate[0].output_path
  source_code_hash = data.archive_file.rotate[0].output_base64sha256

  environment {
    variables = {
      SECRET_ARN = aws_secretsmanager_secret.master.arn
    }
  }

  tags = var.common_tags
}

resource "aws_iam_role" "rotation" {
  count = var.enable_rotation ? 1 : 0

  name = "${var.name_prefix}-rds-rotate"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy" "rotation" {
  count = var.enable_rotation ? 1 : 0

  name = "rotate-secret"
  role = aws_iam_role.rotation[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:PutSecretValue",
          "secretsmanager:UpdateSecretVersionStage",
        ]
        Resource = aws_secretsmanager_secret.master.arn
      },
      {
        Effect = "Allow"
        Action = [
          "rds:ModifyDBInstance",
          "rds:DescribeDBInstances",
        ]
        Resource = aws_db_instance.this.arn
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_secretsmanager_secret_rotation" "this" {
  count = var.enable_rotation ? 1 : 0

  secret_id           = aws_secretsmanager_secret.master.id
  rotation_lambda_arn = aws_lambda_function.rotate[0].arn

  rotation_rules {
    automatically_after_days = 30
  }
}

# --- Outputs ----------------------------------------------------------------------------

output "endpoint" {
  description = "RDS host:port endpoint."
  value       = "${aws_db_instance.this.address}:${aws_db_instance.this.port}"
}

output "host" {
  value = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "security_group_id" {
  value = aws_security_group.this.id
}

output "master_secret_arn" {
  value = aws_secretsmanager_secret.master.arn
}

output "master_secret_name" {
  value = aws_secretsmanager_secret.master.name
}

output "database_name" {
  value = var.database_name
}

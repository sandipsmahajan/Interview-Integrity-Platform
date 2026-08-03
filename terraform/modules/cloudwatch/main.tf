# -----------------------------------------------------------------------------
# Module: cloudwatch
#
# CloudWatch Log Groups + metric filters for the platform. Application logs
# are shipped by the in-cluster fluent-bit/CloudWatch agent (EKS) or by the
# Docker logging driver; this module owns the destinations and retention.
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

variable "kms_key_arn" {
  type        = string
  description = "KMS key ARN encrypting the log groups."
  default     = ""
}

variable "log_retention_days" {
  type        = number
  description = "Retention in days for platform log groups."
  default     = 30
}

variable "services" {
  type        = list(string)
  description = "Microservices owning a log group."
  default = [
    "api-gateway",
    "identity-service",
    "organization-service",
    "recruiter-service",
    "candidate-service",
    "interview-service",
    "desktop-client-service",
    "telemetry-service",
    "policy-engine-service",
    "report-service",
    "notification-service",
    "analytics-service",
    "audit-service",
    "storage-service",
    "feature-flag-service",
    "scheduler-service",
    "integration-service",
    "configuration-service",
    "discovery-service",
  ]
}

resource "aws_cloudwatch_log_group" "service" {
  for_each = toset(var.services)

  name              = "/aws/eks/${var.name_prefix}/${each.value}"
  retention_in_days = var.log_retention_days
  kms_key_id        = var.kms_key_arn != "" ? var.kms_key_arn : null

  tags = merge(var.common_tags, { Name = "/aws/eks/${var.name_prefix}/${each.value}" })
}

# Metric filter: count ERROR-level application log lines per service.
resource "aws_cloudwatch_log_metric_filter" "errors" {
  for_each = toset(var.services)

  name           = "${each.value}-errors"
  pattern        = "ERROR"
  log_group_name = aws_cloudwatch_log_group.service[each.key].name

  metric_transformation {
    name      = "${each.value}_error_count"
    namespace = "IntegrityPro/Logs"
    value     = "1"
  }
}

output "log_group_names" {
  description = "Map of service -> log group name."
  value       = { for k, v in aws_cloudwatch_log_group.service : k => v.name }
}

output "log_group_arns" {
  description = "Map of service -> log group ARN."
  value       = { for k, v in aws_cloudwatch_log_group.service : k => v.arn }
}

output "error_metric_names" {
  description = "Map of service -> CloudWatch metric name for ERROR lines."
  value       = { for k, v in aws_cloudwatch_log_metric_filter.errors : k => v.metric_transformation[0].name }
}

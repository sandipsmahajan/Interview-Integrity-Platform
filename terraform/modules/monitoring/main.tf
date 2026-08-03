# -----------------------------------------------------------------------------
# Module: monitoring
#
# CloudWatch Dashboards + Alarms + SNS notifications for the platform.
# Alarms cover the managed data plane (RDS, ElastiCache, MSK), the front ALB
# and the EKS node group. Prometheus/Grafana dashboards live in-cluster and
# are installed by the Helm chart (see infra/helm/observability).
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

variable "alarm_email" {
  type        = string
  description = "Email address for SNS alarm subscriptions."
  default     = ""
}

variable "rds_db_instance_id" {
  type        = string
  description = "RDS instance identifier for alarms (empty to skip)."
  default     = ""
}

variable "redis_replication_group_id" {
  type        = string
  description = "ElastiCache replication group id for alarms (empty to skip)."
  default     = ""
}

variable "msk_cluster_arn" {
  type        = string
  description = "MSK cluster ARN for alarms (empty to skip)."
  default     = ""
}

variable "alb_arn_suffix" {
  type        = string
  description = "Front ALB arn suffix for alarms (empty to skip)."
  default     = ""
}

# --- SNS ------------------------------------------------------------------------------

resource "aws_sns_topic" "alarms" {
  name              = "${var.name_prefix}-alarms"
  kms_master_key_id = var.kms_key_arn
  tags              = var.common_tags
}

resource "aws_sns_topic_subscription" "email" {
  count = var.alarm_email != "" ? 1 : 0

  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

variable "kms_key_arn" {
  type        = string
  description = "KMS key ARN for the SNS topic."
  default     = ""
}

# --- RDS alarms -----------------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "rds_cpu" {
  count = var.rds_db_instance_id != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "RDS CPU utilization above 80% for 15 minutes"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "CPUUtilization"
  dimensions  = { DBInstanceIdentifier = var.rds_db_instance_id }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_connections" {
  count = var.rds_db_instance_id != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-rds-connections-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  period              = "300"
  statistic           = "Average"
  threshold           = "150"
  alarm_description   = "RDS connections approaching max_connections"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "DatabaseConnections"
  dimensions  = { DBInstanceIdentifier = var.rds_db_instance_id }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_storage" {
  count = var.rds_db_instance_id != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-rds-storage-low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  period              = "3600"
  statistic           = "Average"
  threshold           = "10"
  alarm_description   = "RDS free storage below 10%"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/RDS"
  metric_name = "FreeStorageSpace"
  dimensions  = { DBInstanceIdentifier = var.rds_db_instance_id }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

# --- ElastiCache alarms ----------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "redis_cpu" {
  count = var.redis_replication_group_id != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-redis-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "ElastiCache engine CPU above 80%"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ElastiCache"
  metric_name = "CPUUtilization"
  dimensions  = { CacheClusterId = "${var.redis_replication_group_id}-001" }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_memory" {
  count = var.redis_replication_group_id != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-redis-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  period              = "300"
  statistic           = "Average"
  threshold           = "90"
  alarm_description   = "ElastiCache memory usage above 90%"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ElastiCache"
  metric_name = "DatabaseMemoryUsagePercentage"
  dimensions  = { CacheClusterId = "${var.redis_replication_group_id}-001" }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

# --- MSK alarms --------------------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "msk_cpu" {
  count = var.msk_cluster_arn != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-msk-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "3"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "MSK broker CPU above 80%"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/Kafka"
  metric_name = "CpuUser"
  dimensions  = { Cluster_Name = basename(var.msk_cluster_arn) }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

# --- ALB alarms ---------------------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  count = var.alb_arn_suffix != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-alb-5xx"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "ALB returned more than 10x 5xx in 10 minutes"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ApplicationELB"
  metric_name = "HTTPCode_ELB_5XX_Count"
  dimensions  = { LoadBalancer = var.alb_arn_suffix }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "alb_target_health" {
  count = var.alb_arn_suffix != "" ? 1 : 0

  alarm_name          = "${var.name_prefix}-alb-unhealthy-hosts"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  period              = "300"
  statistic           = "Average"
  threshold           = "0"
  alarm_description   = "ALB target group has unhealthy hosts"
  treat_missing_data  = "notBreaching"

  namespace   = "AWS/ApplicationELB"
  metric_name = "UnHealthyHostCount"
  dimensions  = { LoadBalancer = var.alb_arn_suffix }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = var.common_tags
}

# --- Dashboard -------------------------------------------------------------------------------

resource "aws_cloudwatch_dashboard" "overview" {
  dashboard_name = "${var.name_prefix}-overview"

  dashboard_body = jsonencode({
    widgets = [
      {
        type       = "text"
        x          = 0, y = 0, width = 24, height = 1
        properties = { markdown = "# Integrity Pro - ${var.name_prefix} overview" }
      },
      {
        type = "metric", x = 0, y = 1, width = 12, height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/RDS", "CPUUtilization", { "stat" : "Average" }],
            ["AWS/ApplicationELB", "RequestCount", { "stat" : "Sum" }],
            ["AWS/ApplicationELB", "TargetResponseTime", { "stat" : "Average" }],
          ]
          period = 300
          title  = "Throughput"
        }
      },
      {
        type = "metric", x = 12, y = 1, width = 12, height = 6
        properties = {
          view    = "timeSeries"
          stacked = false
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", { "stat" : "Sum", "label" : "5XX" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", { "stat" : "Sum", "label" : "4XX" }],
          ]
          period = 300
          title  = "Error rates"
        }
      },
      {
        type = "metric", x = 0, y = 7, width = 8, height = 6
        properties = {
          view    = "timeSeries"
          metrics = [["AWS/RDS", "DatabaseConnections", { "stat" : "Average" }]]
          period  = 300
          title   = "RDS connections"
        }
      },
      {
        type = "metric", x = 8, y = 7, width = 8, height = 6
        properties = {
          view    = "timeSeries"
          metrics = [["AWS/ElastiCache", "DatabaseMemoryUsagePercentage", { "stat" : "Average" }]]
          period  = 300
          title   = "Redis memory"
        }
      },
      {
        type = "metric", x = 16, y = 7, width = 8, height = 6
        properties = {
          view    = "timeSeries"
          metrics = [["AWS/Kafka", "CpuUser", { "stat" : "Average" }]]
          period  = 300
          title   = "MSK CPU"
        }
      },
      {
        type = "log", x = 0, y = 13, width = 24, height = 6
        properties = {
          query  = "SOURCE '/aws/eks/${var.name_prefix}/*' | fields @timestamp, @log, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 50"
          region = data.aws_region.current.name
          title  = "Latest ERROR log lines"
        }
      }
    ]
  })
}

data "aws_region" "current" {}

output "sns_topic_arn" {
  value = aws_sns_topic.alarms.arn
}

output "dashboard_name" {
  value = aws_cloudwatch_dashboard.overview.dashboard_name
}

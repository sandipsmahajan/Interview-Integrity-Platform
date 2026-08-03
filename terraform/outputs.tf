# -----------------------------------------------------------------------------
# Root outputs - the values consumed by the deployment pipeline (GitHub
# Actions) to inject runtime configuration into Kubernetes Secrets/ConfigMaps.
# -----------------------------------------------------------------------------

output "environment" {
  description = "Deployed environment name."
  value       = var.environment
}

output "aws_region" {
  description = "Deployed AWS region."
  value       = var.aws_region
}

output "vpc_id" {
  description = "Platform VPC identifier."
  value       = module.vpc.vpc_id
}

output "eks_cluster_name" {
  description = "Amazon EKS cluster name (kubectl context)."
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "Amazon EKS control plane endpoint."
  value       = module.eks.cluster_endpoint
}

output "db_endpoint" {
  description = "RDS PostgreSQL endpoint (host:port)."
  value       = module.rds.endpoint
}

output "redis_endpoint" {
  description = "ElastiCache Redis primary endpoint (host:port)."
  value       = module.redis.primary_endpoint
}

output "kafka_bootstrap_servers" {
  description = "MSK bootstrap brokers (comma separated) or in-cluster Strimzi endpoint."
  value       = var.kafka_enabled ? join(",", module.kafka.bootstrap_brokers_sasl_scram) : "kafka-kafka-bootstrap.kafka.svc.cluster.local:9092"
}

output "alb_dns_name" {
  description = "Front Application Load Balancer DNS name."
  value       = module.alb.alb_dns_name
}

output "alb_zone_id" {
  description = "Front Application Load Balancer hosted zone id (alias records)."
  value       = module.alb.alb_zone_id
}

output "api_url" {
  description = "Public API base URL."
  value       = "https://${local.api_domain}"
}

output "portal_url" {
  description = "Public portal base URL."
  value       = "https://${local.portal_domain}"
}

output "ecr_repositories" {
  description = "ECR repository URLs (account.dkr.ecr.<region>.amazonaws.com/integrity/<service>)."
  value       = module.ecr.repository_urls
}

output "github_actions_role_arn" {
  description = "IAM role assumed by GitHub Actions CI/CD."
  value       = module.iam.github_actions_role_arn
}

output "log_groups" {
  description = "CloudWatch Log Group names per service."
  value       = module.cloudwatch.log_group_names
}

output "sns_alarm_topic_arn" {
  description = "SNS topic receiving CloudWatch alarm notifications."
  value       = module.monitoring.sns_topic_arn
}

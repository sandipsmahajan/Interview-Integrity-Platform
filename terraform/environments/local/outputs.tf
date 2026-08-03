output "environment" {
  value = var.environment
}

output "aws_region" {
  value = var.aws_region
}

output "eks_cluster_name" {
  value = module.eks.cluster_name
}

output "db_endpoint" {
  value = module.rds.endpoint
}

output "db_master_secret_arn" {
  value = module.rds.master_secret_arn
}

output "redis_endpoint" {
  value = module.redis.primary_endpoint
}

output "redis_auth_secret_arn" {
  value = module.redis.auth_secret_arn
}

output "kafka_bootstrap_servers" {
  value = var.kafka_enabled ? join(",", module.kafka.bootstrap_brokers_sasl_scram) : "kafka-kafka-bootstrap.kafka.svc.cluster.local:9092"
}

output "api_url" {
  value = "https://${local.api_domain}"
}

output "portal_url" {
  value = "https://${local.portal_domain}"
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "ecr_repositories" {
  value = module.ecr.repository_urls
}

output "github_actions_role_arn" {
  value = module.iam.github_actions_role_arn
}

output "log_groups" {
  value = module.cloudwatch.log_group_names
}

output "sns_alarm_topic_arn" {
  value = module.monitoring.sns_topic_arn
}

output "jwt_secret_name" {
  value = module.secrets_manager.secret_names["jwt"]
}

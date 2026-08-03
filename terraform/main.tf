# -----------------------------------------------------------------------------
# Integrity Pro - root blueprint main
#
# This is the canonical wiring of every Terraform module. The concrete
# environment roots (terraform/environments/{dev,qa,uat,prod}) follow the
# same structure with environment-specific sizes and flags.
# -----------------------------------------------------------------------------

# --- Naming and tags -------------------------------------------------------------

module "shared" {
  source      = "./modules/shared"
  project     = var.project
  environment = var.environment
  owner       = var.owner
  cost_center = var.cost_center
}

# --- KMS ----------------------------------------------------------------------------

module "kms" {
  source      = "./modules/kms"
  name_prefix = local.name_prefix
  common_tags = local.common_tags
  service_principals = {
    logs       = ["logs.${var.aws_region}.amazonaws.com"]
    cloudwatch = []
  }
  keys = {
    general    = "General purpose platform key"
    rds        = "RDS storage and credential encryption"
    redis      = "ElastiCache at-rest encryption"
    msk        = "MSK storage and credential encryption"
    s3         = "S3 object encryption"
    ecr        = "ECR image encryption"
    eks        = "EKS secrets and node volume encryption"
    secrets    = "Secrets Manager encryption"
    cloudwatch = "CloudWatch log groups and SNS"
  }
}

# --- IAM -----------------------------------------------------------------------------

module "iam" {
  source              = "./modules/iam"
  name_prefix         = local.name_prefix
  common_tags         = local.common_tags
  github_organisation = var.github_organisation
  github_repository   = var.github_repository
  eks_cluster_names   = [module.eks.cluster_name]
}

# --- Networking -----------------------------------------------------------------------

module "vpc" {
  source             = "./modules/vpc"
  name_prefix        = local.name_prefix
  common_tags        = local.common_tags
  cidr               = var.vpc_cidr
  azs                = var.aws_availability_zones
  public_subnets     = var.public_subnets
  private_subnets    = var.private_subnets
  enable_flow_logs   = var.enable_flow_logs
  single_nat_gateway = contains(["dev", "qa"], var.environment)
  flow_log_bucket    = module.s3.bucket_arns["flowlogs"]
}

module "networking" {
  source                     = "./modules/networking"
  name_prefix                = local.name_prefix
  common_tags                = local.common_tags
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  node_sg_id                 = module.eks.node_sg_id
  private_route_table_ids    = module.vpc.private_route_table_ids
  enable_interface_endpoints = !contains(["dev"], var.environment)
}

# --- Object storage + ECR --------------------------------------------------------------

module "s3" {
  source      = "./modules/s3"
  name_prefix = local.name_prefix
  common_tags = local.common_tags
  kms_key_arn = module.kms.key_arns["s3"]
  buckets     = var.object_storage_buckets
}

module "ecr" {
  source               = "./modules/ecr"
  name_prefix          = local.name_prefix
  common_tags          = local.common_tags
  kms_key_arn          = module.kms.key_arns["ecr"]
  image_tag_mutability = var.ecr_image_tag_mutability
  lifecycle_max_images = var.ecr_lifecycle_max_images
}

# --- EKS -------------------------------------------------------------------------------

module "eks" {
  source                         = "./modules/eks"
  name_prefix                    = local.name_prefix
  common_tags                    = local.common_tags
  vpc_id                         = module.vpc.vpc_id
  private_subnet_ids             = module.vpc.private_subnet_ids
  cluster_version                = var.eks_cluster_version
  cluster_encryption_kms_key_arn = module.kms.key_arns["eks"]
  node_instance_types            = var.eks_node_instance_types
  node_desired_size              = var.eks_node_desired_size
  node_min_size                  = var.eks_node_min_size
  node_max_size                  = var.eks_node_max_size
  node_disk_kms_key_arn          = module.kms.key_arns["eks"]
  ami_type                       = var.eks_ami_type
}

# --- RDS / Redis / MSK -------------------------------------------------------------------

module "rds" {
  source                     = "./modules/rds"
  name_prefix                = local.name_prefix
  common_tags                = local.common_tags
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_sg_id]
  kms_key_arn                = module.kms.key_arns["rds"]
  instance_class             = var.db_instance_class
  multi_az                   = var.db_multi_az
  allocated_storage_gb       = var.db_allocated_storage_gb
  backup_retention_days      = var.db_backup_retention_days
  deletion_protection        = var.db_deletion_protection
  enable_rotation            = var.db_enable_rotation
  monitoring_interval        = 60
  performance_insights       = contains(["uat", "prod"], var.environment)
}

module "redis" {
  source                     = "./modules/redis"
  name_prefix                = local.name_prefix
  common_tags                = local.common_tags
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_sg_id]
  kms_key_arn                = module.kms.key_arns["redis"]
  node_type                  = var.redis_node_type
  num_cache_clusters         = var.redis_num_cache_clusters
}

module "kafka" {
  source                     = "./modules/kafka"
  name_prefix                = local.name_prefix
  common_tags                = local.common_tags
  enabled                    = var.kafka_enabled
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_sg_id]
  kms_key_arn                = module.kms.key_arns["msk"]
  broker_instance_type       = var.kafka_broker_instance_type
  broker_volume_gb           = var.kafka_broker_volume_gb
  number_of_broker_nodes     = var.kafka_number_of_broker_nodes
}

# --- ACM / Route53 / ALB ------------------------------------------------------------------

module "acm" {
  source         = "./modules/acm"
  name_prefix    = local.name_prefix
  common_tags    = local.common_tags
  hosted_zone_id = module.route53.zone_id
  certificates = {
    api = {
      domain_name = local.api_domain
    }
    portal = {
      domain_name = local.portal_domain
    }
  }
}

module "alb" {
  source               = "./modules/alb"
  name_prefix          = local.name_prefix
  common_tags          = local.common_tags
  vpc_id               = module.vpc.vpc_id
  public_subnet_ids    = module.vpc.public_subnet_ids
  node_sg_id           = module.eks.node_sg_id
  node_group_asg_names = [module.eks.node_group_asg_name]
  ingress_node_port    = var.alb_ingress_node_port
  acm_certificate_arn  = module.acm.certificate_arns["api"]
  access_logs_bucket   = module.s3.bucket_ids["alb-logs"]
}

module "route53" {
  source       = "./modules/route53"
  name_prefix  = local.name_prefix
  common_tags  = local.common_tags
  dns_domain   = var.dns_domain
  create_zone  = var.create_hosted_zone
  alb_dns_name = module.alb.alb_dns_name
  alb_zone_id  = module.alb.alb_zone_id
}

# --- Secrets / parameters -----------------------------------------------------------------

module "secrets_manager" {
  source      = "./modules/secrets-manager"
  name_prefix = local.name_prefix
  common_tags = local.common_tags
  kms_key_arn = module.kms.key_arns["secrets"]
  secrets = {
    jwt = {
      value       = var.jwt_secret
      random      = true
      rotate_days = 90
      description = "JWT signing secret (HS256)"
    }
  }
}

module "parameter_store" {
  source      = "./modules/parameter-store"
  name_prefix = local.name_prefix
  common_tags = local.common_tags
  kms_key_arn = module.kms.key_arns["secrets"]
  parameters = {
    "eureka/server-url"          = { value = "http://discovery-service.integrity.svc.cluster.local:8761/eureka" }
    "platform/frontend-base-url" = { value = "https://${local.portal_domain}" }
    "platform/api-base-url"      = { value = "https://${local.api_domain}" }
    "platform/timezone"          = { value = "UTC" }
  }
}

# --- Observability -------------------------------------------------------------------------

module "cloudwatch" {
  source             = "./modules/cloudwatch"
  name_prefix        = local.name_prefix
  common_tags        = local.common_tags
  kms_key_arn        = module.kms.key_arns["cloudwatch"]
  log_retention_days = var.log_retention_days
}

module "monitoring" {
  source                     = "./modules/monitoring"
  name_prefix                = local.name_prefix
  common_tags                = local.common_tags
  kms_key_arn                = module.kms.key_arns["cloudwatch"]
  alarm_email                = var.alarm_email
  rds_db_instance_id         = "${local.name_prefix}-rds"
  redis_replication_group_id = "${local.name_prefix}-redis"
  msk_cluster_arn            = var.kafka_enabled ? module.kafka.cluster_arn : ""
  alb_arn_suffix             = module.alb.alb_arn_suffix
}

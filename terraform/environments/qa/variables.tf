# Dev environment variables. Sensitive values are injected by CI as
# TF_VAR_* environment variables, never committed.

variable "project" {
  type    = string
  default = "integrity"
}

variable "environment" {
  type    = string
  default = "qa"
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "aws_availability_zones" {
  type    = list(string)
  default = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "owner" {
  type    = string
  default = "platform"
}

variable "cost_center" {
  type    = string
  default = "platform"
}

# --- Networking ---------------------------------------------------------

variable "vpc_cidr" {
  type    = string
  default = "10.100.0.0/16"
}

variable "public_subnets" {
  type    = list(string)
  default = ["10.100.1.0/24", "10.100.2.0/24", "10.100.3.0/24"]
}

variable "private_subnets" {
  type    = list(string)
  default = ["10.100.11.0/24", "10.100.12.0/24", "10.100.13.0/24"]
}

variable "enable_flow_logs" {
  type    = bool
  default = true
}

# --- DNS / TLS ---------------------------------------------------------

variable "dns_domain" {
  type    = string
  default = "integritypro.example.com"
}

variable "api_subdomain" {
  type    = string
  default = "api.qa"
}

variable "portal_subdomain" {
  type    = string
  default = "portal.qa"
}

variable "create_hosted_zone" {
  type    = bool
  default = false
}

# --- EKS ---------------------------------------------------------------

variable "eks_cluster_version" {
  type    = string
  default = "1.31"
}

variable "eks_node_instance_types" {
  type    = list(string)
  default = ["m6i.large"]
}

variable "eks_node_desired_size" {
  type    = number
  default = 3
}

variable "eks_node_min_size" {
  type    = number
  default = 3
}

variable "eks_node_max_size" {
  type    = number
  default = 9
}

variable "eks_ami_type" {
  type    = string
  default = "AL2_x86_64"
}

# --- RDS ---------------------------------------------------------------

variable "db_instance_class" {
  type    = string
  default = "db.t4g.medium"
}

variable "db_multi_az" {
  type    = bool
  default = false
}

variable "db_allocated_storage_gb" {
  type    = number
  default = 100
}

variable "db_backup_retention_days" {
  type    = number
  default = 14
}

variable "db_deletion_protection" {
  type    = bool
  default = true
}

variable "db_enable_rotation" {
  type    = bool
  default = false
}

# --- Redis -------------------------------------------------------------

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.small"
}

variable "redis_num_cache_clusters" {
  type    = number
  default = 2
}

# --- MSK ---------------------------------------------------------------

variable "kafka_enabled" {
  type    = bool
  default = true
}

# --- ECR / S3 ----------------------------------------------------------

variable "ecr_image_tag_mutability" {
  type    = string
  default = "IMMUTABLE"
}

variable "ecr_lifecycle_max_images" {
  type    = number
  default = 20
}

variable "object_storage_buckets" {
  type = map(object({
    versioning       = optional(bool, true)
    lifecycle_enable = optional(bool, true)
    expiration_days  = optional(number, 0)
  }))
  default = {
    documents = { versioning = true, lifecycle_enable = true }
    reports   = { versioning = true, lifecycle_enable = true }
    uploads   = { versioning = true, lifecycle_enable = true }
    flowlogs  = { versioning = true, lifecycle_enable = true, expiration_days = 90 }
    alb-logs  = { versioning = true, lifecycle_enable = true, expiration_days = 30 }
  }
}

# --- ALB / WAF ----------------------------------------------------------

variable "alb_ingress_node_port" {
  type    = number
  default = 30080
}

variable "enable_waf" {
  type    = bool
  default = false
}

# --- Observability -------------------------------------------------------

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "alarm_email" {
  type    = string
  default = "ops@integritypro.example.com"
}

# --- CI/CD ----------------------------------------------------------------

variable "github_organisation" {
  type    = string
  default = "sandipsmahajan"
}

variable "github_repository" {
  type    = string
  default = "Interview-Integrity-Platform"
}

variable "create_oidc_provider" {
  type    = bool
  default = false
}

variable "jwt_secret" {
  type      = string
  default   = ""
  sensitive = true
}

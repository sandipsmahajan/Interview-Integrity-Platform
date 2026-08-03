# -----------------------------------------------------------------------------
# Root variables. Every value has a safe default so `terraform plan` works out
# of the box; concrete values for a real deployment live in terraform.tfvars
# (generated from terraform.tfvars.example) or in the environment roots.
# -----------------------------------------------------------------------------

# --- Platform identity --------------------------------------------------------

variable "project" {
  description = "Project name used as the prefix for every resource."
  type        = string
  default     = "integrity"
}

variable "environment" {
  description = "Deployment environment: local | dev | qa | uat | prod."
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["local", "dev", "qa", "uat", "prod"], var.environment)
    error_message = "environment must be one of: local, dev, qa, uat, prod."
  }
}

variable "aws_region" {
  description = "Primary AWS region."
  type        = string
  default     = "us-east-1"
}

variable "aws_availability_zones" {
  description = "Availability zones used for subnets (must exist in aws_region)."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
}

variable "owner" {
  description = "Team or individual owning the environment (cost allocation)."
  type        = string
  default     = "platform"
}

variable "cost_center" {
  description = "Cost centre tag for billing reports."
  type        = string
  default     = "platform"
}

# --- Networking ---------------------------------------------------------------

variable "vpc_cidr" {
  description = "CIDR block of the platform VPC."
  type        = string
  default     = "10.100.0.0/16"
}

variable "public_subnets" {
  description = "Public subnet CIDRs, one per AZ."
  type        = list(string)
  default     = ["10.100.1.0/24", "10.100.2.0/24", "10.100.3.0/24"]
}

variable "private_subnets" {
  description = "Private subnet CIDRs, one per AZ. All workloads run here."
  type        = list(string)
  default     = ["10.100.11.0/24", "10.100.12.0/24", "10.100.13.0/24"]
}

variable "enable_flow_logs" {
  description = "Enable VPC flow logs shipped to the central S3 log bucket."
  type        = bool
  default     = true
}

# --- DNS / TLS ----------------------------------------------------------------

variable "dns_domain" {
  description = "Root DNS zone (Route 53). api.<dns_domain> and portal.<dns_domain> are used."
  type        = string
  default     = "integritypro.example.com"
}

variable "create_hosted_zone" {
  description = "Create the Route 53 hosted zone when true, otherwise import an existing zone."
  type        = bool
  default     = false
}

variable "tls_domain" {
  description = "Domain validated by ACM (defaults to dns_domain)."
  type        = string
  default     = ""
}

# --- Amazon EKS ---------------------------------------------------------------

variable "eks_cluster_version" {
  description = "Kubernetes version for the EKS control plane and node groups."
  type        = string
  default     = "1.31"
}

variable "eks_node_instance_types" {
  description = "Instance types for the application managed node group."
  type        = list(string)
  default     = ["m6i.large"]
}

variable "eks_node_desired_size" {
  description = "Desired node count of the application managed node group."
  type        = number
  default     = 3
}

variable "eks_node_max_size" {
  description = "Maximum node count (HPA + cluster autoscaler upper bound)."
  type        = number
  default     = 9
}

variable "eks_node_min_size" {
  description = "Minimum node count."
  type        = number
  default     = 3
}

variable "eks_ami_type" {
  description = "AMI type for managed node groups (AL2_x86_64)."
  type        = string
  default     = "AL2_x86_64"
}

# --- Amazon RDS ---------------------------------------------------------------

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.medium"
}

variable "db_multi_az" {
  description = "Multi-AZ deployment for the RDS instance."
  type        = bool
  default     = false
}

variable "db_allocated_storage_gb" {
  description = "Allocated storage in GiB (gp3)."
  type        = number
  default     = 100
}

variable "db_backup_retention_days" {
  description = "Automated backup retention (0 disables; minimum 7 recommended for prod)."
  type        = number
  default     = 14
}

variable "db_deletion_protection" {
  description = "Protect the RDS instance against accidental deletion."
  type        = bool
  default     = true
}

variable "db_enable_rotation" {
  description = "Rotate the RDS master password with a Secrets Manager rotation Lambda."
  type        = bool
  default     = false
}

# --- Amazon ElastiCache -------------------------------------------------------

variable "redis_node_type" {
  description = "ElastiCache node type."
  type        = string
  default     = "cache.t4g.micro"
}

variable "redis_num_cache_clusters" {
  description = "Number of cache clusters (1 = single node, 2+ = replicas with automatic failover)."
  type        = number
  default     = 1
}

# --- Amazon MSK ---------------------------------------------------------------

variable "kafka_enabled" {
  description = "Provision Amazon MSK (false = in-cluster Strimzi Kafka for dev)."
  type        = bool
  default     = false
}

variable "kafka_broker_instance_type" {
  description = "MSK broker instance type."
  type        = string
  default     = "kafka.t3.small"
}

variable "kafka_broker_volume_gb" {
  description = "Per-broker EBS storage (GiB)."
  type        = number
  default     = 100
}

variable "kafka_number_of_broker_nodes" {
  description = "Number of MSK brokers (3 for high availability)."
  type        = number
  default     = 3
}

# --- ECR / container images ---------------------------------------------------

variable "ecr_image_tag_mutability" {
  description = "ECR tag mutability (immutable recommended)."
  type        = string
  default     = "IMMUTABLE"
}

variable "ecr_lifecycle_max_images" {
  description = "Maximum image tags retained per repository."
  type        = number
  default     = 20
}

# --- Object storage -----------------------------------------------------------

variable "object_storage_buckets" {
  description = "Map of object-storage buckets (suffix -> features)."
  type = map(object({
    versioning       = bool
    lifecycle_enable = bool
  }))
  default = {
    documents = { versioning = true, lifecycle_enable = true }
    reports   = { versioning = true, lifecycle_enable = true }
    uploads   = { versioning = true, lifecycle_enable = true }
    flowlogs  = { versioning = true, lifecycle_enable = true, expiration_days = 90 }
    alb-logs  = { versioning = true, lifecycle_enable = true, expiration_days = 30 }
  }
}

# --- Load balancer / ingress --------------------------------------------------

variable "alb_ingress_node_port" {
  description = "NodePort of the in-cluster nginx-ingress-controller service."
  type        = number
  default     = 30080
}

variable "enable_waf" {
  description = "Attach an AWS WAF web ACL (rate limiting + managed rules) to the front ALB."
  type        = bool
  default     = false
}

# --- Observability ------------------------------------------------------------

variable "log_retention_days" {
  description = "CloudWatch Log Group retention for platform logs."
  type        = number
  default     = 30
}

variable "alarm_email" {
  description = "Email address receiving CloudWatch alarm notifications (SNS)."
  type        = string
  default     = "ops@integritypro.example.com"
}

# --- CI/CD role ---------------------------------------------------------------

variable "github_organisation" {
  description = "GitHub organisation used for the OIDC CI/CD role trust policy."
  type        = string
  default     = "sandipsmahajan"
}

variable "github_repository" {
  description = "GitHub repository used for the OIDC CI/CD role trust policy."
  type        = string
  default     = "Interview-Integrity-Platform"
}

# --- Secrets ------------------------------------------------------------------

variable "jwt_secret" {
  description = "HS256 JWT signing secret (>= 32 bytes). Empty = random per environment."
  type        = string
  default     = ""
  sensitive   = true
}

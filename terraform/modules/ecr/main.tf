# -----------------------------------------------------------------------------
# Module: ecr
#
# One ECR repository per microservice. Repositories:
#   * use immutable tags (a tag maps to exactly one image digest)
#   * scan on push (ECR Basic/Enhanced scanning)
#   * are encrypted with the platform KMS key
#   * expire untagged and old tags via lifecycle policy
#
# The Helm chart and the deployment pipeline reference images as
# <account>.dkr.ecr.<region>.amazonaws.com/<name_prefix>/<service>:<tag>.
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
  description = "KMS key ARN for image encryption."
}

variable "services" {
  type        = list(string)
  description = "Microservice names (one repository per service)."
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

variable "image_tag_mutability" {
  type        = string
  description = "IMMUTABLE or MUTABLE tag mutability."
  default     = "IMMUTABLE"
}

variable "lifecycle_max_images" {
  type        = number
  description = "Maximum image tags kept per repository."
  default     = 20
}

resource "aws_ecr_repository" "this" {
  for_each = toset(var.services)

  name                 = "${var.name_prefix}/${each.value}"
  image_tag_mutability = var.image_tag_mutability
  force_delete         = false

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "KMS"
    kms_key         = var.kms_key_arn
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}/${each.value}" })
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each = toset(var.services)

  repository = aws_ecr_repository.this[each.value].name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after 7 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 7
        }
        action = { type = "expire" }
      },
      {
        rulePriority = 2
        description  = "Keep the most recent ${var.lifecycle_max_images} tagged images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = var.lifecycle_max_images
        }
        action = { type = "expire" }
      }
    ]
  })
}

output "repository_names" {
  description = "Map of service -> repository name."
  value       = { for k, v in aws_ecr_repository.this : k => v.name }
}

output "repository_urls" {
  description = "Map of service -> repository URL."
  value       = { for k, v in aws_ecr_repository.this : k => v.repository_url }
}

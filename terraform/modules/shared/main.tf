# -----------------------------------------------------------------------------
# Module: shared
#
# Central naming convention and tag set. Every module and every root consumes
# this module so resource names and tags are guaranteed consistent.
# -----------------------------------------------------------------------------

variable "project" {
  type        = string
  description = "Project name."
}

variable "environment" {
  type        = string
  description = "Environment name (local/dev/qa/uat/prod)."
}

variable "owner" {
  type        = string
  description = "Team or individual owning the resources."
  default     = "platform"
}

variable "cost_center" {
  type        = string
  description = "Cost allocation tag."
  default     = "platform"
}

locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = var.owner
    CostCenter  = var.cost_center
  }
}

output "name_prefix" {
  description = "Prefix for every resource name: <project>-<environment>."
  value       = local.name_prefix
}

output "common_tags" {
  description = "Tag set applied to every supported resource."
  value       = local.common_tags
}

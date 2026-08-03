provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  description = "Region hosting the Terraform state."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Project prefix for the state resources."
  type        = string
  default     = "integrity"
}

variable "state_bucket_name" {
  description = "Explicit state bucket name (must be globally unique)."
  type        = string
  default     = ""
}

variable "force_destroy_state" {
  description = "Allow the state bucket to be destroyed (set true only when decommissioning the account)."
  type        = bool
  default     = false
}

terraform {
  required_version = ">= 1.9.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.40, < 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "github_organisation" {
  type        = string
  description = "GitHub organisation."
}

variable "github_repository" {
  type        = string
  description = "GitHub repository."
}

# GitHub OIDC provider - account-scoped, created exactly once.
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

output "github_oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.github.arn
}

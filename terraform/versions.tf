# -----------------------------------------------------------------------------
# Integrity Pro - Terraform root configuration
#
# This directory is the reference blueprint for the whole platform. The same
# modules are consumed by the concrete environment roots under
# terraform/environments/{local,dev,qa,uat,prod}.
#
# Terraform version and provider constraints are centralised here so every
# root and every module agrees on the toolchain.
# -----------------------------------------------------------------------------
terraform {
  required_version = ">= 1.9.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.40, < 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.6, < 4.0"
    }
    null = {
      source  = "hashicorp/null"
      version = ">= 3.2, < 4.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = ">= 2.5, < 3.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = ">= 4.0, < 5.0"
    }
  }
}

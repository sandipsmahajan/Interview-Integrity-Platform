# -----------------------------------------------------------------------------
# Bootstrap: Terraform remote-state backend.
#
# One-time run that creates:
#   * the S3 bucket holding every environment's Terraform state
#   * the DynamoDB table used for state locking
#   * the KMS key encrypting state at rest
#
# Usage:
#   cd terraform/bootstrap
#   terraform init
#   terraform apply -var="state_bucket_name=<your-unique-name>"
#
# Afterwards every environment root's backend.tf references the bucket and
# the lock table.
# -----------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

locals {
  state_bucket = var.state_bucket_name != "" ? var.state_bucket_name : "${var.project}-terraform-state-${data.aws_caller_identity.current.account_id}"
  lock_table   = "${var.project}-terraform-locks"
}

resource "aws_kms_key" "state" {
  description             = "KMS key encrypting Terraform state for ${var.project}"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "EnableRootAdmin"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }
        Action    = "kms:*"
        Resource  = "*"
      }
    ]
  })
}

resource "aws_kms_alias" "state" {
  name          = "alias/${var.project}/terraform-state"
  target_key_id = aws_kms_key.state.key_id
}

resource "aws_s3_bucket" "state" {
  bucket        = local.state_bucket
  force_destroy = var.force_destroy_state
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.state.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "state" {
  bucket = aws_s3_bucket.state.id
  rule {
    id     = "expire-noncurrent-versions"
    status = "Enabled"
    filter {
      prefix = ""
    }
    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

resource "aws_dynamodb_table" "locks" {
  name         = local.lock_table
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  tags = {
    Project     = var.project
    Environment = "bootstrap"
    ManagedBy   = "terraform"
  }
}

output "state_bucket" {
  value = aws_s3_bucket.state.id
}

output "lock_table" {
  value = aws_dynamodb_table.locks.id
}

output "state_kms_key_arn" {
  value = aws_kms_key.state.arn
}

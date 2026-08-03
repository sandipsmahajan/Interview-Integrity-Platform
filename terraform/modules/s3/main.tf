# -----------------------------------------------------------------------------
# Module: s3
#
# Object storage buckets. Configuration-only switch to MinIO in development:
# the same bucket names/keys are exposed, so the application code only changes
# its endpoint + credentials (see infra/config/application-*.yml).
#
# Every bucket:
#   * blocks all public access
#   * is encrypted with the platform KMS key (SSE-KMS)
#   * supports versioning (S3 versioning, not the storage-service versioning)
#   * carries lifecycle rules (expire non-current, transition to IA/Glacier)
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
  description = "KMS key ARN used for server-side encryption."
}

variable "buckets" {
  description = "Map of bucket-suffix -> bucket features."
  type = map(object({
    versioning         = optional(bool, true)
    lifecycle_enable   = optional(bool, true)
    noncurrent_days    = optional(number, 30)
    transition_ia_days = optional(number, 90)
    transition_glacier = optional(number, 180)
    expiration_days    = optional(number, 0)
    block_public       = optional(bool, true)
  }))
  default = {}
}

resource "aws_s3_bucket" "this" {
  for_each = var.buckets

  bucket        = "${var.name_prefix}-${each.key}"
  force_destroy = false

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-${each.key}", Role = "object-storage" })
}

resource "aws_s3_bucket_versioning" "this" {
  for_each = var.buckets

  bucket = aws_s3_bucket.this[each.key].id
  versioning_configuration {
    status = each.value.versioning ? "Enabled" : "Suspended"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  for_each = var.buckets

  bucket = aws_s3_bucket.this[each.key].id
  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = var.kms_key_arn
      sse_algorithm     = "aws:kms"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  for_each = var.buckets

  bucket                  = aws_s3_bucket.this[each.key].id
  block_public_acls       = each.value.block_public
  block_public_policy     = each.value.block_public
  ignore_public_acls      = each.value.block_public
  restrict_public_buckets = each.value.block_public
}

resource "aws_s3_bucket_lifecycle_configuration" "this" {
  for_each = { for k, v in var.buckets : k => v if v.lifecycle_enable }

  bucket = aws_s3_bucket.this[each.key].id

  rule {
    id     = "intelligent-tiering"
    status = "Enabled"

    transition {
      days          = each.value.transition_ia_days
      storage_class = "STANDARD_IA"
    }
    transition {
      days          = each.value.transition_glacier
      storage_class = "GLACIER"
    }
    noncurrent_version_expiration {
      noncurrent_days = each.value.noncurrent_days
    }
  }

  dynamic "rule" {
    for_each = each.value.expiration_days > 0 ? [1] : []
    content {
      id     = "expire-objects"
      status = "Enabled"
      expiration {
        days = each.value.expiration_days
      }
    }
  }
}

variable "alb_logs_bucket_suffix" {
  type        = string
  description = "Bucket suffix reserved for ALB access logs (gets the ELB write policy)."
  default     = "alb-logs"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

resource "aws_s3_bucket_policy" "alb_logs" {
  count = lookup(var.buckets, var.alb_logs_bucket_suffix, null) != null ? 1 : 0

  bucket = aws_s3_bucket.this[var.alb_logs_bucket_suffix].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowElbWrite"
        Effect = "Allow"
        Principal = {
          Service = "elasticloadbalancing.amazonaws.com"
        }
        Action   = ["s3:PutObject"]
        Resource = "${aws_s3_bucket.this[var.alb_logs_bucket_suffix].arn}/*"
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

output "bucket_ids" {
  description = "Map of bucket-suffix -> bucket name."
  value       = { for k, v in aws_s3_bucket.this : k => v.id }
}

output "bucket_arns" {
  description = "Map of bucket-suffix -> bucket ARN."
  value       = { for k, v in aws_s3_bucket.this : k => v.arn }
}

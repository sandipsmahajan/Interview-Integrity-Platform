# -----------------------------------------------------------------------------
# Module: kms
#
# Creates one KMS customer-managed key per use-case. Every key:
#   * is rotatable (rotation enabled)
#   * grants full control to the account root (admin)
#   * grants encryption/decryption to the relevant AWS service principal
# The caller receives a map of { key-name = { arn, key_id } }.
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

variable "keys" {
  description = "Map of KMS keys to create: key-name -> description."
  type        = map(string)
  default     = {}
}

variable "service_principals" {
  description = "Additional service principals granted Encrypt/Decrypt/GenerateDataKey on each key: key-name -> list of principals."
  type        = map(list(string))
  default     = {}
}

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
}

resource "aws_kms_key" "this" {
  for_each = var.keys

  description                        = each.value
  deletion_window_in_days            = 10
  enable_key_rotation                = true
  is_enabled                         = true
  multi_region                       = false
  bypass_policy_lockout_safety_check = false

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat(
      [
        {
          Sid    = "EnableRootAdmin"
          Effect = "Allow"
          Principal = {
            AWS = "arn:aws:iam::${local.account_id}:root"
          }
          Action   = "kms:*"
          Resource = "*"
        },
      ],
      [
        for principal in try(var.service_principals[each.key], []) :
        {
          Sid    = "AllowServiceUse${replace(principal, "/[^a-zA-Z0-9]/", "")}"
          Effect = "Allow"
          Principal = {
            Service = principal
          }
          Action = [
            "kms:Decrypt",
            "kms:GenerateDataKey",
            "kms:ReEncryptFrom",
            "kms:ReEncryptTo",
          ]
          Resource = "*"
        }
      ],
      [
        {
          Sid    = "AllowCloudWatchLogs"
          Effect = "Allow"
          Principal = {
            Service = "logs.${data.aws_region.current.name}.amazonaws.com"
          }
          Action = [
            "kms:Decrypt",
            "kms:GenerateDataKey",
          ]
          Resource = "*"
        }
      ]
    )
  })
}

data "aws_region" "current" {}

resource "aws_kms_alias" "this" {
  for_each = var.keys

  name          = "alias/${var.name_prefix}/${each.key}"
  target_key_id = aws_kms_key.this[each.key].key_id
}

output "key_arns" {
  description = "Map of key-name -> KMS key ARN."
  value       = { for k, v in aws_kms_key.this : k => v.arn }
}

output "key_ids" {
  description = "Map of key-name -> KMS key id."
  value       = { for k, v in aws_kms_key.this : k => v.key_id }
}

output "aliases" {
  description = "Map of key-name -> alias ARN."
  value       = { for k, v in aws_kms_alias.this : k => v.arn }
}

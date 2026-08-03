# -----------------------------------------------------------------------------
# Module: parameter-store
#
# Non-secret runtime configuration in AWS Systems Manager Parameter Store:
# feature flags, service registry URL, domain names, logging levels, pool
# sizes. Values are injected into the Kubernetes ConfigMaps by the deployment
# pipeline (aws cli / parameter store CSI driver).
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
  description = "KMS key ARN for SecureString parameters."
  default     = ""
}

variable "parameters" {
  description = <<-EOT
    Map of parameter-suffix -> configuration.
      type : String | StringList | SecureString
      value: parameter value
  EOT
  type = map(object({
    type  = optional(string, "String")
    value = string
  }))
  default = {}
}

resource "aws_ssm_parameter" "this" {
  for_each = var.parameters

  name      = "/${var.name_prefix}/${each.key}"
  type      = each.value.type == "SecureString" ? "SecureString" : each.value.type
  value     = each.value.value
  key_id    = (each.value.type == "SecureString" && var.kms_key_arn != "") ? var.kms_key_arn : null
  tier      = "Standard"
  overwrite = false

  tags = var.common_tags
}

output "parameter_names" {
  description = "Map of parameter-suffix -> full SSM parameter name."
  value       = { for k, v in aws_ssm_parameter.this : k => v.name }
}

output "parameter_arns" {
  description = "Map of parameter-suffix -> SSM parameter ARN."
  value       = { for k, v in aws_ssm_parameter.this : k => v.arn }
}

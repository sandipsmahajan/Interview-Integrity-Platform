# -----------------------------------------------------------------------------
# Module: acm
#
# AWS Certificate Manager certificates validated through Route 53 DNS.
# When `hosted_zone_id` is provided, DNS validation records are created
# automatically; otherwise the certificate is returned as PENDING_VALIDATION
# and the operator adds the CNAME.
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

variable "certificates" {
  description = <<-EOT
    Map of certificate-key -> domain names.
      domain_name : primary domain
      subject_alternative_names: SANs
  EOT
  type = map(object({
    domain_name               = string
    subject_alternative_names = optional(list(string), [])
  }))
  default = {}
}

variable "hosted_zone_id" {
  type        = string
  description = "Route 53 hosted zone id for DNS validation."
  default     = ""
}

resource "aws_acm_certificate" "this" {
  for_each = var.certificates

  domain_name               = each.value.domain_name
  subject_alternative_names = each.value.subject_alternative_names
  validation_method         = "DNS"
  key_algorithm             = "RSA_2048"

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-${each.key}" })
}

resource "aws_route53_record" "validation" {
  for_each = {
    for k, v in aws_acm_certificate.this :
    k => v
    if var.hosted_zone_id != ""
  }

  allow_overwrite = true
  name            = each.value.domain_validation_options[0].resource_record_name
  records         = [each.value.domain_validation_options[0].resource_record_value]
  type            = each.value.domain_validation_options[0].resource_record_type
  zone_id         = var.hosted_zone_id
  ttl             = 60
}

resource "aws_acm_certificate_validation" "this" {
  for_each = var.hosted_zone_id != "" ? var.certificates : {}

  certificate_arn         = aws_acm_certificate.this[each.key].arn
  validation_record_fqdns = [aws_route53_record.validation[each.key].fqdn]
}

output "certificate_arns" {
  description = "Map of certificate-key -> certificate ARN."
  value       = { for k, v in aws_acm_certificate.this : k => v.arn }
}

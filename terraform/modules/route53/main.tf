# -----------------------------------------------------------------------------
# Module: route53
#
# DNS zone + records. When `create_zone` is true the zone is created and the
# nameservers returned (delegate from the registrar). When false, the zone is
# imported by name. Alias records point the API and portal subdomains at the
# front ALB.
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

variable "dns_domain" {
  type        = string
  description = "Root DNS domain (zone name)."
}

variable "create_zone" {
  type        = bool
  description = "Create the hosted zone (false = import existing zone by name)."
  default     = false
}

variable "alb_dns_name" {
  type        = string
  description = "Front ALB DNS name (alias targets)."
}

variable "alb_zone_id" {
  type        = string
  description = "Front ALB hosted zone id."
}

variable "api_subdomain" {
  type        = string
  description = "Subdomain for the API front door."
  default     = "api"
}

variable "portal_subdomain" {
  type        = string
  description = "Subdomain for the web portal."
  default     = "portal"
}

variable "enable_api_record" {
  type        = bool
  description = "Create the api.<domain> record."
  default     = true
}

variable "enable_portal_record" {
  type        = bool
  description = "Create the portal.<domain> record."
  default     = true
}

resource "aws_route53_zone" "this" {
  count = var.create_zone ? 1 : 0

  name = var.dns_domain

  tags = var.common_tags
}

data "aws_route53_zone" "this" {
  count = var.create_zone ? 0 : 1

  name         = var.dns_domain
  private_zone = false
}

locals {
  zone_id = var.create_zone ? aws_route53_zone.this[0].zone_id : data.aws_route53_zone.this[0].zone_id
}

resource "aws_route53_record" "api" {
  count = var.enable_api_record ? 1 : 0

  zone_id = local.zone_id
  name    = "${var.api_subdomain}.${var.dns_domain}"
  type    = "A"

  alias {
    name                   = var.alb_dns_name
    zone_id                = var.alb_zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "portal" {
  count = var.enable_portal_record ? 1 : 0

  zone_id = local.zone_id
  name    = "${var.portal_subdomain}.${var.dns_domain}"
  type    = "A"

  alias {
    name                   = var.alb_dns_name
    zone_id                = var.alb_zone_id
    evaluate_target_health = true
  }
}

output "zone_id" {
  value = local.zone_id
}

output "zone_name" {
  value = var.dns_domain
}

output "nameservers" {
  description = "Zone nameservers (only when the zone was created)."
  value       = var.create_zone ? aws_route53_zone.this[0].name_servers : []
}

locals {
  shared = module.shared

  common_tags = module.shared.common_tags
  name_prefix = module.shared.name_prefix

  api_domain    = "${var.api_subdomain}.${var.dns_domain}"
  portal_domain = "${var.portal_subdomain}.${var.dns_domain}"
}

# -----------------------------------------------------------------------------
# Locals shared by the root and consumed by the modules. The `shared` module
# centralises the naming convention and the common tag set so every root uses
# exactly the same values.
# -----------------------------------------------------------------------------
locals {
  # The shared module encodes the naming convention and the common tags.
  shared = module.shared

  common_tags = module.shared.common_tags
  name_prefix = module.shared.name_prefix

  # Full DNS names used by the front door and the portal.
  api_domain    = "api.${var.dns_domain}"
  portal_domain = "portal.${var.dns_domain}"
}

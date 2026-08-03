# -----------------------------------------------------------------------------
# Provider configuration.
#
# The AWS provider authenticates in the following order (standard AWS SDK
# chain): environment variables / shared credentials file / EC2 instance role
# / GitHub OIDC role (CI). No credentials are committed to this repository.
#
# default_tags guarantees that every supported resource carries the platform
# tags, which feed cost-allocation reports and the tag-based cleanup tooling.
# -----------------------------------------------------------------------------
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

provider "random" {}

# -----------------------------------------------------------------------------
# Module: networking
#
# VPC Endpoints so private-subnet workloads reach AWS services without going
# through the NAT gateway (reduced cost, no public egress, stronger security):
#   * Gateway endpoints:  S3, DynamoDB
#   * Interface endpoints: ECR API + DKR, Secrets Manager, SSM/EC2, Logs,
#                          CloudWatch, STS, Autoscaling
#
# The module builds its own security group (via the shared security-groups
# module) that only allows HTTPS from the EKS node security group.
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

variable "vpc_id" {
  type        = string
  description = "VPC id."
}

variable "private_subnet_ids" {
  type        = list(string)
  description = "Private subnets the interface endpoints attach to."
}

variable "node_sg_id" {
  type        = string
  description = "EKS node security group allowed to call the endpoints."
}

variable "private_route_table_ids" {
  type        = list(string)
  description = "Private route table ids for gateway-endpoint route associations."
  default     = []
}

variable "enable_interface_endpoints" {
  type        = bool
  description = "Create interface endpoints (disable for cost-sensitive environments using NAT egress)."
  default     = true
}

data "aws_region" "current" {}

locals {
  region = data.aws_region.current.name

  interface_endpoint_services = {
    "ecr.api"        = "com.amazonaws.${local.region}.ecr.api"
    "ecr.dkr"        = "com.amazonaws.${local.region}.ecr.dkr"
    "secretsmanager" = "com.amazonaws.${local.region}.secretsmanager"
    "ssm"            = "com.amazonaws.${local.region}.ssm"
    "ec2messages"    = "com.amazonaws.${local.region}.ec2messages"
    "ssmmessages"    = "com.amazonaws.${local.region}.ssmmessages"
    "logs"           = "com.amazonaws.${local.region}.logs"
    "monitoring"     = "com.amazonaws.${local.region}.monitoring"
    "sts"            = "com.amazonaws.${local.region}.sts"
    "autoscaling"    = "com.amazonaws.${local.region}.autoscaling"
  }
}

module "endpoint_sg" {
  source = "../security-groups"

  name        = "${var.name_prefix}-vpc-endpoints"
  description = "Security group for VPC interface endpoints"
  vpc_id      = var.vpc_id
  common_tags = var.common_tags

  ingress_rules = [
    {
      description   = "HTTPS from EKS workloads"
      from_port     = 443
      to_port       = 443
      protocol      = "tcp"
      source_sg_ids = [var.node_sg_id]
    }
  ]

  egress_rules = [
    {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = ["0.0.0.0/0"]
    }
  ]
}

# --- Gateway endpoints -------------------------------------------------------------

resource "aws_vpc_endpoint" "s3" {
  vpc_id            = var.vpc_id
  service_name      = "com.amazonaws.${local.region}.s3"
  vpc_endpoint_type = "Gateway"

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-vpce-s3" })
}

resource "aws_vpc_endpoint_route_table_association" "s3" {
  count = length(var.private_route_table_ids)

  vpc_endpoint_id = aws_vpc_endpoint.s3.id
  route_table_id  = var.private_route_table_ids[count.index]
}

resource "aws_vpc_endpoint" "dynamodb" {
  vpc_id            = var.vpc_id
  service_name      = "com.amazonaws.${local.region}.dynamodb"
  vpc_endpoint_type = "Gateway"

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-vpce-dynamodb" })
}

# --- Interface endpoints -------------------------------------------------------------

resource "aws_vpc_endpoint" "interface" {
  for_each = var.enable_interface_endpoints ? local.interface_endpoint_services : {}

  vpc_id              = var.vpc_id
  service_name        = each.value
  vpc_endpoint_type   = "Interface"
  subnet_ids          = var.private_subnet_ids
  private_dns_enabled = true
  security_group_ids  = [module.endpoint_sg.id]

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-vpce-${each.key}" })
}

output "endpoint_ids" {
  description = "Map of endpoint-key -> VPC endpoint id."
  value = merge(
    { s3 = aws_vpc_endpoint.s3.id, dynamodb = aws_vpc_endpoint.dynamodb.id },
    { for k, v in aws_vpc_endpoint.interface : k => v.id }
  )
}

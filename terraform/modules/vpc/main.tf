# -----------------------------------------------------------------------------
# Module: vpc
#
# Full platform VPC:
#   * 3 AZs, public + private subnets
#   * Internet Gateway (public egress)
#   * NAT Gateways (private egress) - one per AZ or a single shared NAT
#   * Route tables and Network ACLs
#   * VPC flow logs shipped to the central log bucket
#
# The public subnets only carry the load balancer and NAT; every workload runs
# in private subnets.
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

variable "cidr" {
  type        = string
  description = "VPC CIDR block."
}

variable "azs" {
  type        = list(string)
  description = "Availability zones for the subnets."
}

variable "public_subnets" {
  type        = list(string)
  description = "Public subnet CIDRs (one per AZ)."
}

variable "private_subnets" {
  type        = list(string)
  description = "Private subnet CIDRs (one per AZ)."
}

variable "enable_nat_gateway" {
  type        = bool
  description = "Create NAT gateways for private-subnet egress."
  default     = true
}

variable "single_nat_gateway" {
  type        = bool
  description = "Use a single shared NAT gateway (cost optimisation, dev/qa)."
  default     = false
}

variable "enable_flow_logs" {
  type        = bool
  description = "Enable VPC flow logs."
  default     = true
}

variable "flow_log_bucket" {
  type        = string
  description = "S3 bucket receiving flow logs."
  default     = ""
}

variable "enable_dns_hostnames" {
  type        = bool
  description = "Enable DNS hostnames in the VPC."
  default     = true
}

locals {
  az_count = length(var.azs)
}

resource "aws_vpc" "this" {
  cidr_block           = var.cidr
  enable_dns_support   = true
  enable_dns_hostnames = var.enable_dns_hostnames

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-vpc" })
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-igw" })
}

# --- Subnets --------------------------------------------------------------------

resource "aws_subnet" "public" {
  count = local.az_count

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnets[count.index]
  availability_zone       = var.azs[count.index]
  map_public_ip_on_launch = true

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-public-${var.azs[count.index]}", Type = "public" })
}

resource "aws_subnet" "private" {
  count = local.az_count

  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnets[count.index]
  availability_zone = var.azs[count.index]

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-private-${var.azs[count.index]}", Type = "private" })
}

# --- NAT gateways ----------------------------------------------------------------

locals {
  nat_count = var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : local.az_count) : 0
}

resource "aws_eip" "nat" {
  count = local.nat_count

  domain = "vpc"

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-nat-eip-${count.index}" })
}

resource "aws_nat_gateway" "this" {
  count = local.nat_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index % local.az_count].id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-nat-${count.index}" })

  depends_on = [aws_internet_gateway.this]
}

# --- Route tables ----------------------------------------------------------------

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-rt-public" })
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.this.id
}

resource "aws_route_table_association" "public" {
  count = local.az_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  count = local.nat_count

  vpc_id = aws_vpc.this.id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-rt-private-${count.index}" })
}

resource "aws_route" "private_nat" {
  count = local.nat_count

  route_table_id         = aws_route_table.private[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.this[count.index].id
}

resource "aws_route_table_association" "private" {
  count = local.az_count

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = var.single_nat_gateway ? aws_route_table.private[0].id : aws_route_table.private[count.index].id
}

# --- Network ACLs -----------------------------------------------------------------

resource "aws_network_acl" "public" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-nacl-public" })
}

resource "aws_network_acl_rule" "public_ingress" {
  network_acl_id = aws_network_acl.public.id
  rule_number    = 100
  egress         = false
  protocol       = "-1"
  rule_action    = "allow"
  cidr_block     = "0.0.0.0/0"
}

resource "aws_network_acl_rule" "public_egress" {
  network_acl_id = aws_network_acl.public.id
  rule_number    = 100
  egress         = true
  protocol       = "-1"
  rule_action    = "allow"
  cidr_block     = "0.0.0.0/0"
}

resource "aws_network_acl" "private" {
  vpc_id = aws_vpc.this.id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-nacl-private" })
}

resource "aws_network_acl_rule" "private_ingress_vpc" {
  network_acl_id = aws_network_acl.private.id
  rule_number    = 100
  egress         = false
  protocol       = "-1"
  rule_action    = "allow"
  cidr_block     = var.cidr
}

resource "aws_network_acl_rule" "private_ingress_egress_nat" {
  network_acl_id = aws_network_acl.private.id
  rule_number    = 110
  egress         = false
  protocol       = "tcp"
  rule_action    = "allow"
  cidr_block     = "0.0.0.0/0"
  from_port      = 1024
  to_port        = 65535
}

resource "aws_network_acl_rule" "private_egress" {
  network_acl_id = aws_network_acl.private.id
  rule_number    = 100
  egress         = true
  protocol       = "-1"
  rule_action    = "allow"
  cidr_block     = "0.0.0.0/0"
}

resource "aws_network_acl_association" "public" {
  count = local.az_count

  subnet_id      = aws_subnet.public[count.index].id
  network_acl_id = aws_network_acl.public.id
}

resource "aws_network_acl_association" "private" {
  count = local.az_count

  subnet_id      = aws_subnet.private[count.index].id
  network_acl_id = aws_network_acl.private.id
}

# --- Flow logs ----------------------------------------------------------------------

resource "aws_flow_log" "vpc" {
  count = var.enable_flow_logs ? 1 : 0

  iam_role_arn             = aws_iam_role.flow_log[0].arn
  log_destination          = var.flow_log_bucket != "" ? var.flow_log_bucket : aws_cloudwatch_log_group.flow_log[0].arn
  traffic_type             = "ALL"
  vpc_id                   = aws_vpc.this.id
  max_aggregation_interval = 60

  destination_options {
    file_format        = "parquet"
    per_hour_partition = true
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-flow-log" })
}

resource "aws_cloudwatch_log_group" "flow_log" {
  count = (var.enable_flow_logs && var.flow_log_bucket == "") ? 1 : 0

  name              = "/aws/vpc/flow-log/${var.name_prefix}"
  retention_in_days = 30
  tags              = var.common_tags
}

resource "aws_iam_role" "flow_log" {
  count = var.enable_flow_logs ? 1 : 0

  name = "${var.name_prefix}-vpc-flow-log"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "vpc-flow-logs.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy" "flow_log" {
  count = var.enable_flow_logs ? 1 : 0

  name = "publish-flow-logs"
  role = aws_iam_role.flow_log[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams",
        ]
        Resource = "*"
      }
    ]
  })
}

# --- Outputs --------------------------------------------------------------------------

output "vpc_id" {
  value = aws_vpc.this.id
}

output "vpc_cidr" {
  value = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "azs" {
  value = var.azs
}

output "nat_gateway_ids" {
  value = aws_nat_gateway.this[*].id
}

output "private_route_table_ids" {
  description = "Private route table ids (gateway-endpoint associations)."
  value       = compact(aws_route_table.private[*].id)
}

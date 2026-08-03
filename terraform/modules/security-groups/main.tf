# -----------------------------------------------------------------------------
# Module: security-groups
#
# Reusable Security Group factory. `ingress` and `egress` rules are expressed
# as lists so a single module call can describe the complete network policy of
# a component. Rules support source security-group references and CIDRs.
# -----------------------------------------------------------------------------

variable "name" {
  type        = string
  description = "Security group name (prefixed by the module caller)."
}

variable "description" {
  type        = string
  description = "Human readable description of the security group."
  default     = ""
}

variable "vpc_id" {
  type        = string
  description = "VPC the security group belongs to."
}

variable "common_tags" {
  type        = map(string)
  description = "Common platform tags."
  default     = {}
}

variable "ingress_rules" {
  type = list(object({
    description      = optional(string, "")
    from_port        = number
    to_port          = number
    protocol         = string
    cidr_blocks      = optional(list(string), [])
    source_sg_ids    = optional(list(string), [])
    self             = optional(bool, false)
    ipv6_cidr_blocks = optional(list(string), [])
  }))
  description = "Ingress rules."
  default     = []
}

variable "egress_rules" {
  type = list(object({
    description      = optional(string, "")
    from_port        = number
    to_port          = number
    protocol         = string
    cidr_blocks      = optional(list(string), [])
    target_sg_ids    = optional(list(string), [])
    self             = optional(bool, false)
    ipv6_cidr_blocks = optional(list(string), [])
  }))
  description = "Egress rules."
  default = [
    {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = ["0.0.0.0/0"]
    }
  ]
}

resource "aws_security_group" "this" {
  name        = var.name
  description = var.description
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = var.name })
}

resource "aws_security_group_rule" "ingress" {
  count = length(var.ingress_rules)

  type                     = "ingress"
  security_group_id        = aws_security_group.this.id
  description              = lookup(var.ingress_rules[count.index], "description", "")
  from_port                = var.ingress_rules[count.index].from_port
  to_port                  = var.ingress_rules[count.index].to_port
  protocol                 = var.ingress_rules[count.index].protocol
  cidr_blocks              = length(var.ingress_rules[count.index].cidr_blocks) > 0 ? var.ingress_rules[count.index].cidr_blocks : null
  source_security_group_id = length(var.ingress_rules[count.index].source_sg_ids) > 0 ? var.ingress_rules[count.index].source_sg_ids[0] : null
  self                     = var.ingress_rules[count.index].self
  ipv6_cidr_blocks         = length(var.ingress_rules[count.index].ipv6_cidr_blocks) > 0 ? var.ingress_rules[count.index].ipv6_cidr_blocks : null
}

resource "aws_security_group_rule" "egress" {
  count = length(var.egress_rules)

  type                     = "egress"
  security_group_id        = aws_security_group.this.id
  description              = lookup(var.egress_rules[count.index], "description", "")
  from_port                = var.egress_rules[count.index].from_port
  to_port                  = var.egress_rules[count.index].to_port
  protocol                 = var.egress_rules[count.index].protocol
  cidr_blocks              = length(var.egress_rules[count.index].cidr_blocks) > 0 ? var.egress_rules[count.index].cidr_blocks : null
  source_security_group_id = length(var.egress_rules[count.index].target_sg_ids) > 0 ? var.egress_rules[count.index].target_sg_ids[0] : null
  self                     = var.egress_rules[count.index].self
  ipv6_cidr_blocks         = length(var.egress_rules[count.index].ipv6_cidr_blocks) > 0 ? var.egress_rules[count.index].ipv6_cidr_blocks : null
}

output "id" {
  description = "Security group id."
  value       = aws_security_group.this.id
}

output "arn" {
  description = "Security group arn."
  value       = aws_security_group.this.arn
}

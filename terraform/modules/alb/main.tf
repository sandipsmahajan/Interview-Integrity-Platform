# -----------------------------------------------------------------------------
# Module: alb
#
# Front-door Application Load Balancer. Terminates TLS (ACM certificate) and
# forwards to the in-cluster nginx-ingress-controller NodePort service:
#
#   Internet -> ALB(443, ACM TLS) -> NodePort(30080) -> ingress-nginx ->
#   Ingress rules -> microservice Services
#
# Target type is `instance` and the EKS node group ASG is auto-registered, so
# nodes joining/leaving the cluster are tracked automatically.
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

variable "public_subnet_ids" {
  type        = list(string)
  description = "Public subnets (internet-facing ALB)."
}

variable "node_sg_id" {
  type        = string
  description = "EKS node security group (egress target and ingress grant)."
}

variable "node_group_asg_names" {
  type        = list(string)
  description = "EKS managed node group ASG names to register as targets."
}

variable "ingress_node_port" {
  type        = number
  description = "NodePort exposed by the nginx-ingress-controller service."
  default     = 30080
}

variable "acm_certificate_arn" {
  type        = string
  description = "ACM certificate ARN for the listener."
}

variable "waf_web_acl_arn" {
  type        = string
  description = "WAF web ACL ARN (optional)."
  default     = ""
}

variable "access_logs_bucket" {
  type        = string
  description = "S3 bucket receiving ALB access logs."
  default     = ""
}

variable "internal" {
  type        = bool
  description = "Internal vs internet-facing load balancer."
  default     = false
}

resource "aws_security_group" "alb" {
  name        = "${var.name_prefix}-alb"
  description = "Front ALB security group for ${var.name_prefix}"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-alb" })
}

resource "aws_security_group_rule" "alb_https_ingress" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTPS from internet"
}

resource "aws_security_group_rule" "alb_http_ingress" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTP from internet (redirect to 443)"
}

resource "aws_security_group_rule" "alb_egress_nodes" {
  type                     = "egress"
  security_group_id        = aws_security_group.alb.id
  from_port                = var.ingress_node_port
  to_port                  = var.ingress_node_port
  protocol                 = "tcp"
  source_security_group_id = var.node_sg_id
  description              = "Forward to ingress-nginx NodePort"
}

# Node security group grants access from the ALB on the ingress NodePort.
resource "aws_security_group_rule" "node_ingress_alb" {
  type                     = "ingress"
  security_group_id        = var.node_sg_id
  from_port                = var.ingress_node_port
  to_port                  = var.ingress_node_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  description              = "Ingress-nginx NodePort from front ALB"
}

resource "aws_lb" "this" {
  name               = "${var.name_prefix}-alb"
  internal           = var.internal
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false
  enable_http2               = true
  idle_timeout               = 60
  preserve_host_header       = true
  xff_header_processing_mode = "append"

  dynamic "access_logs" {
    for_each = var.access_logs_bucket != "" ? [1] : []
    content {
      bucket  = var.access_logs_bucket
      prefix  = "${var.name_prefix}/alb"
      enabled = true
    }
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-alb" })
}

resource "aws_lb_target_group" "ingress" {
  name        = "${var.name_prefix}-ingress"
  port        = var.ingress_node_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "instance"

  health_check {
    enabled             = true
    protocol            = "HTTP"
    path                = "/healthz"
    port                = "traffic-port"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 10
    timeout             = 5
    matcher             = "200-399"
  }

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-ingress" })
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.ingress.arn
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_autoscaling_attachment" "nodes" {
  count = length(var.node_group_asg_names)

  autoscaling_group_name = var.node_group_asg_names[count.index]
  lb_target_group_arn    = aws_lb_target_group.ingress.arn
}

# --- WAF association -----------------------------------------------------------------

resource "aws_wafv2_web_acl_association" "waf" {
  count = var.waf_web_acl_arn != "" ? 1 : 0

  resource_arn = aws_lb.this.arn
  web_acl_arn  = var.waf_web_acl_arn
}

output "alb_id" {
  value = aws_lb.this.id
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "alb_zone_id" {
  value = aws_lb.this.zone_id
}

output "alb_arn_suffix" {
  value = aws_lb.this.arn_suffix
}

output "target_group_arn" {
  value = aws_lb_target_group.ingress.arn
}

output "security_group_id" {
  value = aws_security_group.alb.id
}

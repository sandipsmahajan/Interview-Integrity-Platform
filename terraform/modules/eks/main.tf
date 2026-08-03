# -----------------------------------------------------------------------------
# Module: eks
#
# Amazon EKS cluster + managed node groups + IRSA:
#   * control plane logging to CloudWatch, secrets encrypted with KMS
#   * managed node groups on private subnets (no public IPs)
#   * EKS managed addons (vpc-cni, coredns, kube-proxy, aws-ebs-csi-driver)
#   * IRSA roles for aws-load-balancer-controller, cluster-autoscaler,
#     external-dns
#   * node security group consumed by rds/redis/kafka/alb modules
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
  description = "Private subnets for node groups."
}

variable "cluster_version" {
  type        = string
  description = "Kubernetes version for the control plane and node groups."
  default     = "1.31"
}

variable "cluster_encryption_kms_key_arn" {
  type        = string
  description = "KMS key ARN encrypting Kubernetes secrets."
}

variable "node_instance_types" {
  type        = list(string)
  description = "Instance types for the main application node group."
  default     = ["m6i.large"]
}

variable "node_desired_size" {
  type        = number
  description = "Desired nodes."
  default     = 3
}

variable "node_min_size" {
  type        = number
  description = "Minimum nodes."
  default     = 3
}

variable "node_max_size" {
  type        = number
  description = "Maximum nodes (HPA + cluster autoscaler upper bound)."
  default     = 9
}

variable "node_disk_size_gb" {
  type        = number
  description = "Node root volume size (GiB)."
  default     = 100
}

variable "node_disk_kms_key_arn" {
  type        = string
  description = "KMS key ARN encrypting node EBS volumes."
}

variable "ami_type" {
  type        = string
  description = "AMI type: AL2_x86_64 | AL2_x86_64_GPU | BOTTLEROCKET_x86_64."
  default     = "AL2_x86_64"
}

variable "cluster_log_types" {
  type        = list(string)
  description = "EKS control plane log types."
  default     = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
}

variable "cloudwatch_log_group_name" {
  type        = string
  description = "CloudWatch log group for the EKS control plane."
  default     = ""
}

variable "admin_principal_arn" {
  type        = string
  description = "IAM principal granted cluster-admin via access entries (break glass)."
  default     = ""
}

data "aws_region" "current" {}
data "aws_caller_identity" "current" {}

locals {
  cluster_name = var.name_prefix

  # Security group rules between control plane and nodes.
  node_port_range = 1024
}

# --- Cluster IAM role -----------------------------------------------------------

resource "aws_iam_role" "cluster" {
  name = "${var.name_prefix}-eks-cluster"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "eks.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "cluster" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

# --- Node role -------------------------------------------------------------------

resource "aws_iam_role" "node" {
  name = "${var.name_prefix}-eks-node"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "ec2.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "node_worker" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "node_cni" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

resource "aws_iam_role_policy_attachment" "node_registry" {
  role       = aws_iam_role.node.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# --- Cluster -----------------------------------------------------------------------

resource "aws_eks_cluster" "this" {
  name                      = local.cluster_name
  version                   = var.cluster_version
  role_arn                  = aws_iam_role.cluster.arn
  enabled_cluster_log_types = var.cluster_log_types

  vpc_config {
    subnet_ids              = concat(var.private_subnet_ids, [])
    endpoint_private_access = true
    endpoint_public_access  = false
    public_access_cidrs     = []
    security_group_ids      = []
  }

  encryption_config {
    provider {
      key_arn = var.cluster_encryption_kms_key_arn
    }
    resources = ["secrets"]
  }

  timeouts {
    create = "40m"
    update = "60m"
    delete = "40m"
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster,
    aws_cloudwatch_log_group.control_plane,
  ]

  tags = var.common_tags
}

resource "aws_cloudwatch_log_group" "control_plane" {
  name              = var.cloudwatch_log_group_name != "" ? var.cloudwatch_log_group_name : "/aws/eks/${var.name_prefix}/cluster"
  retention_in_days = 30
  tags              = var.common_tags
}

# --- Node security group -----------------------------------------------------------

resource "aws_security_group" "node" {
  name        = "${var.name_prefix}-eks-node"
  description = "Security group for ${var.name_prefix} EKS worker nodes"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, { Name = "${var.name_prefix}-eks-node" })
}

resource "aws_security_group_rule" "node_egress" {
  type              = "egress"
  security_group_id = aws_security_group.node.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "node_self" {
  type              = "ingress"
  security_group_id = aws_security_group.node.id
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  self              = true
}

resource "aws_security_group_rule" "node_kubelet_from_cp" {
  type                     = "ingress"
  security_group_id        = aws_security_group.node.id
  from_port                = 10250
  to_port                  = 10250
  protocol                 = "tcp"
  source_security_group_id = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

resource "aws_security_group_rule" "node_443_from_cp" {
  type                     = "ingress"
  security_group_id        = aws_security_group.node.id
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  source_security_group_id = aws_eks_cluster.this.vpc_config[0].cluster_security_group_id
}

# --- Launch template + node group ----------------------------------------------------

resource "aws_launch_template" "node" {
  name = "${var.name_prefix}-eks-node-lt"

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_size           = var.node_disk_size_gb
      volume_type           = "gp3"
      encrypted             = true
      kms_key_id            = var.node_disk_kms_key_arn
      delete_on_termination = true
    }
  }

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
    instance_metadata_tags      = "disabled"
  }

  monitoring {
    enabled = true
  }

  tag_specifications {
    resource_type = "instance"
    tags          = merge(var.common_tags, { Name = "${var.name_prefix}-eks-node" })
  }

  tag_specifications {
    resource_type = "volume"
    tags          = merge(var.common_tags, { Name = "${var.name_prefix}-eks-node" })
  }
}

resource "aws_eks_node_group" "app" {
  cluster_name    = aws_eks_cluster.this.name
  node_group_name = "${var.name_prefix}-app"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.private_subnet_ids
  ami_type        = var.ami_type
  version         = var.cluster_version

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  launch_template {
    id      = aws_launch_template.node.id
    version = aws_launch_template.node.latest_version
  }

  labels = {
    nodegroup-type = "managed-app"
  }

  update_config {
    max_unavailable = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.node_worker,
    aws_iam_role_policy_attachment.node_cni,
    aws_iam_role_policy_attachment.node_registry,
  ]

  tags = var.common_tags
}

# --- Managed addons --------------------------------------------------------------------

resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "vpc-cni"
}

resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "coredns"
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.this.name
  addon_name   = "kube-proxy"
}

resource "aws_iam_role" "ebs_csi" {
  name = "${var.name_prefix}-eks-ebs-csi"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Federated = aws_iam_openid_connect_provider.this.arn }
        Action    = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://", "")}:sub" = "system:serviceaccount:kube-system:ebs-csi-controller-sa"
          }
        }
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "ebs_csi" {
  role       = aws_iam_role.ebs_csi.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
}

resource "aws_eks_addon" "ebs_csi_driver" {
  cluster_name             = aws_eks_cluster.this.name
  addon_name               = "aws-ebs-csi-driver"
  service_account_role_arn = aws_iam_role.ebs_csi.arn
}

# --- OIDC provider -----------------------------------------------------------------------

data "tls_certificate" "oidc" {
  url = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "this" {
  url             = aws_eks_cluster.this.identity[0].oidc[0].issuer
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.oidc.certificates[0].sha1_fingerprint]
}

# --- IRSA roles ----------------------------------------------------------------------------

locals {
  oidc_issuer = replace(aws_eks_cluster.this.identity[0].oidc[0].issuer, "https://", "")
}

resource "aws_iam_role" "lb_controller" {
  name = "${var.name_prefix}-eks-lb-controller"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Federated = aws_iam_openid_connect_provider.this.arn }
        Action    = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${local.oidc_issuer}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
          }
        }
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_policy" "lb_controller" {
  name = "${var.name_prefix}-eks-lb-controller"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:DescribeAccountAttributes",
          "ec2:DescribeAddresses",
          "ec2:DescribeAvailabilityZones",
          "ec2:DescribeInternetGateways",
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceStatus",
          "ec2:DescribeInstanceTypes",
          "ec2:DescribeListeners",
          "ec2:DescribeLoadBalancers",
          "ec2:DescribeLoadBalancerAttributes",
          "ec2:DescribeNetworkInterfaces",
          "ec2:DescribeSecurityGroups",
          "ec2:DescribeSubnets",
          "ec2:DescribeTags",
          "ec2:DescribeVpcs",
          "ec2:GetCoipPoolUsage",
          "elasticloadbalancing:AddTags",
          "elasticloadbalancing:CreateListener",
          "elasticloadbalancing:CreateLoadBalancer",
          "elasticloadbalancing:CreateRule",
          "elasticloadbalancing:CreateTargetGroup",
          "elasticloadbalancing:DeleteListener",
          "elasticloadbalancing:DeleteLoadBalancer",
          "elasticloadbalancing:DeleteRule",
          "elasticloadbalancing:DeleteTargetGroup",
          "elasticloadbalancing:DescribeListeners",
          "elasticloadbalancing:DescribeLoadBalancers",
          "elasticloadbalancing:DescribeRules",
          "elasticloadbalancing:DescribeTags",
          "elasticloadbalancing:DescribeTargetGroups",
          "elasticloadbalancing:DescribeTargetGroupAttributes",
          "elasticloadbalancing:DescribeTargetHealth",
          "elasticloadbalancing:ModifyListener",
          "elasticloadbalancing:ModifyLoadBalancerAttributes",
          "elasticloadbalancing:ModifyRule",
          "elasticloadbalancing:ModifyTargetGroup",
          "elasticloadbalancing:ModifyTargetGroupAttributes",
          "elasticloadbalancing:RemoveTags",
          "elasticloadbalancing:SetSecurityGroups",
          "elasticloadbalancing:SetSubnets",
          "elasticloadbalancing:RegisterTargets",
          "elasticloadbalancing:DeregisterTargets",
          "elasticloadbalancing:SetIpAddressType",
          "elasticloadbalancing:SetRulePriorities",
          "waf-regional:GetWebACLForResource",
          "waf-regional:GetWebACL",
          "waf-regional:AssociateWebACL",
          "waf-regional:DisassociateWebACL",
          "cognito-idp:DescribeUserPoolClient",
          "iam:CreateServiceLinkedRole",
          "iam:GetServerCertificate",
          "iam:ListServerCertificates",
          "acm:DescribeCertificate",
          "acm:ListCertificates",
          "acm:GetCertificate",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lb_controller" {
  role       = aws_iam_role.lb_controller.name
  policy_arn = aws_iam_policy.lb_controller.arn
}

resource "aws_iam_role" "cluster_autoscaler" {
  name = "${var.name_prefix}-eks-cluster-autoscaler"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Federated = aws_iam_openid_connect_provider.this.arn }
        Action    = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${local.oidc_issuer}:sub" = "system:serviceaccount:kube-system:cluster-autoscaler"
          }
        }
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_policy" "cluster_autoscaler" {
  name = "${var.name_prefix}-eks-cluster-autoscaler"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "autoscaling:DescribeAutoScalingGroups",
          "autoscaling:DescribeAutoScalingInstances",
          "autoscaling:DescribeLaunchConfigurations",
          "autoscaling:DescribeScalingActivities",
          "autoscaling:DescribeTags",
          "autoscaling:SetDesiredCapacity",
          "autoscaling:TerminateInstanceInAutoScalingGroup",
          "ec2:DescribeInstanceTypes",
          "eks:DescribeNodegroup",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_autoscaler" {
  role       = aws_iam_role.cluster_autoscaler.name
  policy_arn = aws_iam_policy.cluster_autoscaler.arn
}

resource "aws_iam_role" "external_dns" {
  name = "${var.name_prefix}-eks-external-dns"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Federated = aws_iam_openid_connect_provider.this.arn }
        Action    = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${local.oidc_issuer}:sub" = "system:serviceaccount:external-dns:external-dns"
          }
        }
      }
    ]
  })

  tags = var.common_tags
}

resource "aws_iam_policy" "external_dns" {
  name = "${var.name_prefix}-eks-external-dns"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "route53:ChangeResourceRecordSets",
          "route53:ListHostedZones",
          "route53:ListResourceRecordSets",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "external_dns" {
  role       = aws_iam_role.external_dns.name
  policy_arn = aws_iam_policy.external_dns.arn
}

# --- Break-glass admin access entry -------------------------------------------------------

resource "aws_eks_access_entry" "admin" {
  count = var.admin_principal_arn != "" ? 1 : 0

  cluster_name  = aws_eks_cluster.this.name
  principal_arn = var.admin_principal_arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "admin" {
  count = var.admin_principal_arn != "" ? 1 : 0

  cluster_name  = aws_eks_cluster.this.name
  principal_arn = aws_eks_access_entry.admin[0].principal_arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }
}

# --- Outputs ----------------------------------------------------------------------------------

output "cluster_name" {
  value = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  value = aws_eks_cluster.this.endpoint
}

output "cluster_certificate_authority" {
  value = aws_eks_cluster.this.certificate_authority[0].data
}

output "cluster_oidc_issuer" {
  value = aws_eks_cluster.this.identity[0].oidc[0].issuer
}

output "oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.this.arn
}

output "node_sg_id" {
  description = "Node security group id (ingress source for rds/redis/msk/alb)."
  value       = aws_security_group.node.id
}

output "node_role_arn" {
  value = aws_iam_role.node.arn
}

output "node_group_asg_name" {
  description = "Autoscaling group name of the application node group (ALB target registration)."
  value       = aws_eks_node_group.app.resources[0].autoscaling_groups[0].name
}

output "lb_controller_irsa_arn" {
  value = aws_iam_role.lb_controller.arn
}

output "cluster_autoscaler_irsa_arn" {
  value = aws_iam_role.cluster_autoscaler.arn
}

output "external_dns_irsa_arn" {
  value = aws_iam_role.external_dns.arn
}

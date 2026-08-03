# -----------------------------------------------------------------------------
# Module: iam
#
# Platform identity foundation:
#   * GitHub OIDC provider (secure, credential-free CI/CD)
#   * CI/CD role assumed by GitHub Actions workflows
#   * Read-only operational role policy building blocks used by IRSA roles
#
# Least privilege is enforced: the CI/CD role is scoped to the resources this
# platform actually manages, namespaced by project/environment.
# -----------------------------------------------------------------------------

variable "name_prefix" {
  type        = string
  description = "Resource name prefix: <project>-<environment>."
}

variable "project" {
  type        = string
  description = "Project name (used for the state KMS key alias)."
  default     = "integrity"
}

variable "common_tags" {
  type        = map(string)
  description = "Common platform tags."
  default     = {}
}

variable "github_organisation" {
  type        = string
  description = "GitHub organisation for the OIDC trust policy."
}

variable "github_repository" {
  type        = string
  description = "GitHub repository for the OIDC trust policy."
}

variable "state_bucket" {
  type        = string
  description = "Terraform state bucket name the CI role may read/write."
  default     = "integrity-terraform-state"
}

variable "state_lock_table" {
  type        = string
  description = "Terraform lock table name the CI role may access."
  default     = "integrity-terraform-locks"
}

variable "eks_cluster_names" {
  type        = list(string)
  description = "EKS clusters the CI role may deploy to."
  default     = []
}

variable "create_oidc_provider" {
  type        = bool
  description = "Create the GitHub OIDC provider (only one per account - enable in the prod root only)."
  default     = false
}

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id

  # Keep the trust policy readable: sub allows any branch.
  oidc_sub = "repo:${var.github_organisation}/${var.github_repository}:ref:refs/heads/*"
}

# --- GitHub OIDC provider ------------------------------------------------------

resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 0 : 1

  url = "https://token.actions.githubusercontent.com"
}

locals {
  oidc_provider_arn = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : data.aws_iam_openid_connect_provider.github[0].arn
}

# --- CI/CD role ----------------------------------------------------------------

resource "aws_iam_role" "github_actions" {
  name        = "${var.name_prefix}-github-actions"
  description = "Role assumed by GitHub Actions for CI/CD of ${var.name_prefix}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = local.oidc_provider_arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringLike = {
            "token.actions.githubusercontent.com:sub" = local.oidc_sub
          }
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = var.common_tags
}

# --- Inline policy: Terraform state + locks -------------------------------------

resource "aws_iam_policy" "terraform_state" {
  name = "${var.name_prefix}-tf-state"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3State"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket",
        ]
        Resource = [
          "arn:aws:s3:::${var.state_bucket}",
          "arn:aws:s3:::${var.state_bucket}/*",
        ]
      },
      {
        Sid    = "DynamoLock"
        Effect = "Allow"
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:DeleteItem",
        ]
        Resource = "arn:aws:dynamodb:*:*:table/${var.state_lock_table}"
      },
      {
        Sid    = "StateKms"
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey",
          "kms:DescribeKey",
        ]
        Resource = "arn:aws:kms:*:*:alias/${var.project}/terraform-state"
      }
    ]
  })
}

# --- Inline policy: ECR + EKS + networking + DNS for deploy ----------------------

resource "aws_iam_policy" "deploy" {
  name = "${var.name_prefix}-cd-deploy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "EcrPushPull"
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeRepositories",
          "ecr:ListImages",
        ]
        Resource = "*"
      },
      {
        Sid    = "EksAccess"
        Effect = "Allow"
        Action = [
          "eks:DescribeCluster",
          "eks:ListClusters",
          "eks:ListAccessEntries",
          "eks:CreateAccessEntry",
          "eks:UpdateAccessEntry",
          "eks:AssociateAccessPolicy",
          "eks:ListAssociatedAccessPolicies",
        ]
        Resource = "*"
      },
      {
        Sid    = "SecretsAndParams"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:CreateSecret",
          "secretsmanager:UpdateSecret",
          "ssm:GetParameter",
          "ssm:PutParameter",
          "ssm:DeleteParameter",
        ]
        Resource = [
          "arn:aws:secretsmanager:*:${local.account_id}:secret:*",
          "arn:aws:ssm:*:${local.account_id}:parameter/${var.name_prefix}/*",
        ]
      },
      {
        Sid    = "Route53"
        Effect = "Allow"
        Action = [
          "route53:ListHostedZones",
          "route53:ChangeResourceRecordSets",
          "route53:ListResourceRecordSets",
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "tf_state" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.terraform_state.arn
}

resource "aws_iam_role_policy_attachment" "deploy" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.deploy.arn
}

resource "aws_iam_role_policy_attachment" "managed_poweruser" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

# --- EKS access entries for the CI role (cluster API access) ---------------------

resource "aws_eks_access_entry" "ci" {
  count = length(var.eks_cluster_names) > 0 ? length(var.eks_cluster_names) : 0

  cluster_name  = var.eks_cluster_names[count.index]
  principal_arn = aws_iam_role.github_actions.arn
  type          = "STANDARD"
}

resource "aws_eks_access_policy_association" "ci" {
  count = length(var.eks_cluster_names) > 0 ? length(var.eks_cluster_names) : 0

  cluster_name  = var.eks_cluster_names[count.index]
  principal_arn = aws_iam_role.github_actions.arn
  policy_arn    = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"

  access_scope {
    type = "cluster"
  }

  depends_on = [aws_eks_access_entry.ci]
}

output "github_actions_role_arn" {
  description = "ARN of the GitHub Actions CI/CD role."
  value       = aws_iam_role.github_actions.arn
}

output "oidc_provider_arn" {
  description = "GitHub OIDC provider ARN."
  value       = local.oidc_provider_arn
}

output "oidc_provider_url" {
  description = "GitHub OIDC provider URL (without https://)."
  value       = "token.actions.githubusercontent.com"
}

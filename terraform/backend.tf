# -----------------------------------------------------------------------------
# Remote state - Amazon S3 with DynamoDB state locking.
#
# The state bucket and lock table are created once by the bootstrap root
# (terraform/bootstrap) and shared by every environment root. Keys are
# namespaced per environment so environments never overwrite each other.
#
#   terraform/bootstrap/  ->  creates:  integrity-terraform-state-<account>
#                                      + integrity-terraform-locks-<account>
#
# Per-environment roots override the key:
#   environments/dev  -> key = integrity/dev/terraform.tfstate
#   environments/qa   -> key = integrity/qa/terraform.tfstate
#   environments/uat  -> key = integrity/uat/terraform.tfstate
#   environments/prod -> key = integrity/prod/terraform.tfstate
# -----------------------------------------------------------------------------
terraform {
  backend "s3" {
    bucket         = "integrity-terraform-state"
    key            = "integrity/blueprint/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "integrity-terraform-locks"
    encrypt        = true
    # SSE-KMS is enabled once the bootstrap root has created the key. See
    # bootstrap/README.md for enabling kms_key_id after the first bootstrap.
  }
}

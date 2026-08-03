# The bootstrap root uses the local backend - it has to create the remote
# state bucket itself. After the first `terraform apply` the other roots can
# switch to the S3 backend created here.
terraform {
  backend "local" {}
}

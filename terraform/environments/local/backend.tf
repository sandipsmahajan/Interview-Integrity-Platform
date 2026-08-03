terraform {
  backend "s3" {
    bucket         = "integrity-terraform-state"
    key            = "integrity/local/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "integrity-terraform-locks"
    encrypt        = true
  }
}

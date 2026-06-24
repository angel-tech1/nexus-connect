provider "aws" {
  access_key = "test"
  secret_key = "test"
  region     = "us-east-1"

  # required for non virtual hosted-style endpoint use case.
  # https://registry.terraform.io/providers/hashicorp/aws/latest/docs#s3_use_path_style

  # Floci localstack alternative - all 59 AWS services
  # https://floci.dev/docs
  s3_use_path_style           = true
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  # Floci endpoints - all services at localhost:4566
  # https://floci.dev/
  endpoints {
    s3             = "http://localhost:4566"
    rds            = "http://localhost:4566"
  }
}

resource "aws_s3_bucket" "avatars-bucket" {
  bucket = "avatars-bucket"
}

module "database" {
  source = "./modules/aws-rds-floci"

  region              = var.region
  project_name        = var.project_name
  db_username         = var.db_username
  db_password         = var.db_password
  subnet_ids          = var.subnet_ids
  security_group_ids  = var.security_group_ids
  multi_az            = var.db_multi_az
  publicly_accessible = var.db_publicly_accessible
  db_instance_class   = var.db_instance_class
  db_storage_size     = var.db_storage_size
}

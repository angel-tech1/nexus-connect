variable "region" {
  type        = string
  description = "AWS region for all resources."

  default = "us-east-1"
}

variable "project_name" {
  type        = string
  description = "Name of the project."

  default = "nexus-connect"
}

variable "db_username" {
  type        = string
  description = "PostgreSQL master username."

  default = "nexusadmin"
}

variable "db_password" {
  type        = string
  description = "PostgreSQL master password."
  sensitive   = true

  default = "nexusdev123"
}

variable "subnet_ids" {
  type        = list(string)
  description = "List of subnet IDs for the database."

  default = ["subnet-12345678"]
}

variable "security_group_ids" {
  type        = list(string)
  description = "List of security group IDs for the database."

  default = ["sg-12345678"]
}

variable "db_instance_class" {
  type        = string
  description = "Instance class for the RDS instance."

  default = "db.t3.micro"
}

variable "db_storage_size" {
  type        = number
  description = "Storage size in GB for the RDS instance."

  default = 20
}

variable "db_multi_az" {
  type        = bool
  description = "Whether to deploy database across multiple AZs."

  default = false
}

variable "db_publicly_accessible" {
  type        = bool
  description = "Whether the database instance is publicly accessible."

  default = false
}

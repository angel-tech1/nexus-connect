variable "region" {
  type        = string
  description = "AWS region for the database."
}

variable "project_name" {
  type        = string
  description = "Name of the project (used in resource naming)."

  default = "nexus-connect"
}

variable "db_version" {
  type        = string
  description = "PostgreSQL engine version."

  default = "15"
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

variable "db_max_connections" {
  type        = number
  description = "Maximum database connections."

  default = 100
}

variable "db_username" {
  type        = string
  description = "Master username for the database."
}

variable "db_password" {
  type        = string
  description = "Master password for the database."
  sensitive   = true
}

variable "subnet_ids" {
  type        = list(string)
  description = "List of subnet IDs for the database."
}

variable "security_group_ids" {
  type        = list(string)
  description = "List of security group IDs for the database."
}

variable "multi_az" {
  type        = bool
  description = "Whether to deploy across multiple AZs."

  default = false
}

variable "publicly_accessible" {
  type        = bool
  description = "Whether the database instance is publicly accessible."

  default = false
}

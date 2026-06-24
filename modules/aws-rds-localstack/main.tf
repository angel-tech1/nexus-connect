resource "aws_db_parameter_group" "nexus_connect" {
  name   = "${var.project_name}-db-params"
  family = "postgresql15"

  parameter {
    name  = "max_connections"
    value = var.db_max_connections
  }
}

resource "aws_db_subnet_group" "nexus_connect" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "nexus_connect" {
  identifier              = "${var.project_name}-db"
  engine                  = "postgres"
  engine_version          = var.db_version
  instance_class          = var.db_instance_class
  allocated_storage       = var.db_storage_size
  storage_type            = "gp2"
  username                = var.db_username
  password                = var.db_password
  parameter_group_name    = aws_db_parameter_group.nexus_connect.name
  db_subnet_group_name    = aws_db_subnet_group.nexus_connect.name
  vpc_security_group_ids  = var.security_group_ids
  multi_az                = var.multi_az
  skip_final_snapshot     = true
  publicly_accessible     = var.publicly_accessible
  auto_minor_version_upgrade = true

  tags = {
    Name  = "${var.project_name}-db"
    Owner = "${var.project_name}-maintainers"
  }
}

output "endpoint" {
  value = aws_db_instance.nexus_connect.endpoint
  description = "Database connection endpoint"
}

output "port" {
  value = aws_db_instance.nexus_connect.port
  description = "Database port"
}

output "name" {
  value = aws_db_instance.nexus_connect.db_name
  description = "Database name"
}

output "username" {
  value = aws_db_instance.nexus_connect.username
  description = "Database username"
}

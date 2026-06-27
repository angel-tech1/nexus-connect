# VPC
resource "aws_vpc" "nexus_connect" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "vpc-nexus-connect"
  }
}

# Subnet
resource "aws_subnet" "nexus_private" {
  vpc_id                  = aws_vpc.nexus_connect.id
  cidr_block              = "10.0.0.0/24"
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = false

  tags = {
    Name = "nexus-subnet-private"
  }
}

# DB Subnet Group
resource "aws_db_subnet_group" "nexus_connect_db" {
  name        = "nexus-connect-db-subnet"
  description = "Nexus Connect database subnet group"
  subnet_ids  = [aws_subnet.nexus_private.id]

  tags = {
    Name = "nexus-connect-db-subnet"
  }
}

# RDS Instance
resource "aws_db_instance" "nexus_connect_db" {
  identifier              = "pg-nexus-connect-db"
  engine                  = "postgres"
  engine_version          = "15"
  instance_class          = "db.m5"
  allocated_storage       = 1
  storage_type            = "io1"
  db_name                 = "pg-nexus-connect"
  username                = "pgadmin"
  password                = "pglocal"
  db_subnet_group_name    = aws_db_subnet_group.nexus_connect_db.name
  vpc_security_group_ids  = [aws_security_group.nexus_db.id]
  port                    = 5432
  backup_retention_period = 0
  skip_final_snapshot     = true

  tags = {
    Name = "pg-nexus-connect-db"
  }

  # Note: For io1, you also need to specify iops
  # iops = 1000 # Example value
}

# Security Group for RDS
resource "aws_security_group" "nexus_db" {
  name        = "nexus-db-sg"
  description = "Nexus Connect database security group"
  vpc_id      = aws_vpc.nexus_connect.id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "nexus-db-sg"
  }
}

output "endpoint" {
  value = aws_db_instance.nexus_connect_db.endpoint
  description = "Database connection endpoint"
}

output "port" {
  value = aws_db_instance.nexus_connect_db.port
  description = "Database port"
}

output "name" {
  value = aws_db_instance.nexus_connect_db.db_name
  description = "Database name"
}

output "username" {
  value = aws_db_instance.nexus_connect_db.username
  description = "Database username"
}

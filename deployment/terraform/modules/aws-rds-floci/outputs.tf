output "db_connection_string" {
  value = "postgresql://${var.db_username}:${var.db_password}@${aws_db_instance.nexus_connect_db.endpoint}:${aws_db_instance.nexus_connect_db.port}/${aws_db_instance.nexus_connect_db.db_name}"
  description = "Full JDBC connection string for the database"
}

output "db_url" {
  value = "jdbc:postgresql://${aws_db_instance.nexus_connect_db.endpoint}:${aws_db_instance.nexus_connect_db.port}/${aws_db_instance.nexus_connect_db.db_name}"
  description = "JDBC URL for Ktor application"
}

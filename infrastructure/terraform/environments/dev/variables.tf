variable "project_id" {
  description = "ID del proyecto GCP"
  type        = string
}

variable "region" {
  description = "Región donde se despliega"
  type        = string
  default     = "europe-west1"
}

variable "db_password" {
  description = "Contraseña del usuario de la BD (la pasamos con -var o tfvars)"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "APP_JWT_SECRET compartido por los servicios"
  type        = string
  sensitive   = true
}

variable "db_host" {
  description = "IP pública del Cloud SQL (la que viste en el plan: 34.59.171.207)"
  type        = string
}

variable "frontend_url" {
  description = "URL a la que apunta FRONTEND_URL (de momento localhost, luego el de Cloud Run)"
  type        = string
  default     = "http://localhost:4200"
}

variable "mongo_uri" {
  description = "MongoDB connection string (Atlas M0). Mismo valor para user y book."
  type        = string
  sensitive   = true
}

variable "rabbitmq_uri" {
  description = "AMQP URL de CloudAMQP (plan lemur). Se descompone en host/puerto/usuario/contraseña/vhost."
  type        = string
  sensitive   = true
}

variable "google_books_api_key" {
  description = "GOOGLE_BOOKS_API_KEY para book-service (opcional; sin ella, la búsqueda Google no funciona)"
  type        = string
  default     = ""
  sensitive   = true
}
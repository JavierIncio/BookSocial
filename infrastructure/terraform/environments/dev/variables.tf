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
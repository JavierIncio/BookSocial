resource "google_sql_database_instance" "postgres" {
  name             = "booksocial-db"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = "db-f1-micro" 
    disk_size         = 20
    disk_type         = "PD_SSD"
    availability_type = "ZONAL"

    ip_configuration {
      authorized_networks {
        name  = "public-access"
        value = "0.0.0.0/0"
      }
      # Para pruebas de aprendizaje. NO usar en producción.
    }

    database_flags {
      name  = "cloudsql.iam_authentication"
      value = "off"
    }
  }

  deletion_protection = false
}

resource "google_sql_database" "booksocial" {
  name     = "booksocial"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "booksocial_user" {
  name       = "booksocial"
  instance   = google_sql_database_instance.postgres.name
  password   = var.db_password
}

resource "google_artifact_registry_repository" "apps" {
  location     = var.region
  repository_id = "apps"
  description   = "Imágenes de los servicios de BookSocial"
  format        = "DOCKER"
}

# ---------- Cloud Run: identity (con Redis sidecar) ----------
resource "google_cloud_run_v2_service" "identity" {
  name     = "identity"
  location = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/identity:latest"

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${var.db_host}:5432/booksocial"
      }
      env {
        name  = "SPRING_REDIS_HOST"
        value = "localhost"
      }
      env {
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "SPRING_DATASOURCE_PASSWORD"
        value = var.db_password
      }
      env {
        name  = "FRONTEND_URL"
        value = var.frontend_url
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }

    containers {
      image = "redis:7-alpine"
      command = ["redis-server"]
    }
  }
}

# ---------- Cloud Run: gateway ----------
resource "google_cloud_run_v2_service" "gateway" {
  name     = "gateway"
  location = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/gateway:latest"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env {
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "IDENTITY_SERVICE_URI"
        value = google_cloud_run_v2_service.identity.uri
      }
      env {
        name  = "USER_SERVICE_URI"
        value = "http://user-service:8082"
      }
      env {
        name  = "BOOK_SERVICE_URI"
        value = "http://book-service:8083"
      }
      # review/shelf/social/notification mantienen las URIs por defecto
    }
  }
}

# ---------- Acceso público (sin login) ----------
resource "google_cloud_run_v2_service_iam_member" "public" {
  for_each = {
    identity = google_cloud_run_v2_service.identity.name
    gateway  = google_cloud_run_v2_service.gateway.name
  }
  project  = var.project_id
  location = var.region
  name     = each.value
  role     = "roles/run.invoker"
  member   = "allUsers"
}
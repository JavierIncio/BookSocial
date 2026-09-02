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
  name     = "booksocial"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}

locals {
  rabbitmq       = regex("^amqps?://([^:]+):([^@]+)@([^/:]+)(?::([0-9]+))?(?:/([^/]*))?$", var.rabbitmq_uri)
  rabbitmq_tls   = startswith(var.rabbitmq_uri, "amqps://")
  rabbitmq_port  = coalesce(local.rabbitmq[3], local.rabbitmq_tls ? "5671" : "5672")
  rabbitmq_vhost = coalesce(local.rabbitmq[4], "/")
}

resource "google_artifact_registry_repository" "apps" {
  location      = var.region
  repository_id = "apps"
  description   = "Imágenes de los servicios de BookSocial"
  format        = "DOCKER"
}

# ---------- Cloud Run: identity (con Redis sidecar) ----------
resource "google_cloud_run_v2_service" "identity" {
  name                = "identity"
  location            = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/identity@sha256:87a1832a2a3d58ae92b56eaf8692fe5c983d547f6e01d45d01bc81aa2d529ff0"

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
      env {
        name  = "GOOGLE_CLIENT_ID"
        value = var.google_client_id
      }
      env {
        name  = "GOOGLE_CLIENT_SECRET"
        value = var.google_client_secret
      }
      env {
        name  = "OAUTH_FRONTEND_REDIRECT_URI"
        value = "${google_cloud_run_v2_service.frontend.uri}/en/oauth2/callback"
      }
      env {
        name  = "OAUTH_REDIRECT_URI"
        value = "${var.identity_uri}/login/oauth2/code/google"
      }
    }

    containers {
      image   = "redis:7-alpine"
      command = ["redis-server"]
    }
  }
}

# ---------- Cloud Run: gateway ----------
resource "google_cloud_run_v2_service" "gateway" {
  name                = "gateway"
  location            = var.region
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
        name  = "BOOK_SERVICE_URI"
        value = google_cloud_run_v2_service.book.uri
      }
      env {
        name  = "SOCIAL_SERVICE_URI"
        value = google_cloud_run_v2_service.social.uri
      }
      env {
        name  = "NOTIFICATION_SERVICE_URI"
        value = google_cloud_run_v2_service.notification.uri
      }
      # user/review/shelf: fuera de Cloud Run (limite de conexiones db-f1-micro)
    }
  }
}

# ---------- Cloud Run: book-service ----------
resource "google_cloud_run_v2_service" "book" {
  name                = "book-service"
  location            = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/book:latest"

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
        name  = "SPRING_DATASOURCE_PASSWORD"
        value = var.db_password
      }
      env {
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "SPRING_MONGODB_URI"
        value = var.mongo_uri
      }
      env {
        name  = "SPRING_RABBITMQ_HOST"
        value = local.rabbitmq[2]
      }
      env {
        name  = "SPRING_RABBITMQ_PORT"
        value = local.rabbitmq_port
      }
      env {
        name  = "SPRING_RABBITMQ_USERNAME"
        value = local.rabbitmq[0]
      }
      env {
        name  = "SPRING_RABBITMQ_PASSWORD"
        value = local.rabbitmq[1]
      }
      env {
        name  = "SPRING_RABBITMQ_VIRTUAL_HOST"
        value = local.rabbitmq_vhost
      }
      env {
        name  = "SPRING_RABBITMQ_SSL_ENABLED"
        value = tostring(local.rabbitmq_tls)
      }
      env {
        name  = "GOOGLE_BOOKS_API_KEY"
        value = var.google_books_api_key
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }
  }
}

# ---------- Cloud Run: social-service (solo Mongo + Rabbit, sin Postgres) ----------
resource "google_cloud_run_v2_service" "social" {
  name                = "social-service"
  location            = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/social:latest"

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
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "SPRING_MONGODB_URI"
        value = var.mongo_uri
      }
      env {
        name  = "SPRING_RABBITMQ_HOST"
        value = local.rabbitmq[2]
      }
      env {
        name  = "SPRING_RABBITMQ_PORT"
        value = local.rabbitmq_port
      }
      env {
        name  = "SPRING_RABBITMQ_USERNAME"
        value = local.rabbitmq[0]
      }
      env {
        name  = "SPRING_RABBITMQ_PASSWORD"
        value = local.rabbitmq[1]
      }
      env {
        name  = "SPRING_RABBITMQ_VIRTUAL_HOST"
        value = local.rabbitmq_vhost
      }
      env {
        name  = "SPRING_RABBITMQ_SSL_ENABLED"
        value = tostring(local.rabbitmq_tls)
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }
  }
}

# ---------- Cloud Run: notification-service (solo Mongo + Rabbit, sin Postgres) ----------
resource "google_cloud_run_v2_service" "notification" {
  name                = "notification-service"
  location            = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/notification:latest"

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
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "SPRING_MONGODB_URI"
        value = var.mongo_uri
      }
      env {
        name  = "SPRING_RABBITMQ_HOST"
        value = local.rabbitmq[2]
      }
      env {
        name  = "SPRING_RABBITMQ_PORT"
        value = local.rabbitmq_port
      }
      env {
        name  = "SPRING_RABBITMQ_USERNAME"
        value = local.rabbitmq[0]
      }
      env {
        name  = "SPRING_RABBITMQ_PASSWORD"
        value = local.rabbitmq[1]
      }
      env {
        name  = "SPRING_RABBITMQ_VIRTUAL_HOST"
        value = local.rabbitmq_vhost
      }
      env {
        name  = "SPRING_RABBITMQ_SSL_ENABLED"
        value = tostring(local.rabbitmq_tls)
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }
  }
}

# ---------- Cloud Run: frontend (nginx SPA) ----------
resource "google_cloud_run_v2_service" "frontend" {
  name                = "frontend"
  location            = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/frontend:latest"

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
        name  = "GATEWAY_URI"
        value = var.gateway_uri
      }
      env {
        name  = "NOTIFICATION_URI"
        value = var.notification_uri
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }
  }
}

# ---------- Acceso público (sin login) ----------
resource "google_cloud_run_v2_service_iam_member" "public" {
  for_each = {
    identity             = google_cloud_run_v2_service.identity.name
    gateway              = google_cloud_run_v2_service.gateway.name
    book-service         = google_cloud_run_v2_service.book.name
    social-service       = google_cloud_run_v2_service.social.name
    notification-service = google_cloud_run_v2_service.notification.name
    frontend             = google_cloud_run_v2_service.frontend.name
  }
  project  = var.project_id
  location = var.region
  name     = each.value
  role     = "roles/run.invoker"
  member   = "allUsers"
}
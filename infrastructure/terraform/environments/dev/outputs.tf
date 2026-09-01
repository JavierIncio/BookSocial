output "project_id" {
  value = var.project_id
}

output "gateway_url" {
  value = google_cloud_run_v2_service.gateway.uri
}

output "identity_url" {
  value = google_cloud_run_v2_service.identity.uri
}
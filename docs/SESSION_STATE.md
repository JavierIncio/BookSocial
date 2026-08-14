# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial** (Fase 1 cerrada). El stack de la Fase 1 está funcional y verificado: identity-service + gateway contenerizados con Docker Compose, frontend Angular 21 en el host, y CI con jobs `build` (backend) + `frontend`. La siguiente fase a planificar es la 2 (definir con el usuario: probablemente user-service o book-service siguiendo el patrón de CQRS: PostgreSQL command side + MongoDB query side + eventos RabbitMQ).

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía paso a paso y verifica; el flujo de trabajo se mantiene con todos.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits de cierre en `main`: Fase 1.6 = `e91fb53`; Fase 1.7 = `43345eb`; re-trigger CI = `5cfdb00`. CI verde (jobs `build` + `frontend`).
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper.
- Stack Docker Compose (`infrastructure/docker-compose.yml`): postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity-service (:8081), gateway (:8080) — 5 contenedores healthy con healthchecks `/actuator/health`.
- Secretos en `.env` por módulo, cargados con `spring.config.import: optional:file:.env[.properties]`; `.gitignore` y `.dockerignore` los excluyen; en CI se inyectan como secrets (`APP_JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`).
- OAuth2 Google: redirect `http://localhost:8081/login/oauth2/code/google`; el frontend llama `googleAuthUrl` directo a `:8081` (puerto publicado en compose), no vía gateway — el 401 de `/oauth2/authorization/google` vía gateway es esperado.
- Usuarios de prueba en Postgres: `e2e.final@test.com/Test123456`, `admin@booksocial.com/admin12345`.
- En PowerShell 5.1: `rg` no disponible; JSON inline en `curl.exe` se corrompe (401 falso) — usar `Invoke-RestMethod` con `ConvertTo-Json` o `curl --data "@archivo"`; `gh` no instalado (verificar Actions en navegador).
- Backend de la Fase 1: identidad con roles ADMIN/MODERATOR/USER/MINOR_USER (edad desde `birth_date`), JWT access 15min + refresh en cookie httpOnly con rotación/revocación (hash SHA-256).
- Frontend (convención): estado reactivo con **signals** (`signal` + `asReadonly()`, sin `BehaviorSubject`) e inyección **`inject()`** en servicios, guards y componentes (modelo standalone; sin inyección por constructor).

## Work State

### Completed

- **Refactor frontend (sin commitear)**: `AuthService` migrado de `BehaviorSubject` a signals (`accessToken`/`isAuthenticated` como `asReadonly()`) y de inyección por constructor a `inject(HttpClient)` en `AuthService` y `UserService`; consumidores actualizados a llamada de signal (`auth.accessToken()`, `auth.isAuthenticated()`). Verificado con `ng build` sin errores.
- **Fase 1.7 cerrada** (commit `43345eb`): Dockerfiles multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre` + curl) para identity-service y gateway; `.dockerignore` raíz; compose ampliado con env overrides (`SPRING_DATASOURCE_URL`, `IDENTITY_SERVICE_URI`) y `depends_on: service_healthy`; Actuator `/actuator/health` en permitAll; job `frontend` en `.github/workflows/ci.yml` (Node 24 + npm caché, `npm ci` + `ng build`).
- **E2E verificado**: 5 contenedores healthy; API vía gateway register → `/users/me` → login (cookie refresh con `jti`) → refresh → logout; OAuth2 en navegador; `ng serve` :4200 con proxy a :8080.
- **ROADMAP.md actualizado**: Fase 1 ✅ Completada, sección Fase 1.7 con pasos A–E, errores (OAuth2 401 vía gateway = esperado) y criterios de salida; Fase 2 aún sin abrir.

### Active

- **Commit pendiente del refactor de frontend** (signals + `inject()`): revisar diff, commitear y pushear (convención de cierre de sesión).

### Blocked

- Ninguno.

## Next Move

1. Planificar la **Fase 2** con el usuario (candidatos: user-service con perfil/amistades, o book-service con catálogo; patrón CQRS con Postgres command + Mongo query + eventos RabbitMQ ya disponible en compose).
2. Antes de codificar: actualizar `pom.xml` raíz (módulos), crear el servicio con Spring Initializr, decidir contrato REST vía gateway y añadir su ruta.
3. Mantener convenciones: secretos en `.env` por módulo, healthcheck Actuator, Dockerfile + servicio compose, job CI si aplica, y cerrar cada fase actualizando ROADMAP + commit/push.

## Relevant Files

- `infrastructure/docker-compose.yml` — 5 servicios healthy; identity/gateway con build context `..`, env_file, healthchecks curl.
- `identity-service/Dockerfile`, `gateway/Dockerfile`, `.dockerignore` (raíz) — contenerización de la Fase 1.7.
- `.github/workflows/ci.yml` — jobs `build` + `frontend`, verde.
- `gateway/src/main/resources/application.yaml` — `uri: ${IDENTITY_SERVICE_URI:http://localhost:8081}`; rutas `/auth/**`, `/users/**`; `/actuator/health` permitAll.
- `identity-service/src/main/resources/application.yml` — `url: ${SPRING_DATASOURCE_URL:...}`, OAuth2 Google, `app.jwt.secret` desde env.
- `frontend/` — Angular 21: login, register, oauth2-callback, home, guards, interceptor JWT, `proxy.conf.json` a :8080, `googleAuthUrl` a :8081.
- `docs/ROADMAP.md` — Fase 1 completa; guía de convenciones y errores resueltos (login via PowerShell, OAuth2 redirects, etc.).

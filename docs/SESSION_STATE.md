# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial** en la **Fase 2: `user-service` (perfil + amistades)** con arquitectura CQRS: PostgreSQL como command side y MongoDB como query side. La Fase 1 está cerrada (identity-service + gateway + frontend contenerizados, CI verde). La sub-fase 2.1 (esqueleto del servicio) y 2.2 (perfil con dual-write) están completas y verificadas; falta 2.3 (amistades) y 2.4 (eventos RabbitMQ).

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía paso a paso y verifica; el flujo de trabajo se mantiene con todos.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits de cierre en `main`: Fase 1.7 = `43345eb`; re-trigger CI = `5cfdb00`; SESSION_STATE inicial = `b3a9f28`; refactor frontend = `37af97c`; GUIDE.md Bloque 0-1 = `f48d7a1`; SESSION_STATE = `fb512f8`. CI verde (jobs `build` + `frontend`).
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose (`infrastructure/docker-compose.yml`): postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity-service (:8081), gateway (:8080), user-service (:8082) — 6 contenedores healthy con healthchecks `/actuator/health`.
- Secretos en `.env` por módulo, cargados con `spring.config.import: optional:file:.env[.properties]`; `.gitignore` y `.dockerignore` los excluyen; en CI se inyectan como secrets (`APP_JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`). `user-service/.env` comparte la misma `APP_JWT_SECRET` que identity-service.
- **Spring Boot 4.1**: el prefijo de Mongo es `spring.mongodb.*` (env `SPRING_MONGODB_URI`), NO `spring.data.mongodb.*`. La URI de Mongo necesita `?authSource=admin` (el usuario `booksocial` es root y vive en la DB `admin`).
- Gateway: rutas `/auth/**,/users/**` → identity (:8081), `/profiles/**,/follows/**` → `${USER_SERVICE_URI:http://localhost:8082}`. En compose el gateway define `USER_SERVICE_URI: http://user-service:8082`.
- Seguridad: el gateway aplica strip-then-assert y reenvía `X-User-Id`/`X-User-Email` derivados del JWT; user-service valida el JWT él mismo (mismo `APP_JWT_SECRET`, `issuer: booksocial-identity`) y usa esos headers de confianza.
- OAuth2 Google: redirect `http://localhost:8081/login/oauth2/code/google`; el frontend llama `googleAuthUrl` directo a `:8081`, no vía gateway.
- Usuarios de prueba en Postgres: `e2e.final@test.com/Test123456` (userId 10, perfil creado en E2E), `admin@booksocial.com/admin12345`.
- En PowerShell 5.1: `rg` no disponible; JSON inline en `curl.exe` se corrompe (401 falso) — usar `Invoke-RestMethod` con `ConvertTo-Json` o `curl --data "@archivo"`; `gh` no instalado (verificar Actions en navegador). El login devuelve `accessToken` (camelCase).
- En Windows, un JAR en ejecución bloquea `mvn clean/package` ("Unable to rename ... jar.original"): detener el proceso java o `docker compose down` del servicio antes de reconstruir.
- Frontend (convención): estado reactivo con **signals** (`signal` + `asReadonly()`, sin `BehaviorSubject`) e inyección **`inject()`** en servicios, guards y componentes (modelo standalone; sin inyección por constructor).

## Work State

### Completed

- **Fase 1 cerrada**: identidad (roles ADMIN/MODERATOR/USER/MINOR_USER, JWT access/refresh rotativo en cookie httpOnly), gateway WebMVC con filtro JWT + headers `X-User-*`, frontend Angular 21, contenerización y CI `build`+`frontend`. Refactor frontend a signals + `inject()` commiteado (`37af97c`).
- **Fase 2.1 — Esqueleto user-service**: proyecto Spring Initializr en `user-service/` (webmvc, data-jpa, data-mongodb, security, validation, actuator), `pom.xml` con parent `booksocial-parent` + jjwt, `<module>` añadido al raíz, `application.yaml` (puerto 8082, env import, `spring.mongodb.uri` overridable, `app.jwt.issuer: booksocial-identity`), `.env` con `APP_JWT_SECRET`, seguridad parse-only (JwtService/JwtAuthFilter/RestAuthenticationEntryPoint/SecurityConfig), ruta en gateway, Dockerfile multi-stage, servicio `user-service` en compose (`depends_on` postgres+mongodb `service_healthy`, healthcheck curl, env `SPRING_DATASOURCE_URL` + `SPRING_MONGODB_URI` con `?authSource=admin`). 6/6 contenedores healthy.
- **Fase 2.2 — Perfil CQRS dual-write (cerrada, commit `b1adbdc`)**: `domain/Profile` (JPA, `userId` único), `readmodel/ProfileReadModel` (Mongo, `_id`=userId, contadores followers/following/posts), repositorios JPA+Mongo, `service/ProfileService` (getOrCreate/update con upsert del read model; getByUserId lee solo de Mongo), `web/ProfileController` (`GET/PUT /profiles/me` con `@RequestHeader X-User-Id/X-User-Email`, `GET /profiles/{userId}`), DTOs record + validación, `ProfileNotFoundException` + `GlobalExceptionHandler` (404/400 JSON). E2E vía gateway OK: creación on-demand (userId 10), PUT con dual-write (dato en Postgres y Mongo), lectura desde Mongo, 404 JSON.
- **Fase 2.3 — Amistades (verificada, pendiente de cerrar)**: `domain/Follow` (JPA, unique `(followerId, followeeId)`, self-follow → 400), `readmodel/FollowReadModel` (Mongo, `_id`=`<followerId>:<followeeId>`), repositorios JPA+Mongo, `service/FollowService` (follow/unfollow dual-write + ajuste de contadores en `ProfileReadModel` con `Math.max(0,...)`; followers/following leen solo de Mongo), `web/FollowController` (`POST/DELETE /follows/{targetUserId}` → 201/204, `GET /follows/{userId}/followers|following`), excepciones `SelfFollowException` (400), `AlreadyFollowingException` (409), `NotFollowingException` (404) + handlers en `GlobalExceptionHandler`. E2E vía gateway OK con dos usuarios (10 y 19): 201/409/400, listas, contadores +1/-1, unfollow 204/404, limpieza en Postgres y Mongo. `verify` local OK.
- **Cierre 2.2 commiteado y pusheado**: `b1adbdc` (feat user-service 2.1+2.2) + `c283e9e` (docs GUIDE Bloque 6 + SESSION_STATE). CI verde.

### Active

- **Cierre de la Fase 2.3**: documentar (GUIDE.md 6.4) y commitear/pushear para verificar CI. Después arrancar 2.4.

### Blocked

- Ninguno.

## Next Move

1. Commitear el cierre de 2.3 (docs + código follows) y comprobar CI en verde (Actions en navegador; `gh` no instalado).
2. **Fase 2.4 — Eventos RabbitMQ**: publicar `FollowedEvent`/`UnfollowedEvent` y mover la sincronización de amistades a eventos (sin Outbox; limitación documentada). El perfil permanece en dual-write. Añadir el starter de AMQP al user-service, definir exchange/queue/binding, y al cierre actualizar ROADMAP.md con la Fase 2 completa.
3. Mantener convenciones: secretos en `.env` por módulo, healthcheck Actuator, Dockerfile + servicio compose, `verify` local antes de commit, y cerrar cada fase actualizando ROADMAP + GUIDE + commit/push.

## Relevant Files

- `user-service/` — módulo de la Fase 2: `pom.xml` (booksocial-parent + jjwt), `src/main/resources/application.yaml` (8082, `spring.mongodb.uri`, env overrides), `src/main/java/com/booksocial/user/{domain/Profile.java, readmodel/ProfileReadModel.java, repository/ProfileRepository.java, readmodel/ProfileReadModelRepository.java, service/ProfileService.java, web/ProfileController.java, web/GlobalExceptionHandler.java, web/dto/*, config/SecurityConfig.java, security/JwtService.java|JwtAuthFilter.java|RestAuthenticationEntryPoint.java}`, `Dockerfile`, `.env`.
- `pom.xml` (raíz) — `<modules>` incluye `user-service`.
- `gateway/src/main/resources/application.yaml` — rutas `/auth/**,/users/**` e `/profiles/**,/follows/**`.
- `infrastructure/docker-compose.yml` — 6 servicios healthy; `user-service` y gateway con `USER_SERVICE_URI`.
- `.github/workflows/ci.yml` — jobs `build` + `frontend`; `clean verify` del parent (incluye user-service).
- `docs/ROADMAP.md` — documentada hasta Fase 1; Fase 2 pendiente de añadir al cierre.
- `docs/GUIDE.md` — Bloque 6 añadido (Fase 2: esqueleto + CQRS dual-write + errores).
- `frontend/src/app/core/services/auth.service.ts` — signals + `inject()` + `register(RegisterRequest)`.

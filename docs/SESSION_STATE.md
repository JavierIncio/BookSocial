# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. La **Fase 3 (book-service: catálogo CQRS)** está completa y verificada: esqueleto (3.1) y catálogo con búsqueda (3.2). La Fase 2 está cerrada (user-service: perfil + amistades con eventos RabbitMQ) y la Fase 1 también (identity-service + gateway + frontend contenerizados). Falta planificar la Fase 4.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía paso a paso, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits de cierre en `main` (últimos): Fase 2.4 = `afe681e` (+ docs `b633860`); Fase 3.1 = `24764e4`; Fase 3.2 = `d9b532d`. CI verde.
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose (`infrastructure/docker-compose.yml`): postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity-service (:8081), gateway (:8080), user-service (:8082), **book-service (:8083)** — 7 contenedores healthy con healthchecks `/actuator/health`.
- Secretos en `.env` por módulo, cargados con `spring.config.import: optional:file:.env[.properties]`; `.gitignore` y `.dockerignore` los excluyen; en CI se inyectan como secrets. Todos los servicios comparten la misma `APP_JWT_SECRET`.
- **Spring Boot 4.1**: el prefijo de Mongo es `spring.mongodb.*` (env `SPRING_MONGODB_URI`), NO `spring.data.mongodb.*`. La URI de Mongo necesita `?authSource=admin`.
- Gateway: rutas `/auth/**,/users/**` → identity (:8081), `/profiles/**,/follows/**` → `${USER_SERVICE_URI:http://localhost:8082}`, `/books/**` → `${BOOK_SERVICE_URI:http://localhost:8083}`. En compose: `USER_SERVICE_URI: http://user-service:8082`, `BOOK_SERVICE_URI: http://book-service:8083`.
- Seguridad: el gateway aplica strip-then-assert y reenvía `X-User-Id`/`X-User-Email`/`X-User-Roles` (roles unidos con comas) derivados del JWT; los servicios downstream validan el JWT ellos mismos (mismo `APP_JWT_SECRET`, `issuer: booksocial-identity`). El `POST /books` exige rol `ADMIN` vía `X-User-Roles`.
- OAuth2 Google: redirect `http://localhost:8081/login/oauth2/code/google`; el frontend llama `googleAuthUrl` directo a `:8081`, no vía gateway.
- Usuarios de prueba en Postgres: `e2e.final@test.com/Test123456` (userId 10), `follower2@test.com/Test123456` (userId 19), `admin@booksocial.com/admin12345` (roles USER,ADMIN — el login del admin funciona).
- En PowerShell 5.1: `rg` no disponible; JSON inline en `curl.exe` se corrompe (401 falso) — usar `Invoke-RestMethod` con `ConvertTo-Json` o `curl --data "@archivo"`; `gh` no instalado (verificar Actions en navegador). El login devuelve `accessToken` (camelCase).
- En Windows, un JAR en ejecución bloquea `mvn clean/package` ("Unable to rename ... jar.original"): detener el proceso java o `docker compose down` del servicio antes de reconstruir.
- Frontend (convención): estado reactivo con **signals** (`signal` + `asReadonly()`, sin `BehaviorSubject`) e inyección **`inject()`** (modelo standalone; sin inyección por constructor).
- Spring AMQP 4.x: el converter JSON es **`JacksonJsonMessageConverter`** (el `Jackson2JsonMessageConverter` está deprecado), con trusted packages por varargs.

## Work State

### Completed

- **Fase 1 cerrada**: identidad (roles, JWT access/refresh en cookie httpOnly), gateway WebMVC con filtro JWT + headers `X-User-*`, frontend Angular 21 (signals + `inject()`), contenerización y CI `build`+`frontend`.
- **Fase 2 cerrada (user-service)**: esqueleto (2.1), perfil CQRS dual-write (2.2), amistades dual-write (2.3) y sincronización de amistades por eventos RabbitMQ (2.4). Commits `b1adbdc`+`c283e9e` (2.2), `403539b`+`56e5ea3` (2.3), `afe681e`+`b633860` (2.4). CI verde.
- **Fase 3.1 — Esqueleto book-service (cerrada, commit `24764e4`)**: proyecto Spring Initializr en `book-service/` (webmvc, data-jpa, data-mongodb, security, validation, actuator), parent `booksocial-parent` + jjwt, `<module>` en POM raíz, `application.yaml` (puerto 8083, `spring.mongodb.uri`, `app.jwt.issuer: booksocial-identity`), `.env`, seguridad parse-only copiada de user-service, ruta `/books/**` en gateway (`BOOK_SERVICE_URI`), Dockerfile y servicio compose con healthcheck. **Error resuelto**: faltaba el driver `org.postgresql:postgresql` (runtime) → `ClassNotFoundException: org.postgresql.Driver`. 7/7 contenedores healthy; `GET /books/{isbn}` vía gateway → 401 JSON.
- **Fase 3.2 — Catálogo CQRS con búsqueda (cerrada, commit `d9b532d`)**: `domain/Book` (JPA, isbn único), `readmodel/BookReadModel` (Mongo, `_id`=isbn), repositorios JPA+Mongo (búsqueda derivada `findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase`), `service/BookService` (create dual-write; findByIsbn/search leen solo de Mongo y devuelven `BookResponse`), `web/BookController` (`POST /books` exige `ADMIN` en `X-User-Roles` → 201/403; `GET /books/{isbn}` → 200/404; `GET /books/search?q=` → 200), excepciones `BookNotFoundException`/`BookAlreadyExistsException`/`ForbiddenException` + `GlobalExceptionHandler` (400/403/404/409), `config/BookDataSeeder` (8 libros si tabla vacía, dual-write). **Bugs corregidos en revisión**: condición de rol invertida (`!isAdmin`), mapping `@GetMapping("/search?q=")` mal, servicio devolviendo `BookReadModel` en vez de `BookResponse`. E2E vía gateway OK: 403 (USER), 201 (ADMIN), 409 (duplicado), 400 (validación), 200/404 por ISBN (Mongo), búsqueda case-insensitive (q=Martin→3, q=patterns→2), 9 libros en Postgres y Mongo. `verify` local OK.

### Active

- **Cierre de la Fase 3 completa**: documentar (GUIDE Bloque 7, ROADMAP Fase 3, SESSION_STATE) y commitear/pushear para verificar CI. Después planificar la Fase 4.

### Blocked

- Ninguno.

## Next Move

1. Commitear el cierre de la Fase 3 (docs) y comprobar CI en verde (Actions en navegador; `gh` no instalado).
2. Planificar la **Fase 4** con el usuario (candidatos: `review-service` con eventos RabbitMQ cruzados sobre `BookCreatedEvent`, `shelf-service` (estanterías de libros del usuario) o integrar el catálogo en el frontend Angular). Mantener convenciones: `.env` por módulo, healthcheck Actuator, Dockerfile + servicio compose, `verify` local antes de commit, y cerrar cada fase actualizando ROADMAP + GUIDE + commit/push.

## Relevant Files

- `book-service/` — módulo de la Fase 3: `pom.xml`, `src/main/resources/application.yaml` (8083, `spring.mongodb.uri`, env overrides), `src/main/java/com/booksocial/book/{domain/Book.java|BookAlreadyExistsException.java|BookNotFoundException.java|ForbiddenException.java, readmodel/BookReadModel.java|BookReadModelRepository.java, repository/BookRepository.java, service/BookService.java, web/BookController.java|GlobalExceptionHandler.java, web/dto/*, config/BookDataSeeder.java|SecurityConfig.java, security/JwtService.java|JwtAuthFilter.java|RestAuthenticationEntryPoint.java}`, `Dockerfile`, `.env`.
- `pom.xml` (raíz) — `<modules>` incluye `gateway`, `identity-service`, `user-service`, `book-service`.
- `gateway/src/main/resources/application.yaml` — rutas `/auth/**,/users/**`, `/profiles/**,/follows/**` y `/books/**`.
- `infrastructure/docker-compose.yml` — 7 servicios healthy; `USER_SERVICE_URI` y `BOOK_SERVICE_URI` en el gateway.
- `.github/workflows/ci.yml` — jobs `build` + `frontend`; `clean verify` del parent (incluye todos los módulos).
- `docs/ROADMAP.md` — Fases 1-3 documentadas; Fase 4 pendiente de añadir al cierre.
- `docs/GUIDE.md` — Bloques 0-7 (Bloque 7 = book-service: esqueleto + catálogo CQRS + decisiones).
- `frontend/src/app/core/services/auth.service.ts` — signals + `inject()` + `register(RegisterRequest)`.

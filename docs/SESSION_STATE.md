# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. Las **Fases 1-4 están completadas** (identity + gateway + frontend + user-service + book-service + review-service). La Fase 4 cerró con reseñas CQRS y el primer evento cruzado (BookCreatedEvent book→review). Falta planificar la Fase 5.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía paso a paso, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits recientes: Fase 3.2 = `d9b532d`; Fase 3 docs = `7626c75`; CI fix = `18c04ec`; 4.1 = `54eb127`; 4.2 = `d2e0349`; 4.3 = `0cbddd9`.
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose: postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity (:8081), gateway (:8080), user-service (:8082), book-service (:8083), review-service (:8084) — 8 contenedores healthy.
- Secretos en `.env` por módulo; todos comparten `APP_JWT_SECRET`.
- **Spring Boot 4.1**: Mongo prefix `spring.mongodb.*` (env `SPRING_MONGODB_URI`), `?authSource=admin`.
- Gateway rutas: `/auth/**,/users/**` → identity, `/profiles/**,/follows/**` → user-service, `/books/**` → book-service, `/reviews/**` → review-service. Headers: `X-User-Id`, `X-User-Email`, `X-User-Roles` (roles unidos con comas).
- `POST /books` exige `ADMIN` vía `X-User-Roles`. `POST /reviews/{isbn}` exige que el libro exista en el catálogo local (event-driven).
- Usuarios: `e2e.final@test.com/Test123456` (userId 10), `follower2@test.com/Test123456` (userId 19), `admin@booksocial.com/admin12345` (roles USER,ADMIN).
- Spring AMQP 4.x: `JacksonJsonMessageConverter` (no `Jackson2JsonMessageConverter` deprecated), trusted packages por varargs.
- Eventos: exchange topic `booksocial.events`, routing keys `follow.followed`, `follow.unfollowed`, `book.created`. Colas durables declaradas por cada consumidor.
- En Windows: JAR bloquea `mvn clean/package` (detener proceso java); PowerShell 5.1 `ErrorDetails.Message` vacío (usar `curl.exe -i`).
- CI: GitHub Actions, servicios Postgres (no RabbitMQ/Mongo); context tests pasan porque Spring AMQP/MongoDB son lazy.
- Frontend (convención): signals + `inject()` (standalone, sin constructor).

## Work State

### Completed

- **Fase 1 cerrada**: identity-service (roles, JWT), gateway (JWT filter + headers), frontend Angular 21, contenerización y CI.
- **Fase 2 cerrada (user-service)**: perfil CQRS dual-write, amistades CQRS dual-write, eventos RabbitMQ (follow/unfollow). Commits `b1adbdc`–`b633860`.
- **Fase 3 cerrada (book-service)**: catálogo CQRS con búsqueda, seeder, control de roles ADMIN. Commits `24764e4`–`7626c75`, CI fix `18c04ec`.
- **Fase 4 cerrada (review-service)**: reseñas CQRS con stats agregadas y el primer evento cruzado (`BookCreatedEvent`). Commits `54eb127`–`0cbddd9`.
  - **4.1**: esqueleto review-service (8084), ruta `/reviews/**` en gateway, compose con rabbitmq.
  - **4.2**: book-service publica `BookCreatedEvent` (publisher + seeder); review-service consume → `book_refs` en Mongo. Reset/re-seed verificó 8→9 book_refs.
  - **4.3**: `Review` (JPA, unique isbn+userId), `ReviewReadModel` (Mongo, `_id`=isbn:userId), `ReviewStatsReadModel` (ratingCount, averageRating). Endpoints POST/PUT/GET. Errores corregidos: `JpaRepository<Review,Long>` (no Integer), getters/setters en read models, constructor desde Review, `toResponse` sobrecargado, userId `Long` en repos.

### Active

- **Cierre de la Fase 4 completa**: documentar (GUIDE Bloque 8, ROADMAP Fase 4, SESSION_STATE) y commitear/pushear. Después planificar la Fase 5.

### Blocked

- Ninguno.

## Next Move

1. Commitear el cierre de la Fase 4 (docs) y comprobar CI en verde.
2. Planificar la **Fase 5** con el usuario. Candidatos:
   - `shelf-service` (estanterías: leído/leyendo/quiero leer) con eventos cruzados (`BookCreatedEvent` + posibles eventos de review).
   - Integrar catálogo + reseñas en el **frontend Angular** (búsqueda, detalle de libro con reviews, crear review).
   - `notification-service` con eventos de follow y review.
3. Mantener convenciones: `.env` por módulo, healthcheck Actuator, Dockerfile + compose, `verify` local, cerrar cada fase actualizando ROADMAP + GUIDE + commit/push.

## Relevant Files

- `review-service/` — Fase 4: `pom.xml`, `application.yaml` (8084, mongodb, rabbitmq), `config/{RabbitConfig,SecurityConfig}.java`, `domain/{Review,BookNotInCatalogException,ReviewAlreadyExistsException,ReviewNotFoundException}.java`, `readmodel/{BookRefReadModel,ReviewReadModel,ReviewStatsReadModel}.java` + repos, `repository/ReviewRepository.java`, `events/{BookCreatedEvent,BookCreatedEventConsumer}.java`, `service/ReviewService.java`, `web/{ReviewController,GlobalExceptionHandler}.java`, `web/dto/*`.
- `book-service/` — añadido en 4.2: `config/RabbitConfig.java` (exchange + converter), `events/{BookCreatedEvent,BookEventPublisher}.java`; `BookService.create` y `BookDataSeeder` publican eventos.
- `infrastructure/docker-compose.yml` — 8 servicios healthy; `SPRING_RABBITMQ_HOST` en book-service, user-service y review-service.
- `gateway/src/main/resources/application.yaml` — 4 rutas: identity, user-service, book-service, review-service.
- `pom.xml` (raíz) — módulos: gateway, identity-service, user-service, book-service, review-service.
- `docs/GUIDE.md` — Bloques 0-8 (Bloque 8 = review-service: esqueleto + evento cruzado + reseñas CQRS).
- `docs/ROADMAP.md` — Fases 1-4 documentadas; Fase 5 pendiente.

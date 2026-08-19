# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. Las **Fases 1-5 están completadas** (identity + gateway + frontend auth + user-service + book-service + review-service + shelf-service). CI verde. La **Fase 6 será integrar catálogo, reseñas y estanterías en el frontend Angular**.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits: Fase 5 = `6f23d48`, docs Fase 5 = `4a97f66`, fix frontend zoneless = `1ca3280`.
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose: postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity (:8081), gateway (:8080), user-service (:8082), book-service (:8083), review-service (:8084), shelf-service (:8085) — 9 contenedores.
- Secretos en `.env` por módulo; todos comparten `APP_JWT_SECRET`.
- **Spring Boot 4.1**: Mongo prefix `spring.mongodb.*` (env `SPRING_MONGODB_URI`), `?authSource=admin`.
- Gateway rutas: `/auth/**,/users/**` → identity, `/profiles/**,/follows/**` → user-service, `/books/**` → book-service, `/reviews/**` → review-service, `/shelves/**` → shelf-service. Headers: `X-User-Id`, `X-User-Email`, `X-User-Roles`.
- `POST /books` exige `ADMIN`. `POST /reviews/{isbn}` y `POST /shelves` exigen catálogo local (event-driven).
- Usuarios: `e2e.final@test.com/Test123456` (userId 10), `follower2@test.com/Test123456` (userId 19), `admin@booksocial.com/admin12345` (roles USER,ADMIN).
- Spring AMQP 4.x: `JacksonJsonMessageConverter`, trusted packages por varargs.
- Eventos: exchange `booksocial.events`, keys `follow.followed`, `follow.unfollowed`, `book.created`. Colas durables declaradas por cada consumidor.
- CI: GitHub Actions, Postgres service (no RabbitMQ/Mongo). Context tests pasan porque AMQP/MongoDB son lazy.
- Frontend (convención): signals + `inject()` (standalone, sin constructor).
- **Angular 21 zoneless**: NO usa zone.js. Todos los componentes deben usar **signals** para estado reactivo (`signal()`, `.set()`, `.asReadonly()`). Las propiedades normales mutadas en `.subscribe()` NO disparan change detection. Templates usan `signal()` como funciones: `@if (loading())`, `{{ user()?.name }}`.

## Resumen de APIs disponibles (vía gateway :8080)

### Auth (identity-service)
- `POST /auth/register` — `{ email, password, firstName, lastName, birthDate }`
- `POST /auth/login` — `{ email, password }` → `{ accessToken }`
- `POST /auth/refresh` — rota refresh token
- `POST /auth/logout` — revoca refresh token
- `GET /users/me` — usuario autenticado

### Books (book-service)
- `GET /books/search?q=` — búsqueda por título/autor
- `GET /books/{isbn}` — detalle por ISBN
- `POST /books` — crear libro (solo ADMIN)

### Reviews (review-service)
- `GET /reviews/books/{isbn}` — lista de reseñas de un libro
- `GET /reviews/books/{isbn}/summary` — rating medio + count
- `POST /reviews/{isbn}` — crear reseña `{ rating, comment }`
- `PUT /reviews/{isbn}` — actualizar reseña

### Shelves (shelf-service)
- `GET /shelves` — estantería del usuario actual
- `POST /shelves` — añadir libro `{ bookIsbn, status }` (WANTS_TO_READ/READING/READ)
- `PUT /shelves/{isbn}` — cambiar status
- `DELETE /shelves/{isbn}` — eliminar de estantería

### Profiles (user-service)
- `GET /profiles/me` — perfil del usuario actual
- `PUT /profiles/me` — actualizar perfil
- `GET /profiles/{userId}` — perfil público

### Follows (user-service)
- `POST /follows/{userId}` — seguir
- `DELETE /follows/{userId}` — dejar de seguir
- `GET /follows/{userId}/followers` — seguidores
- `GET /follows/{userId}/following` — siguiendo

## Work State

### Completed

- **Fase 1 cerrada**: identity-service (roles, JWT), gateway (JWT filter + headers), frontend Angular 21 con login/registro/OAuth2, contenerización y CI.
- **Fase 2 cerrada (user-service)**: perfil CQRS dual-write, amistades CQRS con eventos RabbitMQ (follow/unfollow).
- **Fase 3 cerrada (book-service)**: catálogo CQRS con búsqueda, seeder 8 libros, control de roles ADMIN, publicación de BookCreatedEvent.
- **Fase 4 cerrada (review-service)**: reseñas CQRS con stats agregadas, consumer de BookCreatedEvent (book_refs en Mongo).
- **Fase 5 cerrada (shelf-service)**: estanterías CQRS (WANTS_TO_READ/READING/READ), consumer de BookCreatedEvent (book_refs en Mongo).
- **Fix frontend zoneless**: home.ts, login.ts, register.ts migrados de propiedades normales a `signal()` para compatibilidad con Angular 21 zoneless. Build OK.

### Active

- **Fase 6: Frontend Angular — integrar catálogo, reseñas y estanterías.**

### Blocked

- Ninguno.

## Next Move

Empezar la **Fase 6 — Frontend Angular**. Planificación sugerida:

| Paso | Descripción |
|------|-------------|
| 6.1 | Servicios Angular: `BookService`, `ReviewService`, `ShelfService` con llamadas REST al gateway |
| 6.2 | Página de **catálogo** — grid de libros, búsqueda, enlace a detalle |
| 6.3 | Página de **detalle de libro** — info + reseñas + botón "añadir a estantería" |
| 6.4 | Página de **mi estantería** — filtro por status (WANTS_TO_READ / READING / READ) |
| 6.5 | **Crear/editar reseña** — formulario con rating + comentario |

## Relevant Files

### Backend
- `shelf-service/` — Fase 5: domain (Shelf, ShelfStatus, exceptions), readmodel (ShelfReadModel, BookRefReadModel + repos), events (BookCreatedEvent, BookCreatedEventConsumer), service, controller, DTOs, RabbitConfig.
- `review-service/` — Fase 4: domain (Review + exceptions), readmodel (ReviewReadModel, ReviewStatsReadModel, BookRefReadModel), events (BookCreatedEvent, BookCreatedEventConsumer), service, controller, DTOs, RabbitConfig.
- `book-service/` — Fase 3 + 4.2: catálogo CQRS, seeder, RabbitConfig, BookCreatedEvent + BookEventPublisher.
- `user-service/` — Fase 2: perfil CQRS (ProfileService), amistades event-driven (FollowService + FollowEventConsumer).
- `identity-service/` — Fase 1: registro, JWT, OAuth2 Google.
- `gateway/` — Fase 1: JWT filter, 5 rutas, headers strip-then-assert.
- `infrastructure/docker-compose.yml` — 9 servicios.

### Frontend (pendiente Fase 6)
- `frontend/` — Angular 21.2.19, login/registro/OAuth2/guardas/interceptor JWT + home con profile.
- `features/home/` — signals para user/loading/error, llama a `GET /users/me`.
- `features/auth/login/` — signals para errorMessage/loading, Reactive Forms.
- `features/auth/register/` — signals para errorMessage/loading, Reactive Forms.
- Convenciones: **signals** + `inject()` (standalone, sin constructor). Obligatorio en zoneless.

### Docs
- `docs/GUIDE.md` — Bloques 0-8.
- `docs/ROADMAP.md` — Fases 1-5 documentadas; Fase 6 pendiente.
- `docs/SESSION_STATE.md` — Este archivo.

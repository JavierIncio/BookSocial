# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. Las **Fases 1-6 están completadas** (identity + gateway + frontend auth + user-service + book-service + review-service + shelf-service + backend integración Google Books + APIs públicas). CI verde. La **Fase 7 será integrar catálogo, reseñas y estanterías en el frontend Angular**.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits: Fase 6.0 = `249de08`, Fase 6.1-6.2 = `fb57d02`.
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
- **Google Books API**: `https://www.googleapis.com/books/v1/`, API key en `book-service/.env` (`GOOGLE_BOOKS_API_KEY`). Solo se guardan libros con ISBN válido.

## Resumen de APIs disponibles (vía gateway :8080)

### Auth (identity-service)
- `POST /auth/register` — `{ email, password, firstName, lastName, birthDate }`
- `POST /auth/login` — `{ email, password }` → `{ accessToken }`
- `POST /auth/refresh` — rota refresh token
- `POST /auth/logout` — revoca refresh token
- `GET /users/me` — usuario autenticado

### Books (book-service) — GETs públicos sin auth
- `GET /books/search?q=` — búsqueda por título/autor (solo BD)
- `GET /books/search/full?q=` — búsqueda BD + Google Books API
- `GET /books/{isbn}` — detalle por ISBN (auto-importa desde Google si no existe en BD)
- `POST /books` — crear libro (solo ADMIN)

### Reviews (review-service) — auth requerida
- `GET /reviews/books/{isbn}` — lista de reseñas de un libro
- `GET /reviews/books/{isbn}/summary` — rating medio + count
- `GET /reviews/me` — reseñas del usuario actual (X-User-Id)
- `GET /reviews/users/{userId}` — reseñas de un usuario
- `POST /reviews/{isbn}` — crear reseña `{ rating, comment }`
- `PUT /reviews/{isbn}` — actualizar reseña

### Shelves (shelf-service) — GETs públicos sin auth
- `GET /shelves` — estantería del usuario actual (auth)
- `GET /shelves/{isbn}` — todos los usuarios con este libro (público)
- `GET /shelves/users/{userId}` — estanterías de un usuario (público)
- `POST /shelves` — añadir libro `{ bookIsbn, status }` (auth)
- `PUT /shelves/{isbn}` — cambiar status (auth)
- `DELETE /shelves/{isbn}` — eliminar de estantería (auth)

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
- **Fase 6 cerrada (backend integración + APIs públicas)**: Google Books API en book-service (search full + auto-import ISBN), endpoints de usuario en review-service, endpoints públicos en shelf-service. Gateway SecurityConfig actualizado.

### Active

- **Fase 7: Frontend Angular — integrar catálogo, reseñas y estanterías.**

### Blocked

- Ninguno.

## Next Move

Empezar la **Fase 7 — Frontend Angular**. Planificación sugerida:

| Paso | Descripción |
|------|-------------|
| 7.1 | Servicios Angular: `BookService`, `ReviewService`, `ShelfService` con llamadas REST al gateway |
| 7.2 | Página de **catálogo** — grid de libros, búsqueda, enlace a detalle |
| 7.3 | Página de **detalle de libro** — info + reseñas + botón "añadir a estantería" |
| 7.4 | Página de **mi estantería** — filtro por status (WANTS_TO_READ / READING / READ) |
| 7.5 | **Crear/editar reseña** — formulario con rating + comentario |

## Relevant Files

### Backend
- `shelf-service/` — Fase 5 + 6.3: domain, readmodel (ShelfReadModel, BookRefReadModel + repos), events, service, controller (con endpoints públicos), DTOs, RabbitConfig.
- `review-service/` — Fase 4 + 6.2: domain, readmodel (ReviewReadModel, ReviewStatsReadModel, BookRefReadModel), events, service, controller (con endpoints de usuario), DTOs, RabbitConfig.
- `book-service/` — Fase 3 + 6.1: catálogo CQRS, seeder, RabbitConfig, BookCreatedEvent + BookEventPublisher, Google Books integration (GoogleBooksClient, GoogleBooksMapper, GoogleBooksResponse, GoogleBooksProperties).
- `user-service/` — Fase 2: perfil CQRS (ProfileService), amistades event-driven (FollowService + FollowEventConsumer).
- `identity-service/` — Fase 1: registro, JWT, OAuth2 Google.
- `gateway/` — Fase 1 + 6.1 + 6.3: JWT filter, 5 rutas, headers strip-then-assert, SecurityConfig con GETs públicos para books y shelves.
- `infrastructure/docker-compose.yml` — 9 servicios.

### Frontend (pendiente Fase 7)
- `frontend/` — Angular 21.2.19, login/registro/OAuth2/guardas/interceptor JWT + home con profile.
- `features/home/` — signals para user/loading/error, llama a `GET /users/me`.
- `features/auth/login/` — signals para errorMessage/loading, Reactive Forms.
- `features/auth/register/` — signals para errorMessage/loading, Reactive Forms.
- Convenciones: **signals** + `inject()` (standalone, sin constructor). Obligatorio en zoneless.

### Docs
- `docs/GUIDE.md` — Bloques 0-8.
- `docs/ROADMAP.md` — Fases 1-6 documentadas; Fase 7 pendiente.
- `docs/SESSION_STATE.md` — Este archivo.

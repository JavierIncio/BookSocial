# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. Las **Fases 1-8 están completadas** (identity + gateway + frontend auth + user-service + book-service + review-service + shelf-service + backend integración Google Books + APIs públicas + Author entity + Open Library + frontend catálogo/reseñas/estanterías). CI verde. El siguiente paso es la **Fase 8.6 (página de autor)** o planificar la **Fase 9**.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits: Fase 6.0 = `249de08`, Fase 6.1-6.2 = `fb57d02`, Fase 8 = `5af355e`..`6f171c6` (8.1 servicios, 9581f9e/f995124 fixes+catálogo, 143a1b0 detalle, ce6d872 retry Google, 4b496b1 estantería+nav, 6f171c6 formulario reseña).
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose: postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity (:8081), gateway (:8080), user-service (:8082), book-service (:8083), review-service (:8084), shelf-service (:8085) — 9 contenedores.
- Secretos en `.env` por módulo; todos comparten `APP_JWT_SECRET`.
- **Spring Boot 4.1**: Mongo prefix `spring.mongodb.*` (env `SPRING_MONGODB_URI`), `?authSource=admin`.
- Gateway rutas: `/auth/**,/users/**` → identity, `/profiles/**,/follows/**` → user-service, `/books/**,/authors/**` → book-service, `/reviews/**` → review-service, `/shelves/**` → shelf-service. Headers: `X-User-Id`, `X-User-Email`, `X-User-Roles`.
- `POST /books` y `POST /authors` exigen `ADMIN`. `POST /reviews/{isbn}` y `POST /shelves` exigen catálogo local (event-driven).
- Usuarios: `e2e.final@test.com/Test123456` (userId 10), `follower2@test.com/Test123456` (userId 19), `admin@booksocial.com/admin12345` (roles USER,ADMIN).
- Spring AMQP 4.x: `JacksonJsonMessageConverter`, trusted packages por varargs.
- Eventos: exchange `booksocial.events`, keys `follow.followed`, `follow.unfollowed`, `book.created`. `BookCreatedEvent`携带 `(bookIsbn, title, authorName, authorId, occurredAt)`. Colas durables declaradas por cada consumidor.
- CI: GitHub Actions, Postgres service (no RabbitMQ/Mongo). Context tests pasan porque AMQP/MongoDB son lazy.
- Frontend (convención): signals + `inject()` (standalone, sin constructor).
- **Angular 21 zoneless**: NO usa zone.js. Todos los componentes deben usar **signals** para estado reactivo (`signal()`, `.set()`, `.asReadonly()`). Las propiedades normales mutadas en `.subscribe()` NO disparan change detection. Templates usan `signal()` como funciones: `@if (loading())`, `{{ user()?.name }}`.
- **Google Books API**: `https://www.googleapis.com/books/v1/`, API key en `book-service/.env` (`GOOGLE_BOOKS_API_KEY`). Solo se guardan libros con ISBN válido.
- **Open Library API** (sin API key): `https://openlibrary.org`, rate limit ~3 req/s con User-Agent. Endpoints: `/search/authors.json?q=`, `/authors/{id}.json`, `/authors/{id}/works.json`. Cache local en Postgres `authors` + Mongo `authors`.
- **Author entity**: `Author` en Postgres (`authors`) + Mongo (`authors`), con `openLibraryId` como clave de cache. Se crea bajo demanda desde Google Books, Open Library o manualmente.
- **`Book.authorId`** (Long FK → `authors.id`): campo `author` (String) eliminado, migrado a `authorId`+`authorName` en todos los servicios.

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

### Authors (book-service) — GETs públicos sin auth
- `GET /authors/search?q=` — buscar autores (cache local + Open Library API)
- `GET /authors/{olId}` — detalle de autor (Open Library)
- `GET /authors/{olId}/works` — obras de un autor (Open Library)
- `POST /authors` — crear autor (solo ADMIN)

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
- **Fase 7 cerrada (Author entity + Open Library)**: Author entity (Postgres + Mongo) con cache, Open Library API integration (búsqueda autores + obras), migración `author` → `authorId`/`authorName` en book-service + downstream services (review + shelf). Gateway `/authors/**` route.
- **Fase 8 cerrada (frontend catálogo/reseñas/estanterías)**: models+services alineados con DTOs backend (8.1), página catálogo con búsqueda BD+Google (8.2), detalle de libro con reseñas y estantería (8.3), mi estantería con filtros + nav compartido standalone (8.4), formulario crear/editar reseña con estrellas (8.5). Fixes durante la fase: proxy del dev server completo, retry Google Books a 3 intentos + `toResponse` null-safe (documentado en GUIDE 7.3-7.4 y Apéndice C de operaciones).

### Active

- Ninguno. Pendiente opcional: **8.6 — página de autor** (bio + obras + enlace a detalle) o planificar **Fase 9**. El usuario planea añadir i18n para traducciones (UI actual en inglés).

### Blocked

- Ninguno.

## Next Move

Opciones tras cerrar la Fase 8:

| Opción | Descripción |
|--------|-------------|
| 8.6 | Página de **autor** — bio + obras (Open Library) + enlace a detalle de libro; requiere DTO `AuthorResponse` en frontend ya disponible |
| 9.x | Planificar **Fase 9** (feed social, notificaciones, despliegue cloud...) |
| i18n | Añadir internacionalización Angular (`@angular/localize` o ngx-translate) sobre la UI en inglés |

## Relevant Files

### Backend
- `shelf-service/` — Fase 5 + 6.3 + 7.2: domain, readmodel (ShelfReadModel, BookRefReadModel con authorName+authorId), events (BookCreatedEvent 4-args), service, controller, DTOs (ShelfResponse con authorName+authorId), RabbitConfig.
- `review-service/` — Fase 4 + 6.2 + 7.2: domain, readmodel (ReviewReadModel, ReviewStatsReadModel, BookRefReadModel con authorName+authorId), events (BookCreatedEvent 4-args), service, controller, DTOs, RabbitConfig.
- `book-service/` — Fase 3 + 6.1 + 7.1: catálogo CQRS con Author entity, Open Library (OpenLibraryClient, OpenLibraryMapper, OpenLibraryResponse, AuthorDetailResponse, WorksResponse, OpenLibraryProperties), Google Books (GoogleBooksClient, GoogleBooksMapper, GoogleBooksResponse), seeder, BookCreatedEvent 4-args, BookEventPublisher, AuthorController, SecurityConfig con /authors/**.
- `user-service/` — Fase 2: perfil CQRS (ProfileService), amistades event-driven (FollowService + FollowEventConsumer).
- `identity-service/` — Fase 1: registro, JWT, OAuth2 Google.
- `gateway/` — Fase 1 + 6.1 + 6.3 + 7.1: JWT filter, 5 rutas (con /authors/** → book-service), headers strip-then-assert, SecurityConfig con GETs públicos para books, authors y shelves.
- `infrastructure/docker-compose.yml` — 9 servicios.

### Frontend (Fase 8 completada)
- `frontend/` — Angular 21.2.19, login/registro/OAuth2/guardas/interceptor JWT + home con profile.
- `features/home/` — signals para user/loading/error, llama a `GET /users/me`.
- `features/auth/login/`, `features/auth/register/` — signals + Reactive Forms.
- `features/catalog/` — 8.2: catálogo público con búsqueda (search local + searchFull Google), grid de tarjetas.
- `features/book-detail/` — 8.3+8.5: detalle `/book/:isbn` público; reseñas y estantería solo autenticado; formulario de reseña con estrellas (create/update).
- `features/my-shelf/` — 8.4: estantería propia `/shelf` con authGuard y filtros por estado (`computed()`).
- `shared/components/nav/` — nav standalone compartido en todas las páginas (brand, Catalog, My shelf/Logout o Log in según sesión).
- `core/models/` + `core/services/` — book/author/review/shelf alineados 1:1 con DTOs backend.
- `proxy.conf.json` — enruta auth, users, profiles, follows, books, authors, reviews, shelves → gateway :8080.
- Convenciones: **signals** + `inject()` (standalone, sin constructor). Obligatorio en zoneless. UI en inglés (i18n planeado).

### Docs
- `docs/GUIDE.md` — Bloques 0-9, Apéndices A-C (C: operación — despliegue, logs, depuración).
- `docs/ROADMAP.md` — Fases 1-8 documentadas; Fase 8.6 opcional pendiente.
- `docs/SESSION_STATE.md` — Este archivo.

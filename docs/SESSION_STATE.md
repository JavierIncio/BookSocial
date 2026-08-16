# BookSocial — Estado de sesión y prompt de continuación

Documento de handoff para retomar el trabajo en cualquier momento. Se actualiza al cierre de cada sesión.

## Objective

Continuar el monorepo **BookSocial**. Las **Fases 1-4 están completadas y el CI está verde** (identity + gateway + frontend + user-service + book-service + review-service). La Fase 4 cerró con reseñas CQRS y el primer evento cruzado (BookCreatedEvent book→review). Falta planificar la Fase 5.

## Important Details

- El usuario escribe todo el código (VS Code); el asistente guía, revisa y verifica.
- Repositorio: monorepo, branch `main`, remoto `https://github.com/JavierIncio/BookSocial.git`.
- Commits recientes: 4.3 = `0cbddd9`, docs = `5e4eaa2`.
- Runtime: Java 21, Spring Boot 4.1.0, Spring Cloud WebMVC 2025.1.2, Angular 21.2.19, Node 24, Maven wrapper, jjwt 0.12.6.
- Stack Docker Compose: postgres:16-alpine, mongodb:8.0, rabbitmq:4-management, identity (:8081), gateway (:8080), user-service (:8082), book-service (:8083), review-service (:8084) — 8 contenedores healthy.
- Secretos en `.env` por módulo; todos comparten `APP_JWT_SECRET`.
- **Spring Boot 4.1**: Mongo prefix `spring.mongodb.*` (env `SPRING_MONGODB_URI`), `?authSource=admin`.
- Gateway: `/auth/**,/users/**` → identity, `/profiles/**,/follows/**` → user-service, `/books/**` → book-service, `/reviews/**` → review-service. Headers: `X-User-Id`, `X-User-Email`, `X-User-Roles`.
- `POST /books` exige `ADMIN`. `POST /reviews/{isbn}` exige catálogo local (event-driven).
- Usuarios: `e2e.final@test.com/Test123456` (userId 10), `follower2@test.com/Test123456` (userId 19), `admin@booksocial.com/admin12345` (roles USER,ADMIN).
- Spring AMQP 4.x: `JacksonJsonMessageConverter`, trusted packages por varargs.
- Eventos: exchange `booksocial.events`, keys `follow.followed`, `follow.unfollowed`, `book.created`. Colas durables declaradas por cada consumidor.
- CI: GitHub Actions, Postgres service (no RabbitMQ/Mongo). Context tests pasan porque AMQP/MongoDB son lazy.
- Frontend (convención): signals + `inject()` (standalone, sin constructor).

## Análisis: dual-write vs. eventos (Fase 4 — decisión de diseño)

| Servicio | Read model | Eventos publicados | Consumidores externos | Dual-write necesario |
|---|---|---|---|---|
| user-service (perfil) | ProfileReadModel | Ninguno | Ninguno | **Sí** — sin consumidores |
| user-service (amistades) | FollowReadModel | FollowedEvent / UnfollowedEvent | user-service (interno) | **No** — ya es event-driven puro |
| book-service | BookReadModel (búsqueda) | BookCreatedEvent | review-service | **Sí para búsqueda interno**; evento para review-service |
| review-service | ReviewReadModel + ReviewStatsReadModel | Ninguno | Ninguno | **Sí** — sin consumidores |

**Conclusión**: el dual-write se mantiene en profile, book-search y review porque no hay consumidores externos para esos read models. Cuando aparezca uno (notificaciones, feed, etc.), se publica el evento entonces y se elimina el dual-write correspondiente. El patrón actual (dual-write interno + eventos para cross-service) es válido y pragmático.

## Work State

### Completed

- **Fase 1 cerrada**: identity-service (roles, JWT), gateway (JWT filter + headers), frontend Angular 21, contenerización y CI.
- **Fase 2 cerrada (user-service)**: perfil CQRS dual-write, amistades CQRS con eventos RabbitMQ (follow/unfollow). Commits `b1adbdc`–`b633860`.
- **Fase 3 cerrada (book-service)**: catálogo CQRS con búsqueda, seeder, control de roles ADMIN. Commits `24764e4`–`7626c75`, CI fix `18c04ec`.
- **Fase 4 cerrada (review-service)**: reseñas CQRS con stats agregadas y primer evento cruzado (BookCreatedEvent book→review). Commits `54eb127`–`0cbddd9`, docs `5e4eaa2`. CI verde.

### Active

- **Fase 5 pendiente de planificar** con el usuario.

### Blocked

- Ninguno.

## Next Move

Planificar la **Fase 5** con el usuario. Candidatos:
- `shelf-service` (estanterías: leído/leyendo/quiero leer) con eventos cruzados.
- Integrar catálogo + reseñas en el **frontend Angular** (búsqueda, detalle de libro con reviews).
- `notification-service` con eventos de follow y review.

## Relevant Files

- `review-service/` — Fase 4: domain (Review + exceptions), readmodel (ReviewReadModel, ReviewStatsReadModel, BookRefReadModel), events (BookCreatedEvent, BookCreatedEventConsumer), service, controller, DTOs, RabbitConfig.
- `book-service/` — añadido en 4.2: RabbitConfig (exchange + converter), BookCreatedEvent, BookEventPublisher; create y seeder publican eventos.
- `user-service/` — perfil dual-write (ProfileService), amistades event-driven (FollowService + FollowEventConsumer).
- `infrastructure/docker-compose.yml` — 8 servicios healthy.
- `gateway/src/main/resources/application.yaml` — 4 rutas.
- `docs/GUIDE.md` — Bloques 0-8.
- `docs/ROADMAP.md` — Fases 1-4 documentadas.

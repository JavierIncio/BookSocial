# BookSocial — Roadmap de implementación

Guía detallada del proceso de desarrollo del proyecto. Se actualiza **al final de cada fase**, documentando los pasos seguidos, los errores encontrados con su **solución directa aplicada** (no se documenta el enfoque erróneo) y los criterios de salida de la fase.

> Regla de mantenimiento: al cerrar una fase, añadir aquí su sección antes de iniciar la siguiente. Cada fase debe dejar siempre una **versión funcional** del producto.

---

## Contexto global

| Concepto         | Decisión                                                                                    |
| ---------------- | ------------------------------------------------------------------------------------------- |
| Repositorio      | Monorepo en GitHub (privado) — `https://github.com/JavierIncio/BookSocial.git`              |
| Branch principal | `main`                                                                                      |
| Backend          | Microservicios Java 21 + Spring Boot 4.1.0, build con **Maven + wrapper** (`mvnw`)          |
| Frontend         | Angular 21 (CLI 21.2.19) + PWA                                                              |
| Comunicación     | REST síncrona vía API Gateway + eventos asíncronos con RabbitMQ                             |
| Persistencia     | Cada servicio es propietario de sus datos: PostgreSQL (command side) + MongoDB (query side) |
| CI/CD            | GitHub Actions (workflow `ci.yml`), filosofía GitOps, despliegue incremental                |
| Infraestructura  | Docker + Docker Compose (local), Terraform (despliegue, fase posterior)                     |
| Observabilidad   | Prometheus + Grafana, logs JSON, traces distribuidas (fase posterior)                       |

### Entorno local verificado

| Herramienta      | Versión                                                                           |
| ---------------- | --------------------------------------------------------------------------------- |
| JDK (Temurin)    | 21.0.12 LTS — `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot` |
| Maven            | 3.9.16 (instalado local y gestionado por el wrapper)                              |
| Node / npm       | 24.11.1 / 11.11.0                                                                 |
| Angular CLI      | 21.2.19                                                                           |
| Docker / Compose | 29.5.3 / v5.1.4                                                                   |
| Git              | 2.51.2                                                                            |
| Terraform        | 1.14.9                                                                            |

---

## Fase 0 — Cimientos ✅ Completada

**Objetivo**: dejar preparado el monorepo, la infraestructura local y el CI/CD base para empezar a construir servicios con garantías.

### Paso 0.1 — Instalar JDK 21 (Temurin)

- Descargar el instalador MSI de Eclipse Temurin **21** desde https://adoptium.net (Windows x64).
- Durante la instalación, marcar **"Set JAVA_HOME variable"** y **"Add to PATH"**.
- Verificar en una terminal nueva:

```powershell
java -version          # -> openjdk version "21.0.x" LTS
echo $env:JAVA_HOME    # -> C:\Program Files\Eclipse Adoptium\jdk-21.0.x...
```

> Nota: el JDK 25 del equipo queda intacto; `JAVA_HOME` apunta a 21, que es lo que usa Maven/Spring. Si se necesita el 25, solo se cambia `JAVA_HOME`.

### Paso 0.2 — Actualizar Angular CLI a 21

```powershell
npm install -g @angular/cli@21
ng version   # -> Angular CLI: 21.x.x
```

Solo actualiza la CLI global; el proyecto Angular se creará con `ng new` en la Fase 1.

### Paso 0.3 — Crear el repositorio GitHub

- Crear el repositorio **vacío** `BookSocial` (privado) en https://github.com/new.
- **No** añadir README, `.gitignore` ni license desde el asistente (nace vacío).
- Guardar la URL remota: `https://github.com/JavierIncio/BookSocial.git`.

### Paso 0.4 — Inicializar git en local

```powershell
git init -b main
git config user.name "JavierIncio"
git config user.email "<tu-correo-de-github@ejemplo.com>"
```

Crear `.gitignore` raíz:

```gitignore
# --- Java / Maven ---
target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar
.idea/
*.iml
.vscode/

# --- Node / Angular ---
node_modules/
dist/
.angular/cache/
*.log

# --- Entorno / secretos ---
.env
.env.*
application-local.yml
application-local.yaml
secrets/
*.pem
*.key

# --- Infraestructura ---
*.tfstate
*.tfstate.backup
*.tfvars
.terraform/
```

Crear `README.md` raíz con una descripción breve del proyecto y referencia a este documento.

### Paso 0.5 — Estructura de carpetas del monorepo

```
frontend/  gateway/  identity-service/  user-service/  book-service/
review-service/  shelf-service/  social-service/  club-service/
messaging-service/  news-service/  notification-service/
infrastructure/  docs/
```

Cada carpeta vacía lleva un `.gitkeep` para que git la rastree en el primer commit.

### Paso 0.6 — Maven + parent POM + wrapper `mvnw`

1. **Instalar Maven** (descarga del zip 3.9.x desde https://maven.apache.org, descomprimir en `C:\Program Files\Apache\apache-maven-3.9.x`).

```powershell
[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\Program Files\Apache\apache-maven-3.9.x", "User")
$path = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$path;C:\Program Files\Apache\apache-maven-3.9.x\bin", "User")
# abrir terminal nueva y verificar
mvn -v   # -> Apache Maven 3.9.x, Java version: 21.0.x
```

2. **Parent POM** en la raíz del repo (`pom.xml`) — agrupará todos los microservicios como módulos:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
    <relativePath/>
  </parent>

  <groupId>com.booksocial</groupId>
  <artifactId>booksocial-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>BookSocial</name>
  <description>Parent pom del monorepo BookSocial</description>

  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <modules>
    <!-- Cada servicio se añadirá aquí cuando se cree su pom -->
  </modules>
</project>
```

3. **Generar el wrapper** (crea `mvnw`, `mvnw.cmd` y `.mvn/wrapper/`):

```powershell
mvn -N wrapper:wrapper
.\mvnw -v
```

### Paso 0.7 — Infraestructura local con Docker Compose

Archivo `infrastructure/docker-compose.yml` con PostgreSQL, MongoDB y RabbitMQ (healthchecks y volúmenes persistidos). Arranque:

```powershell
docker compose -f infrastructure/docker-compose.yml up -d
docker ps   # -> 3 contenedores "Up (healthy)"
```

Consola de gestión de RabbitMQ: http://localhost:15672 (`guest`/`guest`).

> Estas son **solo** las bases de datos y el broker. Los contenedores de las aplicaciones se añadirán en fases siguientes.

### Paso 0.8 — Workflow base de CI/CD (`ci.yml`)

Archivo `.github/workflows/ci.yml` — versión final aplicada:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5

      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: "temurin"
          java-version: "21"
          cache: maven

      - name: Build backend with Maven wrapper
        run: |
          chmod +x mvnw
          ./mvnw -B clean verify
```

Notas:

- Se dispara en cada push a `main` y en cada pull request.
- `setup-java` con caché Maven acelera builds posteriores.
- Se usa el **wrapper** (`./mvnw`) para que CI no dependa de Maven instalado.

### Paso 0.9 — Primer commit y push

```powershell
git add -A
git commit -m "chore: fase 0 - monorepo, infraestructura y CI base"
git remote add origin https://github.com/JavierIncio/BookSocial.git
git push -u origin main
```

Verificar en GitHub: código subido, y **Actions → CI en verde**.

---

### Errores encontrados en la Fase 0 (con solución directa)

**1. `Process completed with exit code 126` en Actions**

- Causa: el script `./mvnw` no tiene bit de ejecución en el runner Ubuntu (al commitear desde Windows, git no lo guarda).
- Solución aplicada: añadir `chmod +x mvnw` antes de ejecutar el wrapper en el workflow (idempotente y portable).

**2. Anotación: "Node.js 20 is deprecated" en actions/checkout@v4 y actions/setup-java@v4**

- Causa: las versiones v4 ejecutan con Node 20, ya deprecado.
- Solución aplicada: subir a `actions/checkout@v5` y `actions/setup-java@v5` (Node 24). Los archivos del repo ya reflejan las versiones correctas.

### Criterios de salida de la Fase 0

- [x] JDK 21 + Maven + wrapper operativos localmente.
- [x] Repositorio GitHub conectado y rama `main` estable.
- [x] Monorepo estructurado con parent POM.
- [x] Postgres, MongoDB y RabbitMQ levantados con healthchecks.
- [x] CI en verde sin anotaciones de deprecación.
- [x] Versión funcional: repo con pipeline operativo.

---

## Fase 1 — Identity Service + Gateway + Security + Angular 21 ✅ Completada

**Objetivo**: primer servicio real: registro con email/contraseña, login con OAuth2 Google, emisión de JWT + refresh tokens, roles (`ADMIN`, `MODERATOR`, `USER`, `MINOR_USER` con edad calculada desde la fecha de nacimiento) y un gateway con filtro de autenticación. Frontend Angular 21 con login, registro y guardas de rutas.

**Progreso**: Fase 1 completada — pasos 1–7 (identity-service con registro, JWT y OAuth2 Google; gateway WebMVC con filtro de validación JWT y headers `X-User-*` strip-then-assert; frontend Angular 21 con login, registro, OAuth2 y guardas; backend contenerizado con Docker Compose y CI ampliado con job de frontend).

> Gestión de secretos: los valores reales viven en `.env` por módulo (`.env`, `.env.*` en `.gitignore`), cargados con `spring.config.import=optional:file:.env[.properties]`. En CI se inyectan como secrets (`APP_JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`) en el bloque `env:` de `.github/workflows/ci.yml`.

### Fase 1.1 — Generación del identity-service ✅ Completada

**Objetivo**: crear el microservicio `identity-service` con Spring Initializr y conectarlo al parent POM del monorepo.

- Generar el proyecto (Java 21, Spring Boot 4.1.0, web, data-jpa, validation, security) en `identity-service/`.
- Añadirlo a `<modules>` del parent POM de la raíz.
- Configurar en `application.yml`: datasource PostgreSQL (`booksocial`/`booksocial`) y puerto `8081` (para no chocar con el futuro gateway).

> Decisión de versión: se alineó el monorepo a **Spring Boot 4.1.0** (commit `abcb588`), dejando el parent POM como única fuente de versión para todos los servicios.

### Fase 1.2 — Modelo de usuario, roles y registro con seed admin ✅ Completada

**Objetivo**: entidad `User`, roles, registro con validación y edad calculada, y seed del primer administrador.

- `domain/User`: email único, `password_hash`, `first_name`, `last_name`, `birth_date`, roles como colección embebida en `user_roles`, `enabled`, timestamps y `google_id` (este último se aprovecha en la fase 1.4).
- `domain/Role`: `ADMIN`, `MODERATOR`, `USER`, `MINOR_USER`.
- `UserService.register`: rechaza email duplicado (`EmailAlreadyExistsException`), encripta con BCrypt y asigna rol `MINOR_USER` si la edad calculada (`Period.between`) es < 18, `USER` en caso contrario.
- `config/AdminDataInitializer`: crea el admin (credenciales `app.admin.*` en `application.yml`) con roles `ADMIN` + `USER` si no existe.
- DTOs `RegisterRequest` (con bean validation) y `UserResponse` (devuelve la edad, nunca la password).

### Fase 1.3 — Spring Security con JWT access/refresh ✅ Completada

**Objetivo**: proteger la API, emitir JWT en login/registro, rotar refresh tokens y soportar logout.

- Dependencias: `spring-boot-starter-security` + `jjwt` (api/impl/jackson) en `identity-service/pom.xml`.
- `security/JwtService`: JWT HS256 con secret base64 de `app.jwt.secret`, claims `uid`, `roles`, `type` (`access`/`refresh`), TTL de 15 min / 7 días.
- `security/JwtAuthFilter`: valida el `Authorization: Bearer` y solo autentica tokens de tipo `access`.
- `security/RestAuthenticationEntryPoint`: respuestas JSON `401` para peticiones no autenticadas.
- `service/RefreshTokenService`: guarda el **hash SHA-256** del refresh token (nunca el valor en claro), valida no revocado + no expirado y revoca al rotar.
- `service/AuthService` + `controller/AuthController`: `/auth/register`, `/auth/login`, `/auth/refresh` (rotación), `/auth/logout` (revocación); `controller/UserController` con `/users/me`.
- `config/SecurityConfig`: CSRF off, sesiones `STATELESS`, `permitAll` para los endpoints `/auth/*`, resto autenticado.
- `exception/GlobalExceptionHandler`: errores JSON uniformes.

#### Errores encontrados en la Fase 1.3 (con solución directa)

**1. `POST /auth/logout` devolvía `401`**

- Causa: `/auth/logout` no estaba en la lista de `permitAll` y el cliente no envía access token al cerrar sesión (solo la cookie).
- Solución aplicada: añadir `/auth/logout` a `permitAll` en `SecurityConfig` (commit `d78cd79`).

### Fase 1.4 — Login OAuth2 con Google ✅ Completada (pendiente de commit)

**Objetivo**: login con la cuenta de Google, creación o vínculo automático del usuario y emisión de los JWT propios del servicio.

- Dependencia: `spring-boot-starter-oauth2-client`.
- Credenciales en `application.yml` (`spring.security.oauth2.client.registration.google`): el `client-id` se mantiene en el repositorio y el **`client-secret` se carga desde la variable de entorno `GOOGLE_CLIENT_SECRET`** (nunca en git).
- Consola de Google Cloud (OAuth consent screen en modo Testing): redirect URI `http://localhost:8081/login/oauth2/code/google`; las cuentas de prueba se añaden como **Test users**.
- `UserService.linkOrCreateOAuthUser`: busca por `google_id` (claim `sub`); si no lo encuentra, busca por email y vincula el `google_id`; si no existe, crea el usuario con password aleatoria e inutilizable, sin `birth_date` y rol `USER`.
- `security/TokenCookieService`: cookie `refresh_token` `httpOnly`, `SameSite=Lax`, `path=/`, con la vida del refresh token; `clear()` para el logout.
- `security/OAuth2AuthenticationSuccessHandler`: vincula/crea el usuario, emite tokens, fija la cookie y redirige a `app.oauth2.frontend-redirect-uri#access_token=...`.
- `security/OAuth2AuthenticationFailureHandler`: redirige a `frontend-redirect-uri#error=access_denied`.
- `config/SecurityConfig`: sesión `IF_REQUIRED` (el flujo OAuth2 guarda el parámetro `state` en la sesión HTTP) y `permitAll` para `/oauth2/authorization/**` y `/login/oauth2/code/**`.
- `/auth/refresh` y `/auth/logout` aceptan el refresh token desde la cookie **o** desde el body JSON (retrocompatibilidad con 1.3).
- `UserRepository.findByGoogleId`.

#### Errores encontrados en la Fase 1.4 (con solución directa)

**1. `IncorrectResultSizeDataAccessException ... expected 1 but found 3` al completar el login de Google**

- Causa: el vínculo leía el atributo de Google con la clave errónea (`attrs.get("googleId")`); el identificador persistente del OIDC claim es **`sub`**.
- Solución aplicada: leer `attrs.get("sub")` en `linkOrCreateOAuthUser`.

**2. `401` al probar `/auth/register` y `/auth/login` con `curl.exe` desde PowerShell 5.1**

- Causa: PowerShell elimina las comillas dobles al pasar el JSON como argumento → Spring recibe `{email:...}` (JSON inválido) y el `RestAuthenticationEntryPoint` responde `401` en lugar de `400`.
- Solución aplicada: usar `Invoke-RestMethod` (con `ConvertTo-Json`) o guardar el body en un archivo y llamar a `curl --data-binary "@body.json"`.

**3. `#error=access_denied` al probar con una segunda cuenta de Google**

- Causa: la aplicación OAuth2 está en modo **Testing** y esa cuenta no estaba en la lista de **Test users** de la pantalla de consentimiento.
- Solución aplicada: añadir la cuenta en OAuth consent screen → Audience → Test users.

#### Criterios de salida de la Fase 1.4

- [x] Login con Google crea el usuario con `google_id` y rol `USER`.
- [x] Login con una cuenta ya registrada por email la vincula (sin duplicados).
- [x] `GET /users/me` devuelve el usuario autenticado por Google.
- [x] Cookie `refresh_token` `httpOnly` + `SameSite=Lax`; rotación en `/auth/refresh`.
- [x] `/auth/refresh` y `/auth/logout` funcionan con cookie y con body JSON.
- [x] Cancelar el login redirige a `#error=access_denied`.
- [x] El `client-secret` de Google queda fuera del repositorio (variable de entorno).
- [x] Versión funcional: identity-service con registro, JWT y login Google.

### Fase 1.7 — Contenerización con Docker Compose y ampliación del CI ✅ Completada

**Objetivo**: levantar el backend completo (postgres + identity-service + gateway) con Docker Compose, dejar los microservicios preparados para contenerizarse (Actuator + configuración overridable por variable de entorno) y ampliar el CI para compilar el frontend Angular.

#### Paso A — Preparación de los servicios para contenerización

- `spring-boot-starter-actuator` añadido a `gateway/pom.xml` y `identity-service/pom.xml`.
- Bloque `management.endpoints.web.exposure.include: health` en ambas configuraciones.
- `/actuator/health` en `permitAll` de ambos `SecurityConfig` (healthcheck sin autenticación).
- Configuración overridable por entorno: el gateway lee `IDENTITY_SERVICE_URI` (default `http://localhost:8081`) y el identity-service lee `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/booksocial`).

#### Paso B — Dockerfiles multi-stage

- `identity-service/Dockerfile` y `gateway/Dockerfile`: stage de build `maven:3.9-eclipse-temurin-21` con `./mvnw -B -pl <módulo> -am package -DskipTests`; stage runtime `eclipse-temurin:21-jre` con `curl` instalado y `ENTRYPOINT ["java","-jar","app.jar"]`.
- `.dockerignore` raíz: excluye `**/target/`, `**/node_modules/`, `.git/`, `**/.env*` y las carpetas `frontend/`, `infrastructure/`, `docs/`, `.github/` (los secretos `.env` nunca entran a las imágenes).

#### Paso C — docker-compose ampliado

- `infrastructure/docker-compose.yml`: nuevos servicios `identity-service` (puerto 8081, `env_file: ../identity-service/.env`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/booksocial`, healthcheck curl a `/actuator/health`, `depends_on: postgres` `service_healthy`) y `gateway` (puerto 8080, `env_file: ../gateway/.env`, `IDENTITY_SERVICE_URI=http://identity-service:8081`, healthcheck curl a `/actuator/health`, `depends_on: identity-service` `service_healthy`).
- Arranque con un solo comando: `docker compose -f infrastructure/docker-compose.yml up -d --build`.

#### Paso D — CI ampliado

- `.github/workflows/ci.yml`: nuevo job `frontend` (Node 24 con caché npm, `npm ci` + `ng build` con `working-directory: frontend`). El job `build` de backend (Maven + Postgres service + secrets) queda intacto.

#### Paso E — Verificación E2E en Docker

- 5 contenedores healthy (postgres, mongodb, rabbitmq, identity-service, gateway).
- Suite API vía gateway: register → `/users/me` → login con cookie `refresh_token` → refresh → logout, todo `200`/`204`.
- Healthchecks `/actuator/health` `UP` en 8080 y 8081.
- Frontend `ng serve` en el host (:4200) con proxy a :8080; flujo completo en navegador: register → home → F5 → logout → login → Google (incógnito).

#### Errores encontrados en la Fase 1.7 (con solución directa)

**1. `GET /oauth2/authorization/google` devuelve `401` a través del gateway**

- Causa: el gateway no tiene esa ruta en `permitAll`. No es un fallo: el frontend no pasa por el gateway para OAuth2, `googleAuthUrl` apunta directamente a `http://localhost:8081/oauth2/authorization/google` (puerto publicado en compose), y el flujo de Google funciona correctamente en navegador.

#### Criterios de salida de la Fase 1.7

- [x] Backend completo levantado con Docker Compose (5 contenedores healthy).
- [x] Healthchecks con Actuator `/actuator/health` sin autenticación.
- [x] Configuración overridable por variable de entorno (`IDENTITY_SERVICE_URI`, `SPRING_DATASOURCE_URL`).
- [x] Secretos fuera de las imágenes (`.env` excluido por `.dockerignore`, inyectado con `env_file`).
- [x] CI compila backend y frontend.
- [x] E2E completo: backend en Docker + frontend en el host + API verde vía gateway.
- [x] Versión funcional: todo el stack de la Fase 1 con un solo `docker compose up`.

### Cierre de la Fase 1

- [x] Fase 1.5 — Generar `gateway` (Spring Cloud Gateway) con filtro de validación JWT (rutas a `identity`, `JwtService`/`JwtAuthFilter`/`SecurityConfig`, entry point 401 JSON y headers `X-User-Id`/`X-User-Email`/`X-User-Roles` strip-then-assert).
- [x] Fase 1.6 — Crear el proyecto Angular 21 (`ng new`) en `frontend/` con login/registro, guardas, interceptor JWT y fixes de auth en identity-service.
- [x] Fase 1.7 — Levantar todo con Docker Compose y ampliar el CI para compilar ambos servicios.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 2 — user-service: perfil y amistades con CQRS ✅ Completada

**Objetivo**: construir `user-service` (puerto `8082`), propietario del perfil de usuario y de las amistades, con arquitectura CQRS (PostgreSQL command side + MongoDB query side) y sincronización de amistades por eventos RabbitMQ.

**Progreso**: Fase 2 completada — esqueleto del servicio (2.1), perfil con dual-write (2.2), amistades con dual-write (2.3) y migración de la sincronización de amistades a eventos (2.4).

### Fase 2.1 — Esqueleto del user-service ✅ Completada

**Objetivo**: crear el microservicio contenerizado y conectado al gateway.

- Proyecto Spring Initializr (Java 21, Spring Boot 4.1.0) con starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation`, `actuator` en `user-service/`; `pom.xml` con parent `booksocial-parent` + jjwt 0.12.6; `<module>` añadido al parent POM raíz.
- `application.yml`: puerto `8082`, `.env` import, datasource Postgres `booksocial`/`booksocial`, `spring.mongodb.uri` overridable, `app.jwt.secret`/`issuer: booksocial-identity`, actuator.
- Seguridad parse-only: `JwtService` (valida sin emitir), `JwtAuthFilter` (solo `type=access`), `RestAuthenticationEntryPoint` (401 JSON), `SecurityConfig` (STATELESS, `/actuator/health` permitAll).
- Gateway: ruta `/profiles/**,/follows/**` → `${USER_SERVICE_URI:http://localhost:8082}`; en compose `USER_SERVICE_URI: http://user-service:8082`.
- `Dockerfile` multi-stage (espejo de identity-service) y servicio `user-service` en compose (`depends_on` postgres+mongodb `service_healthy`, healthcheck curl, env overrides de datasource y Mongo).

#### Errores encontrados en la Fase 2.1 (con solución directa)

**1. user-service conecta a `localhost:27017` aunque `SPRING_DATA_MONGODB_URI` está definida**

- Causa: en Spring Boot 4.1 el prefijo de Mongo es **`spring.mongodb.*`** (env `SPRING_MONGODB_URI`); `spring.data.mongodb.*` ya no se aplica.
- Solución aplicada: renombrar la propiedad en `application.yml` y la variable en compose.

**2. `MongoCommandException ... AuthenticationFailed` (error 18)**

- Causa: el usuario `booksocial` es root y se autentica contra la DB `admin`; sin `?authSource=admin` el driver autentica contra la DB del URI.
- Solución aplicada: añadir `?authSource=admin` a la URI de Mongo.

**3. `Unable to rename ...jar.original` al reconstruir con Maven en Windows**

- Causa: el proceso `java` que ejecuta el JAR mantiene el fichero bloqueado.
- Solución aplicada: detener el proceso java antes de `mvn clean/package`.

### Fase 2.2 — Perfil con CQRS dual-write ✅ Completada

**Objetivo**: perfil de usuario con escrituras en Postgres y lecturas desde Mongo.

- `domain/Profile` (JPA, `userId` único) como command side; `readmodel/ProfileReadModel` (Mongo, `_id`=userId, contadores followers/following/posts) como query side.
- `ProfileService.getOrCreate`/`update`: dual-write (misma operación escribe Postgres y hace upsert del read model); `getByUserId` lee **solo de Mongo** (separación de rutas del CQRS).
- `ProfileController`: `GET/PUT /profiles/me` (identidad desde headers `X-User-Id`/`X-User-Email` puestos por el gateway), `GET /profiles/{userId}`.
- DTOs record con bean validation, `ProfileNotFoundException` y `GlobalExceptionHandler` (404/400 JSON).
- E2E vía gateway: creación on-demand, PUT con dual-write (dato en Postgres y Mongo), lectura desde Mongo, 404 JSON.

### Fase 2.3 — Amistades con CQRS dual-write ✅ Completada

**Objetivo**: relación de amistad (follow) con sus listas y contadores.

- `domain/Follow` (JPA, unique `(followerId, followeeId)`, self-follow → 400) y `readmodel/FollowReadModel` (Mongo, `_id`=`<followerId>:<followeeId>`).
- `FollowController`: `POST/DELETE /follows/{targetUserId}` (201/204), `GET /follows/{userId}/followers` y `GET /follows/{userId}/following`.
- Excepciones: `SelfFollowException` (400), `AlreadyFollowingException` (409), `NotFollowingException` (404).
- Ajuste de contadores del `ProfileReadModel` con `Math.max(0, ...)`.
- E2E vía gateway con dos usuarios: 201/409/400, listas, contadores +1/-1, unfollow 204/404, limpieza en ambas BD.

### Fase 2.4 — Sincronización de amistades con RabbitMQ ✅ Completada

**Objetivo**: desacoplar la escritura del read model de amistades usando eventos.

- Dependencia `spring-boot-starter-amqp`; prefijo de configuración `spring.rabbitmq.*` (sin cambios en Boot 4.1); `guest`/`guest` válido en red Docker (`loopback_users.guest = false`).
- `RabbitConfig`: exchange topic `booksocial.events`, colas `user-service.follows.followed` y `user-service.follows.unfollowed` con sus bindings, y `MessageConverter` `JacksonJsonMessageConverter` (reemplazo del deprecado `Jackson2JsonMessageConverter` en Spring AMQP 4) con trusted packages `com.booksocial.user.events`.
- `FollowEventPublisher` publica `FollowedEvent`/`UnfollowedEvent` dentro de la transacción (fallo de publish → rollback de Postgres); sin Outbox (limitación documentada: hueco commit-tras-publish).
- `FollowEventConsumer` (un `@RabbitListener` por cola): upsert/delete del `FollowReadModel` y contadores **recalculados** con `countByFollowerId`/`countByFolloweeId` (idempotente ante redelivery).
- `FollowService` pasa a escribir solo Postgres + publicar; las listas siguen leyendo Mongo (consistencia eventual).
- Verificación: colas drenadas (0 mensajes), bindings correctos, logs `Processed FollowedEvent/UnfollowedEvent`, Mongo y contadores actualizados.

### Cierre de la Fase 2

- [x] Fase 2.1 — Esqueleto del user-service contenerizado y enrutado por el gateway.
- [x] Fase 2.2 — Perfil con CQRS dual-write (comandos Postgres, lecturas Mongo).
- [x] Fase 2.3 — Amistades con CQRS dual-write (follow/unfollow, listas y contadores).
- [x] Fase 2.4 — Sincronización de amistades por eventos RabbitMQ (sin Outbox; limitación documentada).
- [x] Actualizar este documento al cerrar la fase.

---

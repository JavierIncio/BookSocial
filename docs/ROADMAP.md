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
| Frontend         | Angular 21 (CLI 21.2.21) + PWA + i18n (en/es/pt)                                              |
| Comunicación     | REST síncrona vía API Gateway + eventos asíncronos con RabbitMQ                             |
| Persistencia     | Cada servicio es propietario de sus datos: PostgreSQL (command side) + MongoDB (query side) |
| CI/CD            | GitHub Actions (workflow `ci.yml`), filosofía GitOps, despliegue incremental                |
| Infraestructura  | Docker + Docker Compose (local), **Terraform (GCP: Cloud SQL + Cloud Run + Artifact Registry, con MongoDB Atlas + CloudAMQP gratuitos)** |
| Observabilidad   | Prometheus + Grafana, logs JSON, traces distribuidas (fase posterior)                       |

### Entorno local verificado

| Herramienta      | Versión                                                                           |
| ---------------- | --------------------------------------------------------------------------------- |
| JDK (Temurin)    | 21.0.12 LTS — `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot` |
| Maven            | 3.9.16 (instalado local y gestionado por el wrapper)                              |
| Node / npm       | 24.11.1 / 11.11.0                                                                 |
| Angular CLI      | 21.2.21                                                                           |
| Docker / Compose | 29.5.3 / v5.1.4                                                                   |
| Git              | 2.51.2                                                                            |
| Terraform        | 1.15.8                                                                          |

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
- `ProfileService.getOrCreate`/`update`: dual-write (misma operación escribe Postgres y hace upsert del read model); `getByUserId` lee de Mongo y, si falta, materializa on-demand desde Postgres (o devolvía un perfil **sintético** `user-{id}@booksocial.local` / `displayName:"user-{id}"`) para que el feed/campana nunca reciban 404 y muestren un nombre; `displayName` se deriva de la parte local del email cuando está vacío (`deriveDisplayName`). **[Actualizado en Fase 11**: el sintético se eliminó; ahora se devuelve un read model transitorio sin persistir ni email falso — ver Fase 11.3].
- `ProfileController`: `GET/PUT /profiles/me` (identidad desde headers `X-User-Id`/`X-User-Email` puestos por el gateway), `GET /profiles/{userId}`.
- DTOs record con bean validation, `ProfileNotFoundException` y `GlobalExceptionHandler` (404/400 JSON).
- E2E vía gateway: creación on-demand, PUT con dual-write (dato en Postgres y Mongo), lectura desde Mongo, perfil sintético para userId sin perfil (ya no responde 404). **[En Fase 11 el sintético pasó a ser un transitorio sin email falso].**

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

## Fase 3 — book-service: catálogo de libros con CQRS ✅ Completada

**Objetivo**: construir `book-service` (puerto `8083`), propietario del catálogo de libros, replicando el patrón CQRS del user-service (PostgreSQL command side + MongoDB query side) con búsqueda y alta restringida por rol ADMIN.

**Progreso**: Fase 3 completada — esqueleto del servicio (3.1) y catálogo con búsqueda (3.2).

### Fase 3.1 — Esqueleto del book-service ✅ Completada

**Objetivo**: crear el microservicio contenerizado y enrutado por el gateway.

- Proyecto Spring Initializr (Java 21, Spring Boot 4.1.0) en `book-service/` con starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation`, `actuator`; parent `booksocial-parent` + jjwt 0.12.6; `<module>` en el POM raíz.
- Puerto `8083`, `application.yaml` espejo de user-service; seguridad parse-only copiada (`JwtService`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`, `SecurityConfig`).
- Gateway: ruta `/books/**` → `${BOOK_SERVICE_URI:http://localhost:8083}`; compose con `BOOK_SERVICE_URI: http://book-service:8083`.
- Dockerfile multi-stage y servicio compose espejo de user-service (healthcheck Actuator, `depends_on` postgres+mongodb `service_healthy`).
- **Error resuelto**: falta del driver `org.postgresql:postgresql` (runtime) en el POM → `ClassNotFoundException: org.postgresql.Driver` al arrancar.

### Fase 3.2 — Catálogo CQRS con búsqueda ✅ Completada

**Objetivo**: alta de libros (solo ADMIN), consulta por ISBN y búsqueda por título/autor desde el read model.

- `domain/Book` (JPA, `isbn` único) como command side; `readmodel/BookReadModel` (Mongo, `_id`=isbn) como query side.
- `BookService`: `create` dual-write; `findByIsbn`/`search` leen **solo de Mongo**.
- `BookController`: `POST /books` (201, exige rol `ADMIN` en el header `X-User-Roles` del gateway, 403 en caso contrario), `GET /books/{isbn}` (200/404), `GET /books/search?q=` (búsqueda `ContainingIgnoreCase` sobre título/autor).
- DTOs record con validación, excepciones `BookNotFoundException`/`BookAlreadyExistsException`/`ForbiddenException` y `GlobalExceptionHandler` (400/403/404/409).
- `BookDataSeeder` (CommandLineRunner): si la tabla está vacía, inserta 8 libros de ejemplo escribiendo ambos lados (Postgres + Mongo).
- E2E vía gateway: 403 (USER), 201 (ADMIN), 409 (duplicado), 400 (validación), 200/404 por ISBN (Mongo), búsqueda case-insensitive, 9 libros en ambas BD. `verify` local OK.

### Cierre de la Fase 3

- [x] Fase 3.1 — Esqueleto del book-service contenerizado y enrutado por el gateway.
- [x] Fase 3.2 — Catálogo CQRS con búsqueda y alta restringida por rol ADMIN.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 4 — review-service: reseñas CQRS + primer evento cruzado ✅ Completada

**Objetivo**: construir `review-service` (puerto `8084`) con reseñas de libros (CQRS: Postgres command + Mongo query + stats agregadas) y el primer **evento cruzado** entre servicios: book-service publica `BookCreatedEvent` → review-service consume y mantiene un catálogo local desnormalizado.

**Progreso**: Fase 4 completada — esqueleto (4.1), evento cruzado (4.2) y reseñas CQRS (4.3).

### Fase 4.1 — Esqueleto del review-service ✅ Completada

- Proyecto Spring Initializr en `review-service/` con starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation`, `actuator`, `amqp`; parent `booksocial-parent` + jjwt; driver `postgresql`.
- Puerto `8084`, seguridad parse-only copiada, gateway `Path=/reviews/**` → `${REVIEW_SERVICE_URI:http://localhost:8084}`.
- Compose con `depends_on` postgres+mongodb+rabbitmq, `SPRING_RABBITMQ_HOST`.

### Fase 4.2 — Evento cruzado BookCreatedEvent ✅ Completada

- **book-service**: añadido `spring-boot-starter-amqp`, `RabbitConfig` (exchange + converter), `BookCreatedEvent` + `BookEventPublisher`. `BookService.create` y `BookDataSeeder` publican eventos.
- **review-service**: `RabbitConfig` (exchange + cola `review-service.books.created` + binding), `BookCreatedEvent` (copia local para deserialización), `BookCreatedEventConsumer` → upsert `BookRefReadModel` (isbn, title, author) en Mongo.
- Verificación: reset de libros + re-seeding → 8 `book_refs` en review-service; POST /books → evento → 9º `book_ref`.

### Fase 4.3 — Reseñas CQRS con stats agregadas ✅ Completada

- `domain/Review` (JPA, unique `(book_isbn, user_id)`, rating 1-5) + `ReviewRepository`.
- `readmodel/ReviewReadModel` (Mongo, `_id` = `"<isbn>:<userId>"`) + `ReviewStatsReadModel` (Mongo, ratingCount, averageRating).
- `ReviewService`: `create` (verifica catálogo local → 422 si no existe, dual-write + syncStats), `update`, `listByBook` (Mongo), `summary` (Mongo).
- `ReviewController`: `POST /reviews/{isbn}` (201/409/422), `PUT /reviews/{isbn}` (200), `GET /reviews/books/{isbn}`, `GET /reviews/books/{isbn}/summary`.
- DTOs record con validación, excepciones 404/409/422/400, `GlobalExceptionHandler`.
- E2E: POST 201, duplicate 409, rating inválido 400, PUT 200 con sync, libro inexistente 422, review de libro nuevo vía evento 201. `verify` local OK.

### Cierre de la Fase 4

- [x] Fase 4.1 — Esqueleto del review-service contenerizado.
- [x] Fase 4.2 — Evento cruzado BookCreatedEvent (book-service → review-service).
- [x] Fase 4.3 — Reseñas CQRS con catálogo local y stats agregadas.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 5 — shelf-service: estanterías personales CQRS ✅ Completada

**Objetivo**: construir `shelf-service` (puerto `8085`), propietario de las estanterías personales de cada usuario (leído / leyendo / quiero leer), consumiendo `BookCreatedEvent` para mantener un catálogo local desnormalizado, con CQRS (Postgres command + Mongo query).

**Progreso**: Fase 5 completada — esqueleto (5.1), evento cruzado (5.2) y estanterías CQRS (5.3).

### Fase 5.1 — Esqueleto del shelf-service

- Proyecto Spring Initializr en `shelf-service/` con starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation`, `actuator`, `amqp`; parent `booksocial-parent` + jjwt 0.12.6; driver `postgresql`.
- Puerto `8085`, seguridad parse-only copiada de review-service.
- Gateway: ruta `Path=/shelves/**` → `${SHELF_SERVICE_URI:http://localhost:8085}`.
- Compose: `shelf-service` con `depends_on` postgres+mongodb+rabbitmq, `SPRING_RABBITMQ_HOST`.
- Añadir `<module>` al parent POM raíz.

### Fase 5.2 — Evento cruzado BookCreatedEvent (consumer)

- `config/RabbitConfig`: exchange `booksocial.events` + cola `shelf-service.books.created` + binding key `book.created` + `JacksonJsonMessageConverter` (trusted `com.booksocial.shelf.events`).
- `events/BookCreatedEvent` (copia local), `events/BookCreatedEventConsumer` → upsert `BookRefReadModel` (isbn, title, author) en Mongo.
- Verificar: re-seed de libros → book_refs en shelf-service, POST /books → evento → nuevo book_ref.

### Fase 5.3 — Estanterías CQRS

- `domain/ShelfStatus` (enum: `WANTS_TO_READ`, `READING`, `READ`).
- `domain/Shelf` (JPA, unique `(user_id, book_isbn)`, status, timestamps).
- `readmodel/ShelfReadModel` (Mongo, `_id` = `"<userId>:<isbn>"`, con title/author desnormalizados desde eventos).
- `ShelfService`: create (verifica catálogo local → 422, 409 si duplicado), update status, delete, listByUser (Mongo), findByIsbn (Mongo).
- `ShelfController`: `POST /shelves` (201), `PUT /shelves/{isbn}` (200), `DELETE /shelves/{isbn}` (204), `GET /shelves` (lista del usuario actual), `GET /shelves/{isbn}` (check), `GET /shelves/users/{userId}` (público).
- DTOs record, excepciones 404/409/422/400, `GlobalExceptionHandler`.
- E2E completo y `verify` local.

### Cierre de la Fase 5

- [x] Fase 5.1 — Esqueleto del shelf-service contenerizado.
- [x] Fase 5.2 — Evento cruzado BookCreatedEvent (consumer).
- [x] Fase 5.3 — Estanterías CQRS con catálogo local.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 6 — Backend integración + APIs públicas ✅ Completada

**Objetivo**: enriquecer el backend con integración externa (Google Books API) y abrir endpoints públicos para que el frontend pueda consumir catálogo, reseñas y estanterías sin depender del estado de autenticación.

**Progreso**: Fase 6 completada — Google Books en book-service (6.1), endpoints de usuario en review-service (6.2), endpoints públicos en shelf-service (6.3).

### Fase 6.1 — Google Books API en book-service ✅ Completada

**Objetivo**: auto-import de libros desde Google Books API con búsqueda extendida y creación on-demand por ISBN.

- `config/GoogleBooksProperties`: `@ConfigurationProperties(prefix = "app.google-books")`, record `apiKey` + `apiUrl`.
- `service/google/GoogleBooksClient`: `RestClient` contra Google Books API, `search(query)` y `findByIsbn(isbn)` (query `isbn:{isbn}`), manejo de errores con log.
- `service/google/GoogleBooksResponse`: records anidados (Volume, VolumeInfo, ImageLinks, IndustryIdentifier) con `@JsonProperty` para snake_case.
- `service/google/GoogleBooksMapper`: `toBook(Volume)` → Book entity (persiste Author), `toReadModel(Volume)` → BookReadModel (sin persistencia), métodos públicos `extractIsbn`, `extractYear`, `extractAuthorName`, `extractCategory`, `extractCoverUrl`.
- `BookService`: `searchExternal(q)` combina BD + Google (sin persistir resultados externos), `findByIsbn(isbn)` auto-importa si no existe en BD.
- `BookController`: `GET /books/search/full` (BD + Google), `GET /books/search` (solo BD), `GET /books/{isbn}` auto-importa on-demand.
- Gateway + SecurityConfig: `GET /books/**` permitido sin auth en ambos niveles.
- Config: `GOOGLE_BOOKS_API_KEY` en `.env` (100 req/día sin key, 1000 con key).

**Errores encontrados y corregidos**:
1. `findByIsbn` usaba `.queryParam("isbn", isbn)` en vez de `.queryParam("q", "isbn:" + isbn)`.
2. Campo `category` (singular, String) no mapeaba `categories` (plural, List<String>) de Google Books API.
3. `api-key` en `application.yaml` sin default vacío → fallaba en CI sin `.env`.
4. Gateway + book-service bloqueaban GETs sin auth → añadido `permitAll()` para `GET /books/**`.

### Fase 6.2 — Endpoints de usuario en review-service ✅ Completada

**Objetivo**: permitir listar reseñas por usuario (perfil público y mi perfil).

- `ReviewReadModelRepository`: añadido `findByUserIdOrderByCreatedAtDesc(Long userId)`.
- `ReviewService.listByUser(Long userId)`: lista reseñas desde Mongo por userId.
- `ReviewController`: `GET /reviews/me` (usa header `X-User-Id`), `GET /reviews/users/{userId}` (path variable).

### Fase 6.3 — Endpoints públicos en shelf-service ✅ Completada

**Objetivo**: permitir consultar estanterías por usuario y por libro sin autenticación (para perfil público y detalle de libro).

- `ShelfReadModelRepository`: añadido `findAllByBookIsbn(String bookIsbn)`.
- `ShelfService.listByBookIsbn(String bookIsbn)`: lista todas las estanterías con un libro.
- `ShelfController`: `GET /shelves/{isbn}` (usuarios con ese libro), `GET /shelves/users/{userId}` (estanterías de un usuario).
- **Path ordering**: `GET /shelves/users/{userId}` declarado antes de `GET /shelves/{isbn}` para evitar conflicto de path matching.
- Gateway + shelf-service SecurityConfig: `GET /shelves/**` permitido sin auth.

### Cierre de la Fase 6

- [x] Fase 6.1 — Google Books API integrada en book-service (search full + auto-import ISBN).
- [x] Fase 6.2 — Endpoints de usuario en review-service (GET /reviews/me + GET /reviews/users/{userId}).
- [x] Fase 6.3 — Endpoints públicos en shelf-service (GET /shelves/{isbn} + GET /shelves/users/{userId}).
- [x] Gateway SecurityConfig actualizado para permitir GETs públicos en books y shelves.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 7 — Author entity + Open Library + migración `author` → `authorId`/`authorName` ✅ Completada

**Objetivo**: promover el campo `author` (String) a una entidad independiente `Author` (Postgres + Mongo) integrada con **Open Library API** para datos biográficos y obras. Migrar todos los servicios downstream para usar `authorName`+`authorId` en el evento cruzado `BookCreatedEvent` y sus read models.

**Progreso**: Fase 7 completada — Author entity + Open Library en book-service (7.1), migración de downstream services (7.2).

### Fase 7.1 — Author entity + Open Library en book-service ✅ Completada

**Objetivo**: entidad `Author` con cache dual (Postgres + Mongo), integración Open Library API, y migración de `Book.author` a `Book.authorId`.

- **Author entity** (`domain/Author.java`): JPA entity con `id`, `openLibraryId` (único), `name`, `bio`, `birthDate`, `deathDate`, `photoUrl`, `topSubjects` (JSON serializado), `workCount`, `createdAt`.
- **AuthorReadModel** (`readmodel/AuthorReadModel.java`): Mongo document con `_id` = `openLibraryId`, mismos campos + `cachedAt`.
- **AuthorRepository** (JPA): `findByOpenLibraryId`, `findByNameContainingIgnoreCase`.
- **AuthorReadModelRepository** (Mongo): `findByNameContainingIgnoreCase`.
- **Book.java migrado**: campo `author` eliminado, añadido `authorId` (Long FK → `authors.id`).
- **BookReadModel migrado**: campos `authorName`+`authorId` (String) en vez de `author`.
- **BookReadModelRepository migrado**: query `findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase`.
- **BookResponse + CreateBookRequest**: `authorName`+`authorId` en vez de `author`.
- **BookService migrado**: `create`, `findByIsbn` (auto-import), `search`, `searchExternal`, `upsertReadModel`, `toResponse` — todos resuelven `Author` por FK.
- **GoogleBooksMapper migrado**: inyecta `AuthorRepository`, crea/busca `Author` antes de construir `Book`.
- **BookDataSeeder migrado**: crea autores vía `AuthorRepository`, asigna `authorId`, publica evento con 4 args.
- **BookCreatedEvent migrado**: `(bookIsbn, title, authorName, authorId, occurredAt)`.
- **BookEventPublisher migrado**: `publishBookCreated(bookIsbn, title, authorName, authorId)`.

#### Open Library integration

- **OpenLibraryProperties**: `@ConfigurationProperties(prefix = "app.open-library")`, record `apiUrl` + `userAgent`.
- **OpenLibraryClient**: `RestClient` contra `https://openlibrary.org`, endpoints `searchAuthors`, `getAuthor`, `getWorks`. User-Agent `booksocial/1.0`. Rate limit ~3 req/s.
- **OpenLibraryMapper**: `toReadModel(AuthorDoc)` → `AuthorReadModel`, `coverUrl(olId)` → URL de cover, `extractKey(path)` → OL key.
- **OpenLibraryResponse**, **AuthorDetailResponse**, **WorksResponse**: records anidados para la API de Open Library.
- **AuthorService**: `searchAuthors` (cache local → Open Library), `getAuthor` (cache → Open Library), `getAuthorWorks`, `createAuthor`.
- **AuthorController**: `GET /authors/search`, `GET /{olId}`, `GET /{olId}/works`, `POST` (ADMIN).
- **Cache pattern**: primera búsqueda → Open Library API → guardar en Postgres+Mongo; siguientes → Mongo.
- **BookServiceApplication**: `@EnableConfigurationProperties({GoogleBooksProperties.class, OpenLibraryProperties.class})`.

#### Gateway + SecurityConfig

- **Gateway application.yaml**: ruta `/books/**,/authors/**` → book-service.
- **Gateway SecurityConfig**: `GET /authors/**` permitAll.
- **book-service SecurityConfig**: `GET /authors/**` permitAll.

### Fase 7.2 — Migración de downstream services ✅ Completada

**Objetivo**: actualizar `BookCreatedEvent` y `BookRefReadModel` en review-service y shelf-service para usar `authorName`+`authorId`.

- **review-service**:
  - `BookCreatedEvent`: `(bookIsbn, title, authorName, authorId, occurredAt)`.
  - `BookCreatedEventConsumer`: constructor con 4 args.
  - `BookRefReadModel`: `authorName`+`authorId` en vez de `author`.
- **shelf-service**:
  - `BookCreatedEvent`: `(bookIsbn, title, authorName, authorId, occurredAt)`.
  - `BookCreatedEventConsumer`: constructor con 4 args.
  - `BookRefReadModel`: `authorName`+`authorId` en vez de `author`.
  - `ShelfReadModel`: `authorName`+`authorId` en vez de `author`, constructor actualizado.
  - `ShelfResponse`: `authorName`+`authorId` en vez de `author`.
  - `ShelfService.toResponse()`: actualizado.

#### Verificación

- Los 4 servicios (book, review, shelf, gateway) compilan y pasan tests (`mvn compile -q` + `mvn test -q`).

### Cierre de la Fase 7

- [x] Fase 7.1 — Author entity + Open Library en book-service.
- [x] Fase 7.2 — Migración de downstream services (review + shelf).
- [x] Gateway + SecurityConfig actualizados para `/authors/**`.
- [x] Verificación: compilación y tests OK en los 4 servicios.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 8 — Frontend Angular: catálogo, reseñas y estanterías ✅ Completada

**Objetivo**: integrar el frontend Angular con las APIs de catálogo, reseñas, estanterías y autores a través del gateway: páginas de catálogo con búsqueda, detalle de libro con reseñas y estantería, mi estantería con filtros y formulario de reseñas.

**Progreso**: Fase 8 completada — servicios+modelos (8.1), catálogo (8.2), detalle de libro (8.3), mi estantería + nav compartido (8.4), formulario de reseña (8.5), página de autor (8.6).

### Fase 8.1 — Models + Services ✅ Completada

- `core/models/`: `book.models.ts` (`BookResponse`), `author.models.ts` (`AuthorResponse`, `WorkEntry`, `WorksResponse`), `review.models.ts` (`ReviewResponse`, `ReviewSummaryResponse`), `shelf.models.ts` (`ShelfResponse`, union type `ShelfStatus`) — alineados 1:1 con los DTOs del backend.
- `core/services/`: `BookService` (search/searchFull/getByIsbn), `AuthorService` (search/detail/works), `ReviewService` (byBook/summary/mine/create/update), `ShelfService` (mine/byUser/create/updateStatus/remove). Convención: `inject(HttpClient)` + `Observable<T>` tipado.

### Fase 8.2 — Página de catálogo ✅ Completada

- `features/catalog/`: ruta pública `/catalog`, lazy loaded.
- Carga inicial con `GET /books/search?q=` vacío (catálogo local completo); búsqueda con `GET /books/search/full` (BD + Google Books).
- Reactive Forms para el buscador; grid responsive de tarjetas con fallback de portada; estados loading/error/empty diferenciados; tarjetas enlazan a `/book/:isbn`.

### Fase 8.3 — Detalle de libro ✅ Completada

- `features/book-detail/`: ruta pública `/book/:isbn`.
- Info del libro (auto-import desde Google si no está en BD), rating medio + lista de reseñas (solo autenticado), caja "My shelf" con alta/cambio de estado/borrado.
- Página pública: reseñas y estantería se cargan condicionalmente según `isAuthenticated()`.

### Fase 8.4 — Mi estantería + nav compartido ✅ Completada

- `features/my-shelf/`: ruta `/shelf` protegida con `authGuard`; filtro por estado vía chips (All/Want to read/Reading/Read) con `computed()` reactivo; badges de estado coloreados; enlace a detalle.
- `shared/components/nav/`: componente standalone reutilizado en todas las páginas de la app (brand, Catalog, My shelf + Logout autenticado / Log in invitado).

### Fase 8.5 — Formulario de reseña ✅ Completada

- En el detalle de libro: "Write a review" / "Edit your review" con selector de 1–5 estrellas clicables + comentario opcional.
- Precarga la reseña propia vía `GET /reviews/me`; POST vs PUT según exista; refresca summary y lista al guardar.

### Fase 8.6 — Página de autor ✅ Completada

**Backend:**
- `GET /authors/id/{authorId}`: resolución por PK interna (la que exponen los libros como `authorId`). Lógica en 3 pasos: (1) Postgres solo para lookup PK → olId; (2) si el autor no tiene olId, `resolveOpenLibraryIdByName()` exact-match contra Mongo, o busca en Open Library + cachea en Mongo; (3) ficha completa desde `getAuthor()` (Mongo-first).
- `AuthorService.getAuthor()` rediseñado: Mongo-first con merge (conserva subjects/workCount del cache cuando enriquece bio).
- `AuthorDetailResponse.bio`: `Object` + `bioText()` para manejar la bio polimórfica de Open Library (string vs objeto `{type, value}`).

**Frontend:**
- `features/author-detail/`: ruta pública `/author/:authorId`, lazy loaded. Foto/iniciales + nombre + fechas + subjects + bio + grid de obras desde Open Library.
- Enlace en `book-detail`: nombre del autor clickable → `/author/{authorId}`.
- `proxy.conf.json`: claves convertidas a regex con frontera (`^/auth(/|$)`) para evitar colisión `/author` vs `/auth` al refrescar.

#### Fixes durante la fase

- **Backend**: `toResponse` null-safe (crash con resultados efímeros de Google Books), retry de 3 intentos ante 503 transitorios de Google, filtro/dedupe por ISBN en `searchExternal`, normalización de `api-url`, parseo de la bio polimórica de Open Library (`Object` + `bioText()`), `getAuthor` Mongo-first con merge. Documentado en GUIDE 7.3–7.4.
- **Frontend**: `proxy.conf.json` ampliado con claves regex con frontera (`^/auth(/|$)` etc.) para evitar colisión entre rutas de SPA y prefijos de API; entrada errónea `/catalog` eliminada. UI íntegramente en inglés (i18n posterior).

### Cierre de la Fase 8

- [x] Fase 8.1 — Models + Services (books/authors/reviews/shelves).
- [x] Fase 8.2 — Página de catálogo con búsqueda.
- [x] Fase 8.3 — Detalle de libro con reseñas y estantería.
- [x] Fase 8.4 — Mi estantería con filtro por estado + nav compartido.
- [x] Fase 8.5 — Formulario crear/editar reseña.
- [x] Fase 8.6 — Página de autor con obras.
- [x] Verificación: build de producción OK; flujos E2E probados contra el stack Docker.
- [x] Actualizar este documento al cerrar la fase.

---

## Fase i18n — Internacionalización Angular ✅ Completada

**Objetivo**: añadir internacionalización (`@angular/localize`) al frontend Angular 21 con soporte para 3 idiomas: inglés (default), español y portugués.

**Progreso**: Fase i18n completada — setup (A), anotación de templates (B), anotación de TypeScript (C), extracción y traducción (D), verificación (E).

### Fase A — Setup ✅ Completada

- **Upgrade Angular**: `^21.2.0` → `^21.2.21` (todos los paquetes alineados).
- **Instalar `@angular/localize`**: `@angular/localize@^21.2.21` en devDependencies.
- **Polyfill**: `"polyfills": ["@angular/localize/init"]` en `angular.json` options.
- **Tipos**: `"types": ["@angular/localize"]` en `tsconfig.app.json` compilerOptions.
- **i18n config**: bloque `"i18n"` en nivel de proyecto en `angular.json` con `sourceLocale: "en"` y locales `es`/`pt`.
- **Build config**: `"localize": true` en configuración production de `angular.json`.

#### Errores encontrados (con solución directa)

1. **`ng add @angular/localize` falla por peer dependency conflict** — `@angular/localize` resolvía a `21.2.21` pero compiler era `21.2.19`. Solución: borrar `node_modules` + `package-lock.json`, actualizar todos los rangos a `^21.2.21` en `package.json`, reinstalar limpio.
2. **Warning `Include '@angular/localize/init' as a polyfill instead`** — import en `main.ts` no es la forma recomendada en Angular 21. Solución: quitar import, usar polyfill en `angular.json`.
3. **Propiedad `i18n` no permitida en `options`** — en `@angular/build:application`, `i18n` va a nivel de proyecto (no dentro de `options` ni `architect.build`). Solución: mover bloque `i18n` a nivel `"frontend": { "i18n": {...} }`.
4. **`$localize` not found** — `types: []` en `tsconfig.app.json` no incluía `@angular/localize`. Solución: añadir `"@angular/localize"` al array.

### Fase B — Anotación de templates ✅ Completada

- 9 archivos HTML anotados: `nav.html`, `login.html`, `register.html`, `oauth2-callback.html`, `home.html`, `catalog.html`, `book-detail.html`, `my-shelf.html`, `author-detail.html`.
- ~50 strings anotados con `i18n="@@key"` (attributes, text content, placeholders, aria-labels).
- `category` (dato de BD) no traducido intencionalmente.
- Build de desarrollo verificado sin errores.

### Fase C — Anotación de TypeScript ✅ Completada

- 8 archivos `.ts` modificados: `login.ts`, `register.ts`, `oauth2-callback.ts`, `home.ts`, `catalog.ts`, `book-detail.ts`, `my-shelf.ts`, `author-detail.ts`.
- ~25 strings reemplazados con `$localize` tagged template literals.
- `statusLabels` maps y `filters` arrays traducidos.
- Build de desarrollo verificado sin errores.

### Fase D — Extracción y traducción ✅ Completada

- `ng extract-i18n` → 87 messages extraídos a `src/locale/messages.xlf`.
- `src/locale/messages.es.xlf` — traducciones al español (87 trans-units).
- `src/locale/messages.pt.xlf` — traducciones al portugués (87 trans-units).
- Todos los `<x>` tags, `ctype`, `equiv-text` preservados intactos.
- Nombres propios no traducidos: BookSocial, Google, Open Library, ISBN.

### Fase E — Verificación ✅ Completada

- `ng build` produce `dist/frontend/browser/{en,es,pt}/`.
- Cada locale tiene su bundle completo con traducciones embebidas.

### Cierre de la Fase i18n

- [x] Fase A — Setup (`@angular/localize`, polyfill, tipos, angular.json).
- [x] Fase B — Templates anotados (~50 strings en 9 archivos).
- [x] Fase C — TypeScript anotado (~25 strings en 8 archivos).
- [x] Fase D — Archivos de traducción creados (es + pt).
- [x] Fase E — Build de producción con 3 locales verificado.
- [x] Actualizar este documento al cerrar la fase.

## Fase 9 — Feed social + Notificaciones ✅ Completada

Objetivo: construir el **feed social** de actividad (`social-service`, :8086) y las **notificaciones en tiempo real** (`notification-service`, :8087), ambos como proyecciones de lectura puras sobre **MongoDB + RabbitMQ**, con push **WebSocket STOMP**.

### Fase 9.1 — Esqueleto del social-service ✅ Completada

- Proyecto en `social-service/` **solo Mongo** (sin JPA): starters `spring-webmvc`, `spring-data-mongodb`, `spring-security`, `spring-validation`, `actuator`, `amqp`; parent `booksocial-parent` + jjwt.
- Puerto **8086**, seguridad parse-only (sin autenticación de sesión, solo decode JWT para el gateway), Dockerfile multi-stage documentado.
- Gateway: ruta `Path=/feed/**` → `${SOCIAL_SERVICE_URI:http://localhost:8086}`.
- Compose: bloque `social-service` (8086) con mongodb+rabbitmq, depends_on y healthcheck.

### Fase 9.2 — Eventos de dominio en review-service y shelf-service ✅ Completada

- **review-service**: `ReviewCreatedEvent`/`ReviewUpdatedEvent` + `ReviewEventPublisher` + hooks en `ReviewService.create/update` (denormalizan `title`/`authorName`/`authorId` desde los `book_refs`). Keys: `review.created`, `review.updated`.
- **shelf-service**: `ShelfChangedEvent` + `ShelfEventPublisher` + hooks en `create` y `updateStatus` (no en `delete`). Key: `shelf.changed`. La cola original `shelf-service.books.created` (`book.created`) se conserva.
- Ambos compilan y mantienen sus consumers previos intactos.

### Fase 9.3 — Feed social con fanout-on-write ✅ Completada

- Read models en Mongo: `ActivityItemReadModel`, `FollowerIndexReadModel` (colección `followers`, `_id` = userId, lista de followerIds), `FeedEntryReadModel` (colección `feed_entries`, `_id` = `feedUserId:activityId`).
- Copias locales de eventos (desacoplamiento): `FollowedEvent`, `UnfollowedEvent`, `ReviewCreatedEvent`, `ReviewUpdatedEvent`, `ShelfChangedEvent`. Interface `ReviewEvent` compartida por los dos records de review.
- `RabbitConfig` con 5 colas de consumo: `social-service.follows.followed`, `.follows.unfollowed`, `social-service.reviews.created`, `.reviews.updated`, `social-service.shelves.changed`.
- `FeedService`: `handleFollowed/Unfollowed/ReviewCreated/ReviewUpdated/ShelfChanged`, `fanout`/`fanoutToUser` (escribe actividad + copia en feed de cada seguidor), `generateActivityId` (UUID) y `getFeed(userId, cursor, limit)` con paginación cursor (`occurredAt` + `_id` descendentes, limit+1).
- Consumers: `FollowEventConsumer`, `ReviewEventConsumer`, `ShelfEventConsumer`.
- `FeedController`: `GET /feed?cursor=&limit=` con `X-User-Id` (el gateway hace strip-and-assert y lo reinyecta desde el claim `uid`).
- E2E verificado: follow social2→social1 pobló `followers` (`8:[9]`), actividades y `feed_entries`; la estantería generó entradas `SHELF`; `GET /feed` (vía gateway) devuelve ordenado y con paginación.

### Fase 9.4 — Notificaciones con WebSocket STOMP ✅ Completada

- Proyecto en `notification-service/` (solo Mongo, puerto **8087**), starter extra `spring-boot-starter-websocket`.
- `NotificationReadModel` (colección `notifications`, `_id` = `userId:notificationId`, **idempotente**) + repositorio (`findByUserIdOrderByOccurredAtDesc`, `countByUserIdAndReadFalse`).
- `NotificationService`: `createFollowNotification` (`notificationId = "FOLLOW:" + followerId`), `listNotifications`, `unreadCount`, `markAllAsRead` (bulk `MongoTemplate.updateMulti`).
- `RabbitConfig`: colas `notification-service.follows.followed` + `notification-service.reviews.created` (esta última sin consumer aún).
- `FollowEventConsumer`: consume `follow.followed` → crea notificación + **push STOMP** a `/topic/notifications/{userId}` vía `SimpMessagingTemplate`.
- `WebSocketConfig` (`@EnableWebSocketMessageBroker`): endpoint `/ws`, broker `/topic`+`/queue`, app prefix `/app`.
- `JwtHandshakeInterceptor` (`@Component` inyectado): valida el JWT del query string `?token=`, extrae el claim **`uid`** y lo guarda en los atributos de sesión.
- `NotificationController`: `GET /notifications`, `GET /notifications/unread-count`, `POST /notifications/read` — todos con `@RequestHeader("X-User-Id")`.
- Gateway: ruta `/notifications/**` → `${NOTIFICATION_SERVICE_URI:http://localhost:8087}`.

#### Errores encontrados y corregidos en la Fase 9 (con solución directa)

1. **El gateway reescribe `X-User-Id`** (strip-and-assert): descarta el header del cliente y lo reinyecta desde el claim `uid` del JWT. Los E2E que forzaban el header a mano recibían los datos del `uid` del token, no del header. Regla: contra el gateway hay que usar el token del usuario correcto.
2. **`JwtHandshakeInterceptor` fallaba el handshake**: `Long.valueOf(claims.getSubject())` petaba porque el `sub` del JWT es el **email**. Solución: leer el claim `uid` como `Number`.
3. **El gateway devolvía 401 al handshake `/ws`**: SecurityConfig del gateway exigía auth en `anyRequest()`, pero el handshake WS no lleva `Authorization`. Solución: `permitAll("/ws/**")` en gateway (la validación real la hace notification-service con el mismo `APP_JWT_SECRET`).
4. **Spring Cloud Gateway (WebMVC) NO proxea WebSockets** (`Can "Upgrade" only to "WebSocket"`): el routing reactivo es imprescindible para WS. Decisión: el cliente STOMP se conecta **directo** a `ws://localhost:8087/ws?token=` (sin pasar por el gateway); la ruta `/ws/**` del gateway se eliminó. En producción haría falta un proxy con soporte WS (nginx/traefik) o SCG reactivo.
5. **`/ws-info` fantasmatico**: permitAll huérfano en notification-service (endpoint inexistente). Eliminado.

#### Cierre de la Fase 9

- [x] Fase 9.1 — Esqueleto social-service (solo Mongo, :8086) + compose + gateway.
- [x] Fase 9.2 — Eventos de dominio en review (created/updated) y shelf (changed).
- [x] Fase 9.3 — Feed con fanout-on-write, índice de seguidores y paginación cursor.
- [x] Fase 9.4 — Notificaciones: read model + consumer + REST + WebSocket STOMP con push.
- [x] E2E: REST vía gateway (`/notifications`, `/unread-count`, `/read`) y push STOMP en tiempo real (cliente Node con paquete `ws`).
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 10 — Frontend: feed social + notificaciones en tiempo real ✅ Completada

**Objetivo**: integrar en el frontend Angular el **feed social** (página `/feed` con la actividad de los usuarios seguidos y paginación por cursor) y las **notificaciones en tiempo real** (campana en el nav con badge de no leídas, lista desplegable, marcar leídas y suscripción **STOMP** directa a `ws://localhost:8087/ws?token=`). Se desvía puntualmente de la convención "el usuario escribe el código": esta fase la implementa el asistente a petición del usuario.

### Fase 10.1 — Dependencias, proxy y base de datos de modelo ❌→✅

- **Deps**: `@stomp/stompjs` (cliente STOMP) + `websocket` (+ `@types/websocket` dev) en `frontend/package.json`.
- **angular.json**: `allowedCommonJsDependencies: ["@stomp/stompjs", "websocket"]` en build options para silenciar el WARN de CommonJS (el paquete no es ESM).
- **proxy.conf.json**: añadidas claves `^/feed(/|$)` y `^/notifications(/|$)` → gateway `:8080`.
- **`AuthService.userId()`**: decodifica el claim **`uid`** del JWT (base64url, sin librería externa) — necesario para el topic STOMP `/topic/notifications/{userId}`.
- **Models 1:1 con DTOs**: `core/models/feed.models.ts` (`FeedActivityType`, `FeedPayload`, `FeedItemResponse`, `FeedPageResponse`), `core/models/notification.models.ts` (`NotificationResponse`), `ProfileResponse` añadido a `user.models.ts`.
- **Services**: `FeedService.getFeed(cursor?, limit?)` (GET `/feed`), `NotificationService.list()/unreadCount()/markAllAsRead()` (GET `/notifications`, `/unread-count`, POST `/read`), `UserService.profile(userId)` (GET `/profiles/{userId}`).

### Fase 10.2 — Suscripción STOMP en tiempo real ✅ Completada

- `core/services/notification.realtime.service.ts` (`NotificationRealtimeService`): `Client` de `@stomp/stompjs` con `brokerURL = ws://localhost:8087/ws?token=<jwt>` (WS directo, sin gateway — SCG WebMVC no proxea WebSockets), `reconnectDelay: 5000`, `onConnect` suscribe a `/topic/notifications/{userId}` y parsea `NotificationResponse`.
- Señales `connected()`; `connect(onNotification)` gestiona el ciclo de vida (disconnect previo, activate) y `disconnect()` lo cierra en teardown.
- El token se URL-encodea; los frames malformados se ignoran.

### Fase 10.3 — Página de feed ✅ Completada

- `features/feed/` — ruta `/feed` (lazy) protegida con `authGuard`.
- Carga inicial (`limit=10`) y **"Load more"** con `nextCursor`; estados loading/error/empty.
- Tarjetas por tipo: `FOLLOW` (te siguió / siguió a otro lector), `REVIEW` (título + rating ★ + comentario) y `SHELF` (título + estado con badges reutilizando keys `shelfStatus*`); enlaces a `/book/:isbn`; fecha con `DatePipe`.
- **Enriquecimiento de nombres de actor**: `GET /profiles/{userId}` por actor distinto con caché **reactiva** (`signal<Map<number,string>>`) porque zoneless no redibuja con mutaciones ajenas a signals; "You"/"Tú" cuando `actorId === userId`.

### Fase 10.4 — Campana de notificaciones ✅ Completada

- `features/notifications/notification-bell/` integrada en el nav (solo autenticado): badge con `unread-count`, dropdown con lista (nombre del follower enriquecido vía `/profiles/{id}`), botón **"Mark all as read"** (optimista: POST `/notifications/read` + reset local) y **push en tiempo real** que antepone la notificación entrante y sube el contador.
- Ciclo de vida: `ngOnInit` → `load()` + `realtime.connect()`; `ngOnDestroy` → `realtime.disconnect()`.
- Nav: nuevo enlace "Feed" (`@@navFeed`) y `<app-notification-bell />`.

### Fase 10.5 — i18n (extracción y traducciones) ✅ Completada

- `ng extract-i18n --output-path src/locale` → **104 trans-units** (116 message occurrences) en `messages.xlf` (+23 respecto a la Fase 9).
- Traducciones completas añadidas a `messages.es.xlf` y `messages.pt.xlf`. Fix de la fase i18n: los `$localize` de TS usaban `$localize`@@key:Text`` (sin `:` inicial), que muestra el literal `@@key:Text` en runtime — corregidos los 43 usos a `$localize`:@@key:Text```, re-extraídos con IDs con nombre y re-mapeadas las traducciones es/pt.
- `feedLoadMore` y otros con interpolación preservan `<x id="INTERPOLATION" .../>`.

### Verificación de la Fase 10

- `ng build` (producción, `localize: true`): **OK sin warnings**, 3 locales `dist/frontend/browser/{en,es,pt}/`.
- Build dev sin errores; i18n: xlf == es == pt == 104 trans-units (sin faltantes ni extras).
- Pendiente: **E2E manual en navegador contra el stack Docker** (two-user: feed + push STOMP en vivo). Commit + push hecho (`cc12352`).

### Fixes post-Fase 10 (commiteables en `main`)

- **Colisión proxy↔SPA**: el dev-server (Vite) proxea las navegaciones de página completas antes que el fallback SPA; la clave `^/feed` del proxy hacía que **F5 en `/feed`** devolviera el 401 del gateway. Fix: la API pasó a `/api/feed` y `/api/notifications` con `pathRewrite` → `/feed`/`/notifications`; la página SPA `/feed` ya no colisiona.
- **Query string**: Vite matchea `req.url` incluyendo el query string, así que la frontera `(/|$)` no matcheaba `/api/feed?limit=10` → el dev-server servía `index.html` (`text/html`) → **"Failed to load your feed."**. Fix: **todas** las claves del proxy usan el límite `(\?|/|$)`. Esto también protegía (aún estando roto) a `/books/search?q=`, `/authors/search?q=` y `/books/search/full?q=`.
- **Sesión en F5**: guard asíncrono con `AuthService.ensureSession()` (promesa memoizada) + `withEnabledBlockingInitialNavigation()` para que la restauración de sesión (refresh por cookie) preceda a la navegación protegida.
- **Verificado con Chrome headless + CDP** (F5 a `/feed` con cookie `refresh_token`): `/feed` → index.html (SPA), `/api/feed?limit=10` → JSON 200, refresh por cookie 200, feed renderizado con actividad FOLLOW/REVIEW/SHELF y nombres enriquecidos. El push STOMP funciona (solo permite origen `http://localhost:4200`; para el E2E usar el puerto por defecto).

### Cierre de la Fase 10

- [x] Fase 10.1 — Deps STOMP + proxy (`/feed`, `/notifications`) + models/services + `AuthService.userId()`.
- [x] Fase 10.2 — `NotificationRealtimeService` con STOMP directo a `:8087` y topic por userId.
- [x] Fase 10.3 — Página `/feed` con paginación cursor y enriquecimiento de nombres.
- [x] Fase 10.4 — Campana de notificaciones en nav (badge, lista, mark-all-read, push en vivo).
- [x] Fase 10.5 — i18n: extracción + traducciones es/pt. **Fix**: los 43 `$localize` de TS usaban `$localize`@@key:Text`` (mostraban el literal en runtime) → corregidos a `$localize`:@@key:Text```; `feedLoadMore`/`feedLoadingMore` separados en dos trans-units para que el botón se traduzca. **104 trans-units** con es/pt completos.
- [x] Verificación: build de producción con 3 locales, sin warnings.
- [ ] E2E manual en navegador contra el stack Docker (feed + push STOMP entre dos usuarios).
- [x] Commit + push de la Fase 10 (`cc12352`).
- [x] Actualizar este documento al cerrar la fase.

---

## Fase 12 — Notificación de reseñas (consumer `review.created` + fanout a seguidores) ✅ Completada

**Objetivo**: completar la infraestructura de notificaciones con la notificación de **nuevas reseñas**. Cierre del pendiente heredado de la Fase 9.4: la cola `notification-service.reviews.created` ya estaba declarada pero **sin consumer**. Se añade el fanout a los seguidores del autor de la reseña (patrón espejo del feed del social-service) con push STOMP.

> Nota de convención: el backend lo implementa el usuario; el asistente guía, revisa y verifica. En esta fase el usuario escribió el código paso a paso siguiendo la guía.

### Fase 12.1 — Índice de seguidores local en notification-service ✅ Completada

- `readmodel/FollowerIndexReadModel` (colección `followers`, `_id` = userId → lista de followerIds) + `FollowerIndexReadModelRepository` — misma colección y patrón que el social-service (desacoplado, sin HTTP).
- `events/UnfollowedEvent` (copia local) + cola/binding `notification-service.follows.unfollowed` (`follow.unfollowed`) en `RabbitConfig`.
- `FollowEventConsumer` extendido: consume `followed` **y** `unfollowed`, delega en `NotificationService` (consumer delgado).
- `NotificationService`:
  - `handleFollowed` → notificación FOLLOW (existente) **+** `addFollower` (mantiene el índice).
  - `handleUnfollowed` → `removeFollower`.
  - `addFollower`/`removeFollower` con read-modify-write sobre el índice (mismo patrón que social-service).

### Fase 12.2 — Fanout de reseñas a seguidores ✅ Completada

- `events/ReviewCreatedEvent` (copia local, mismo record que review-service: `reviewId`, `bookIsbn`, `title`, `authorName`, `rating`, `comment`, `actorUserId`, `occurredAt`).
- `events/ReviewEventConsumer` → `@RabbitListener` sobre `REVIEW_CREATED_QUEUE` (cola ya declarada en Fase 9.4) que delega en `NotificationService.handleReviewCreated`.
- `NotificationService.handleReviewCreated`:
  - Construye payload rico con **`HashMap` mutable** (no `Map.of`) para tolerar `comment` **nullable** sin `NullPointerException`.
  - Busca los seguidores del `actorUserId` en el índice y hace **fanout**: una notificación `REVIEW` por seguidor.
  - **Idempotente**: `notificationId = "REVIEW:" + reviewId` → `_id = followerId:REVIEW:reviewId` (el upsert por clave evita duplicados en redelivery).
  - Push STOMP a `/topic/notifications/{followerId}` (el destinatario, no el actor).

### Verificación de la Fase 12

- Compilación Maven OK (`notification-service`, reactor `-am`).
- E2E con Docker (usuario verificado): A sigue a B → B publica reseña → notificación `REVIEW` para A en Mongo (`_id=A:REVIEW:<reviewId>`), visible en `GET /notifications`, y push STOMP al topic de A. Realizado por el usuario en navegador/API.

### Cierre de la Fase 12

- [x] Fase 12.1 — Índice de seguidores local + consumer follow/unfollow.
- [x] Fase 12.2 — Consumer `review.created` + fanout a seguidores, idempotente, con push STOMP.
- [x] Compilación OK.
- [x] E2E verificado por el usuario contra el stack Docker.
- [x] Actualizar ROADMAP + SESSION_STATE + commit.

### Fix rate-limit post-Fase 11 — `X-Forwarded-For` (identity-service) ✅

- `RateLimitFilter.getClientIp()` lee `X-Forwarded-For` (primer elemento de la lista) con fallback a `remoteAddr`, para que tras un proxy cada cliente tenga su bucket (antes todos compartían la IP del proxy).
- E2E verificado por el usuario: 5×`200` + 6ª `429` con `X-Forwarded-For: 203.0.113.7`; `200` de nuevo con `203.0.113.8` (no comparten bucket). Contenedor Redis = `booksocial-redis`.
- Nota de seguridad: sin proxy de confianza, `X-Forwarded-For` es spoofeable (anotado para producción).

---

## Fase 13 — Despliegue cloud con Terraform (GCP) ✅ Alcanzada (A + B)

**Objetivo**: probar el camino 1 (Cloud Run) sobre el backend. **Alcance A**: identity + gateway + Redis sidecar + Cloud SQL. **Alcance B**: user-service + book-service con MongoDB Atlas M0 + CloudAMQP como Mongo/RabbitMQ externos gratuitos. Documentado en `docs/GUIDE-INFRA.md`.

### Fase 13A — Cloud SQL + Registry + Cloud Run identity/gateway ✅ Completada

- `infrastructure/terraform/environments/dev/`: `provider.tf`, `variables.tf`, `main.tf`, `outputs.tf`, `.terraform.lock.hcl` (tfvars/tfstate/.terraform ignorados).
- Cloud SQL `booksocial-db` (POSTGRES_16, `db-f1-micro`, authorized `0.0.0.0/0`), BD + user; Artifact Registry `apps`; Cloud Run `identity` (con sidecar `redis:7-alpine` `command=["redis-server"]`) y `gateway`; IAM `allUsers` invoker.
- Lecciones: `SPRING_DATASOURCE_PASSWORD`, `SERVER_PORT=8080`, sidecar `redis-server` directo, `deletion_protection=false`, "inconsistent final plan" (reaplicar), bodies JSON con `curl.exe` se rompen en PowerShell.

### Fase 13B — user-service + book-service (Mongo Atlas + CloudAMQP) ✅ Completada

- Cuentas gratuitas: **MongoDB Atlas M0** (URI `mongodb+srv://.../booksocial?authSource=admin`) y **CloudAMQP** (plan lemur, URI `amqps://user:pass@host`).
- `variables.tf`: +`mongo_uri`, `rabbitmq_uri`, `google_books_api_key` (sensitive). `main.tf`: `locals` con regex de descomposición AMQP (host `[^/:]+`, puerto/vhost con `coalesce`), `google_cloud_run_v2_service` `user` y `book`, gateway con `USER_SERVICE_URI`/`BOOK_SERVICE_URI` reales, IAM ampliado a 4 servicios.
- Lecciones: Mongo URI **debe llevar la BD en el path** (`Database name must not be empty` → Error code 9); `/follows/{userId}/following|followers` (con path variable); POST sin body en Cloud Run → `411 Length Required`; `/actuator/health` `DOWN` por Atlas M0 en BD `local` (inocuo).

### Verificación de la Fase 13 (E2E en la nube)

- **A**: `POST /auth/register` directo a identity (201 + tokens), `POST /auth/login` vía gateway (200).
- **B**: register/login vía gateway (201/200); `GET /profiles/me` materializa perfil en Postgres; `POST /follows/6` (201) → `GET /follows/3/following` y `/follows/6/followers` listan el follow; `followersCount` 0→1; unfollow `DELETE /follows/6` (204) y reset; `GET /books/9780061120084` auto-importa desde Google Books (200).

### Cierre de la Fase 13

- [x] Fase 13A — Cloud SQL + Registry + Cloud Run identity/gateway + IAM.
- [x] Fase 13B — user-service + book-service con Atlas M0 + CloudAMQP + URIs reales en gateway.
- [x] `terraform validate`/`fmt` OK; apply OK.
- [x] E2E nube: auth, perfiles, follow/unfollow, auto-import libros.
- [x] Actualizar GUIDE-INFRA.md (3.6-3.7 + Apéndice A) + SESSION_STATE + ROADMAP.

Restan (siguientes pasos): review/shelf/social/notification en la nube (misma receta que B), OAuth2 Google real (`GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` + redirect_uri HTTPS), frontend (Bloque 4), estado remoto (`terraform backend "gcs"`).

> **Revisión posterior (límite del `db-f1-micro`)**: al desplegar los 8 servicios en Cloud Run, varios con datosource arrancando en paralelo agotan las conexiones del `db-f1-micro` → `FATAL: remaining connection slots are reserved` (SQLState `53300`). El flag `max_connections` **no es editable** en tier shared-core. Se **revirtió** el terraform para dejar **identity + gateway + book-service** (los que caben de forma fiable), verificado E2E (login 200, `GET /books/...` 200). user-service, review, shelf, social y notification quedan fuera de Cloud Run. Para desplegarlos hace falta subir el tier de Cloud SQL (ver `GUIDE-INFRA.md` 3.7 y Apéndice A).

---

## Fase 11 — Reset de contraseña + directorio People + perfil público y follow ❖ Completada

**Objetivo**: (1) recuperación de contraseña por email en identity + páginas Angular; (2) directorio de usuarios **People** (`/users`), perfil público (`/users/:id`) y botón de seguimiento en feed/People/perfil; (3) **eliminar el perfil sintético** para que no se inventen correos `user-{id}@booksocial.local`.

### Fase 11.1 — Reset de contraseña (identity + frontend) ✅ Completada

- `PasswordResetToken`: token aleatorio de 32 bytes (hex) enviado por email; en BD solo el **hash SHA-256**, `expiresAt` 30 min, `used`. `PasswordResetService.requestReset` **nunca revela** si el email existe (responde `200` igual); `resetPassword` valida hash+uso+caducidad y re-encodea BCrypt.
- `POST /auth/forgot-password` y `POST /auth/reset-password` (`permitAll`); errores `INVALID_TOKEN`/`EXPIRED_TOKEN`/`ALREADY_USED` en `GlobalExceptionHandler`.
- Mail: `spring-boot-starter-mail` + `spring.mail.*` y plantilla HTML `password-reset-email.html` con `{{RESET_URL}}`.
- Frontend: `/forgot-password` y `/reset-password` (lazy), enlace en login, métodos en `AuthService`, interceptor ignora ambos endpoints.

### Fase 11.2 — People + perfil público + follow ✅ Completada

- Backend: `GET /profiles/search?q=` (Mongo, displayName/email insensitive) en `ProfileController`/`ProfileService`.
- Frontend: `FollowService` con cache reactiva de `followingIds`; `FollowButton`; páginas `/users` (People con buscador) y `/users/:id` (perfil con pestañas seguidores/siguiendo); enlace People en nav; `authGuard`.
- **Materialización del perfil propio** en login/registro/OAuth2/refresh: `AuthService.applyToken` → `GET /profiles/me` para que los usuarios recién creados aparezcan en People.
- Proxy: `/api/users/me` con `pathRewrite` → `/users/me` (evita colisionar con la ruta SPA `/users`).

### Fase 11.3 — Eliminación del perfil sintético ✅ Completada

- `Profile.email` nullable. `getByUserId` devuelve un **read model transitorio** (`user-{id}`, `email:null`) **sin persistir** cuando no hay perfil; purga correos `@booksocial.local` legados en Mongo.
- `findOrCreateProfile` repara el perfil con el email real de identity cuando está vacío/sintético y deja `displayName` derivable del email.
- `toResponse`/`upsertReadModel` nunca exponen ni persisten correos sintéticos.

### Verificación de la Fase 11

- Compilación Maven de identity y user-service OK; `npm run build` **sin warnings** (i18n completo, **153 trans-units**).
- API verificado: `/profiles/me` → 200 con email real; `/profiles/search` → lista; `/profiles/5` (sin perfil) → `{"displayName":"user-5","email":null}` sin crear nada.
- Requiere recrear `booksocial-user` con imagen nueva (`docker compose build user-service` + `up -d --force-recreate`).

### Cierre de la Fase 11

- [x] Fase 11.1 — Reset de contraseña (backend + frontend + email).
- [x] Fase 11.2 — People, perfil público, follow y materialización del perfil.
- [x] Fase 11.3 — Eliminación del perfil sintético.
- [x] i18n: 49 trans-units nuevas traducidas en es/pt (153 en total).
- [x] Commit por bloque: `7e8dee3` (reset), `cb239f3` (People/follow/perfiles), `3e016f7` (i18n).
- [x] E2E manual en navegador: login con Test One/Test Two → People muestra ambos y se puede seguir desde feed/perfil.
- [x] Actualizar este documento al cerrar la fase.

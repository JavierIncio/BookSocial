# BookSocial — Guía de Desarrollo (Infraestructura)

Guía de la **infraestructura** de BookSocial: entorno local con Docker Compose, contenerización de los microservicios, y despliegue cloud con Terraform en Google Cloud Platform (GCP).

> El contenido relativo a Docker (compose, Dockerfiles, CI) se ha extraído de `GUIDE.md` y se amplía aquí con el despliegue cloud. Para el backend Java ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md); para el frontend Angular ver [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md).

---

## Cómo usar esta guía

La guía está organizada en **bloques cronológicos**: cada bloque se construye sobre el anterior, como un curso progresivo.

- **Bloque 0** — Infraestructura local con Docker Compose (bases de datos y broker).
- **Bloque 1** — Contenerización: Dockerfiles multi-stage, `.dockerignore` y CI.
- **Bloque 2** — Operación local: arranque, logs, redespliegue y herramientas.
- **Bloque 3** — Despliegue cloud con Terraform (GCP): Cloud SQL, Artifact Registry, Cloud Run, IAM y verificación end-to-end.

**Nivel de detalle**: se incluye el código real del proyecto (compose, Dockerfiles, `.tf`) con explicaciones de _por qué_ se toma cada decisión y una sección final de errores típicos con su solución (son los que se encontraron al construir la fase).

---

## Tabla de contenidos

| Bloque                                                                                     | Tema                                              | Fase       |
| ------------------------------------------------------------------------------------------ | ------------------------------------------------- | ---------- |
| [0. Infraestructura local Docker](#bloque-0--infraestructura-local-docker-compose)         | Compose, healthchecks, volúmenes, puertos         | —          |
| [0.0. Qué es Docker](#00--qué-es-docker-apuntes-para-principiantes)                        | Imágenes, contenedores, volúmenes, puertos, Dockerfile | —      |
| [1. Contenerización y CI](#bloque-1--contenerización-y-ci)                                 | Dockerfiles, `.dockerignore`, compose ampliado, CI | Fase 1    |
| [1.1.1. Ciclo de vida Maven](#111--el-ciclo-de-vida-de-maven-y-qué-hace-mvnw--b--pl-identity-service--am-package--dskiptests) | Fases de Maven, `package`, `-pl`/`-am`/`-DskipTests` | Fase 1 |
| [2. Operación local](#bloque-2--operación-local)                                            | Comandos útiles, logs, redespliegue               | Referencia |
| [3. Deploy cloud con Terraform](#bloque-3--despliegue-cloud-con-terraform-fase-13)         | GCP, Cloud SQL, Registry, Cloud Run, IAM, Mongo/Rabbit externos | Fase 13    |
| [A. Errores típicos del Bloque 3](#apéndice-a--errores-típicos-del-despliegue-cloud)      | Troubleshooting con solución directa              | Referencia |

---

# Bloque 0 — Infraestructura local (Docker Compose)

## 0.0 — ¿Qué es Docker? (apuntes para principiantes)

> Si nunca has trabajado con Docker, lee este apartado primero. Es la base del Bloque 0 y del Bloque 1: todo lo que verás (compose, Dockerfiles, contenedores, imágenes) cae dentro de los conceptos que se explican aquí.

**El problema que resuelve Docker**: las bases de datos (PostgreSQL, MongoDB, RabbitMQ, Redis) y las aplicaciones dependen de versiones exactas de ejecutables, bibliotecas y configuración. Instalarlas "a mano" en cada máquina da errores distintos según el sistema. Docker **empaqueta una aplicación y todo lo que necesita** en una unidad reproducible que corre igual en cualquier equipo.

**Los conceptos esenciales (con la analogía del "pedido")**

1. **Imagen** (`image`) — la *plantilla o receta*. Es un paquete de solo-lectura con el código, el runtime, las librerías y la config. Casos: `postgres:16-alpine`, `mongo:8.0`, `redis:7-alpine`, `eclipse-temurin:21-jre`. Se comparte y versiona. En Bloque 0 usamos **imágenes oficiales**; en Bloque 1 las **construimos nosotros** (Dockerfiles).

   - Las imágenes se organizan por **`repositorio:etiqueta`** (repository:tag). `postgres:16-alpine` = repositorio `postgres`, tag `16-alpine`. El tag suele ser la versión; es lo que te asegura "esta imagen concreta".
   - `-alpine` es una variante **ultraligera** (basada en Alpine Linux): ocupa menos. Ideal para desarrollo.

2. **Contenedor** (`container`) — una *instancia en ejecución* de una imagen. La imagen es el plano; el contenedor es el barco corriendo. Puedes tener **muchos contenedores de la misma imagen**. Los contenedores son efímeros: se crean, corren y se destruyen; los **datos** se guardan aparte (volúmenes, ver abajo).

3. **Volumen** (`volume`) — el **almacenamiento persistente** que sobrevive a la vida del contenedor. Si un contenedor se elimina y se recrea, sin volumen se pierden sus datos. Con volumen (`volumes: postgres-data:/var/lib/postgresql/data`), los datos quedan guardados en el host y sobreviven.

4. **Puerto publicado** (`ports: "5432:5432"`) — el formato es `"HOST:CONTENEDOR"`. Expone el puerto del contenedor hacia la máquina que ejecuta Docker. Así un microservicio corriendo en el host se conecta a `localhost:5432` y llega al PostgreSQL del contenedor.

5. **Red / nombre de contenedor** — los servicios del mismo `docker compose` comparten una red interna y se llaman **por su nombre de servicio** (`postgres`, `redis`, `identity-service`) en vez de por IP. Por eso `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/...` funciona: dentro de la red Docker, `postgres` es el host.

**Dockerfile: cómo se construye una imagen propia (Bloque 1)**

Un `Dockerfile` es una receta que **dice paso a paso cómo construir** la imagen. Cada instrucción crea una **capa** del sistema de ficheros; Docker cachea capas, de modo que reconstruir es incremental (ver 2.2).

```dockerfile
FROM eclipse-temurin:21-jre          # parte de una base ya preparada
WORKDIR /app                         # carpeta de trabajo dentro de la imagen
COPY app.jar app.jar                 # copia el jar hacia la imagen
ENTRYPOINT ["java", "-jar", "app.jar"]  # qué comando ejecuta al arrancar el contenedor
```

- **`FROM ... AS nombre`**: define una **etapa** (stage). En un Dockerfile multi-stage hay varios `FROM`, y el último define la imagen final. Las etapas intermedias se descartan salvo lo que copies de ellas → imagen final pequeña (ver 1.1).
- **`COPY --from=build <origen> <destino>`**: copia un fichero **desde otra etapa** (`build`) a la imagen final. Es el mecanismo clave del multi-stage: compilas con herramientas gordas (Maven) y solo copias el artefacto (el JAR) a la imagen final mínima.
- **`EXPOSE`**: documenta el puerto que la app escucha (no lo publica; es informativo).
- **`RUN`**: ejecuta un comando durante la construcción (p.ej. instalar `curl`).

**Healthcheck: por qué `healthy`/`unhealthy`**

Un contenedor puede estar **arrancado** (Docker lo levantó) pero su servicio interno **todavía no listo** (PostgreSQL inicializando, la JVM cargando). El `healthcheck` interroga al servicio interno (p.ej. `pg_isready`) para saber cuándo está realmente preparado. `docker compose ps` lo muestra en la columna STATUS (`healthy | unhealthy | starting`), y es la base del `depends_on: condition: service_healthy` (un servicio no arranca hasta que su dependencia está sana). Ver [0.2].

**Docker Compose: la "orquesta" de la infraestructura**

Docker solo corre **un** contenedor. **Compose** (`infrastructure/docker-compose.yml`) define y levanta **varios contenedores relacionados** con una sola orden (`docker compose up -d --build`), gestionando red, volúmenes, healthchecks y orden de arranque (`depends_on`). En Bloque 0 Compose levanta solo la infraestructura (BBDD + broker); en Bloque 1 añade los microservicios contenerizados.

**El término "contexto de build"** (`build.context`)

Al construir una imagen, Docker manda **una carpeta íntegra** al demonio (el *contexto*, p.ej. `context: ..` → la raíz del monorepo). Solo puede copiar lo que está dentro de ese contexto (`COPY . .`). El `.dockerignore` excluye carpetas del contexto para que no viajen (véase `.dockerignore` en 1.2): así la imagen no manda `target/`, `node_modules/`, `.env` ni secretos.

**El porqué de `docker compose ... up -d --build`**

- `up`: crea e inicia los servicios definidos.
- `-d` (detached): arranca en segundo plano y te devuelve la consola (no bloquea).
- `--build`: reconstruye las imágenes propias **antes** de levantar los contenedores (imprescindible tras cambiar código en un microservicio, ver 2.2).

## 0.1 — Estructura de carpetas

El monorepo separa el **código de negocio** de la **infraestructura**:

```
  frontend/
  gateway/
  identity-service/
  user-service/
  book-service/
  review-service/
  shelf-service/
  social-service/
  notification-service/
  infrastructure/
    docker-compose.yml
    terraform/
  docs/
```

- Cada microservicio tiene **aislamiento total**: su propio código, `pom.xml`, `.env` y `Dockerfile`.
- `infrastructure/` contiene todo aquello que pertenece a la infraestructura y no al código de negocio: **Docker Compose**, **Terraform** y configuraciones relacionadas.
- `docs/` contiene el roadmap y el estado de cada sesión.

El `.gitignore` raíz tiene un bloque de infraestructura:

```gitignore
# --- Infraestructura ---
*.tfstate
*.tfstate.backup
*.tfvars
.terraform/
```

Reglas críticas:

- `*.tfvars` y `*.tfstate` **nunca se commitean**: contienen secretos (`db_password`, `jwt_secret`) y el estado de Terraform (también sensible). El `.terraform.lock.hcl` **sí** se commitea (fija las versiones de los providers para que todo el equipo use las mismas).
- La política del proyecto es que **ningún dato sensible viva en Git**: los `.env` de cada módulo se cargan en runtime y nunca entran en las imágenes Docker.

## 0.2 — `infrastructure/docker-compose.yml`

El compose inicial define únicamente la infraestructura necesaria en local:

- **PostgreSQL 16** (datos relacionales — command side del CQRS).
- **MongoDB 8** (lecturas CQRS — query side).
- **RabbitMQ 4** (broker de eventos asíncronos).
- **Redis 7** (rate-limiting distribuido, añadido en la Fase 11).

```yml
name: booksocial

services:
  postgres:
    image: postgres:16-alpine
    container_name: booksocial-postgres
    environment:
      POSTGRES_USER: booksocial
      POSTGRES_PASSWORD: booksocial
      POSTGRES_DB: booksocial
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U booksocial"]
      interval: 5s
      timeout: 5s
      retries: 10

  mongodb:
    image: mongo:8.0
    container_name: booksocial-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: booksocial
      MONGO_INITDB_ROOT_PASSWORD: booksocial
      MONGO_INITDB_DATABASE: booksocial
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping')"]
      interval: 5s
      timeout: 5s
      retries: 10

  rabbitmq:
    image: rabbitmq:4-management
    container_name: booksocial-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: booksocial-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  postgres-data:
  mongodb-data:
  rabbitmq-data:
```

> Si no se definen, el usuario y contraseña por defecto de RabbitMQ son `guest / guest` (consola web: [http://localhost:15672](http://localhost:15672)).

### Conceptos clave

**Volúmenes**: permiten que los datos sobrevivan al ciclo de vida del contenedor. PostgreSQL se elimina y se recrea, pero los datos persisten en `postgres-data`. Sin volumen, al eliminar el contenedor se pierde la base de datos.

**Healthchecks**: no comprueban que el contenedor arrancó, sino que el **servicio interno está listo** (PostgreSQL puede estar arrancado mientras inicializa). Se consulta el estado con `docker compose ps` → `healthy | unhealthy | starting`. Este mecanismo se reutiliza con `depends_on: condition: service_healthy` para que los microservicios esperen a sus dependencias.

**Puertos publicados**: se publican del contenedor al host (`"5432:5432"`). Mientras los microservicios se ejecutan en la máquina host se conectan a `localhost:5432`, `localhost:27017`, `localhost:5672` y `localhost:6379`.

---

# Bloque 1 — Contenerización y CI

Hasta el Bloque 0 los servicios se ejecutan directamente en el host. Este bloque los contenedoriza y amplía el pipeline de CI para verificar todo en un entorno aislado: transición de desarrollo local a despliegue consistente.

## 1.1 — Dockerfiles multi-stage

Cada servicio tiene su propio `Dockerfile` con **dos etapas**. El contexto de build es **la raíz del monorepo** (porque necesita el `pom.xml` padre y el wrapper `mvnw`):

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -B -pl identity-service -am package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/identity-service/target/identity-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **Stage de build**: una imagen de Maven compila solo ese módulo. `-pl <módulo>` ejecuta solo ese proyecto; `-am` (also make) compila también los proyectos de los que depende (el parent `booksocial-parent`); `-DskipTests` acelera el build (los tests ya corren en CI).
- **Stage de runtime**: parte de una imagen Java mínima (`21-jre`, sin Maven, sin código fuente) y solo copia el JAR → imagen **pequeña**. Se instala `curl` porque el healthcheck de Docker Compose lo usa.
- El nombre del JAR no es casual: Maven lo genera como `<artifactId>-<version>.jar`. Con `version = 0.1.0-SNAPSHOT` en el parent POM, cada módulo produce `X-0.1.0-SNAPSHOT.jar`.

### 1.1.1 — El ciclo de vida de Maven y qué hace `./mvnw -B -pl identity-service -am package -DskipTests`

**Qué es Maven**: una herramienta de **build de Java** que lee el `pom.xml` de cada módulo y sabe cómo compilar, empaquetar y gestionar dependencias. Tiene un **ciclo de vida** predefinido: una **secuencia fija de fases** que se ejecuta en orden. Invocar una fase ejecuta **todas las anteriores**.

**Las fases del ciclo de vida (la cadena que cuenta aquí)**:

| Fase | Qué hace |
| --- | --- |
| `validate` | Comprueba que el proyecto es correcto (estructura, dependencias). |
| `compile` | Compila el código fuente (`.java` → `.class`). |
| `test` | Compila y ejecuta los **tests** (`src/test/java`). |
| `package` | Empaqueta lo compilado → genera el **artefacto** (`.jar`/`.war`). |
| `verify`/`install`/`deploy` | Pasos posteriores (verificación, instalación local, despliegue). |

Como las fases son **acumulativas**, decir `mvn package` = hacer `validate + compile + test + package`. Es decir, **Maven ya corre los tests durante `package`** (por eso `-DskipTests` "salta" algo que por defecto ocurre).

**Desglose argumento por argumento de la línea del Dockerfile**:

```
./mvnw -B -pl identity-service -am package -DskipTests
```

- **`./mvnw`** — el **Maven Wrapper**: un script que descarga una versión concreta de Maven automáticamente. Garantiza que **todos** (local, CI, Docker) usen la **misma versión**, sin instalar Maven a mano. Vive en la raíz del monorepo. `./` lo ejecuta desde la carpeta actual.
- **`-B`** (batch mode) — desactiva el progreso/animation y los `colorcodes` de Maven: salida limpia y estable para CI/scripts/Docker. Sin él, Maven asume que hay una terminal interactiva (puede dar problemas en un build no interactivo).
- **`-pl identity-service`** (project-list) — dice: *"compila **solo** este proyecto",* **no todo el monorepo**. Acelera el build: en un monorepo con 7 módulos, construir solo el que toca es mucho más rápido.
- **`-am`** (also-make) — imprescindible compañero de `-pl`: *"y también los proyectos de los que **depende**"*. Mientras `-pl` limita el alcance, `-am` lo re-expande **hacia arriba** para incluir los módulos-padre/dependencias necesarios (aquí, el parent `booksocial-parent`). El módulo `identity-service` hereda de `booksocial-parent`: para construirlo, Maven necesita compilar/escanear antes su POM padre.
- **`package`** — la fase a ejecutar (ver tabla): compila, testea y genera el JAR `identity-service-0.1.0-SNAPSHOT.jar` en `target/`.
- **`-DskipTests`** — *"compila los tests pero no los ejecutes"*. Es **distinto de `-Dmaven.test.skip=true`** (que ni siquiera compila los tests). `-DskipTests` los compila (valida que siguen compilando) pero **no los corre** → build más rápido. Los tests "reales" se ejecutan en la **CI** (no interesa repetirlos en cada `docker build`).

**Por qué el `RUN` combina `chmod +x mvnw &&`**: el script `mvnw` necesita permiso de ejecución, pero el COPY de Docker puede no preservarlo según el sistema (Windows). Se le da `+x` justo antes de lanzarlo.

**De dónde sale el nombre del JAR** (`identity-service-0.1.0-SNAPSHOT.jar`): Maven lo nombra como `<artifactId>-<version>.jar`. El `artifactId` (`identity-service`) y el `version` (`0.1.0-SNAPSHOT`) vienen del `pom.xml` (heredados del parent). Por eso, al copiar el JAR a la imagen final, la ruta **no es casual**: refleja exactamente esos dos campos del POM.

El gateway es idéntico, cambiando el módulo (`-pl gateway`) y el JAR copiado (`gateway-0.1.0-SNAPSHOT.jar`).

## 1.2 — `.dockerignore` raíz

```gitignore
**/target/
**/node_modules/
.angular/
.git/
.idea/
*.iml
.vscode/
**/.env
**/.env.*
frontend/
infrastructure/
docs/
.github/
```

Protege dos cosas: **tamaño** del contexto de build (no entran `target/`, `node_modules/`, `.git/`) y, sobre todo, **secretos** — los `.env` de los módulos **nunca entran en la imagen**. Los secretos se inyectan en runtime.

## 1.3 — `docker-compose.yml` ampliado

A los servicios de infraestructura se añaden las aplicaciones (ejemplo identity-service + gateway):

```yaml
identity-service:
  build:
    context: ..
    dockerfile: identity-service/Dockerfile
  container_name: booksocial-identity
  ports:
    - "8081:8081"
  env_file:
    - ../identity-service/.env   # secretos desde el host, fuera de la imagen
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/booksocial
    SPRING_REDIS_HOST: redis
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
    interval: 10s
    timeout: 5s
    retries: 12
    start_period: 30s

gateway:
  build:
    context: ..
    dockerfile: gateway/Dockerfile
  container_name: booksocial-gateway
  ports:
    - "8080:8080"
  env_file:
    - ../gateway/.env
  environment:
    IDENTITY_SERVICE_URI: http://identity-service:8081
  depends_on:
    identity-service:
      condition: service_healthy
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
```

Tres patrones importantes:

- **`build.context: ..`**: el contexto es la raíz del monorepo (necesita el `pom.xml` padre y el wrapper); el `dockerfile` se indica por ruta relativa.
- **`env_file` + `environment`**: los secretos (`APP_JWT_SECRET`, `GOOGLE_CLIENT_*`) llegan del `.env` del módulo; la **configuración no sensible** (URLs de otros contenedores) se sobreescribe con `environment`. Así el mismo JAR funciona dentro de la red Docker usando los nombres de contenedor (`postgres`, `redis`, `identity-service`) como host.
- **`depends_on ... service_healthy`**: el gateway no arranca hasta que identity-service responde a `/actuator/health`, y este no arranca hasta que Postgres y Redis están sanos.

Arranque completo con un solo comando:

```powershell
docker compose -f infrastructure/docker-compose.yml up -d --build
```

## 1.4 — CI ampliado (`ci.yml`)

Dos jobs en paralelo:

**Job `build` (backend)** — con un servicio PostgreSQL de soporte (cubre la dependencia de los tests de identity) y los secrets inyectados como variables de entorno:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    env:
      APP_JWT_SECRET: ${{ secrets.APP_JWT_SECRET }}
      GOOGLE_CLIENT_SECRET: ${{ secrets.GOOGLE_CLIENT_SECRET }}
      GOOGLE_CLIENT_ID: ${{ secrets.GOOGLE_CLIENT_ID }}
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: booksocial
          POSTGRES_USER: booksocial
          POSTGRES_PASSWORD: booksocial
        ports:
          - 5432:5432
```

> Nota CI: el test de contexto del identity-service fallaba en CI porque `RateLimitService` conecta a Redis de forma **eager** y CI no tiene Redis. Se mockea con `@MockitoBean RateLimitService rateLimitService;` en `IdentityServiceApplicationTests`.

**Job `frontend`** — compila el Angular con Node 24:

```yaml
frontend:
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: frontend
  steps:
    - uses: actions/checkout@v5
    - uses: actions/setup-node@v5
      with:
        node-version: "24"
        cache: npm
        cache-dependency-path: frontend/package-lock.json
    - run: npm ci
    - run: npm run build
```

---

# Bloque 2 — Operación local

Todos los comandos se ejecutan desde la **raíz del monorepo** y usan el compose de `infrastructure/docker-compose.yml`.

## 2.1 — Arrancar, parar y estado

```powershell
docker compose -f infrastructure/docker-compose.yml up -d          # arrancar todo (respeta depends_on + healthchecks)
docker compose -f infrastructure/docker-compose.yml down           # parar todo (conserva volúmenes)
docker compose -f infrastructure/docker-compose.yml ps             # estado + health de cada contenedor
```

## 2.2 — Redesplegar un servicio tras cambiar su código

```powershell
docker compose -f infrastructure/docker-compose.yml build book-service   # reconstruye la imagen
docker compose -f infrastructure/docker-compose.yml up -d book-service   # recrea el contenedor con la imagen nueva
```

- El build es **incremental por capas**: al cambiar código fuente, Docker invalida la capa `COPY` y recompila solo lo necesario.
- Si sospechas que el contenedor sirve **código antiguo** (un bug corregido sigue apareciendo), fuerza rebuild completo: `docker compose build --no-cache <servicio>`.

## 2.3 — Logs

```powershell
docker logs booksocial-book --tail 100          # últimas 100 líneas
docker logs booksocial-book -f                  # seguir en vivo
docker logs booksocial-book --since 10m         # últimos 10 minutos
docker logs booksocial-gateway 2>&1 | Select-String ERROR   # filtrar errores
```

## 2.4 — Herramientas de inspección

```powershell
# RabbitMQ: colas, exchanges, mensajes → http://localhost:15672 (guest / guest)

# Mongo: read models
docker exec booksocial-mongodb mongosh -u booksocial -p booksocial --authenticationDatabase admin booksocial --eval "db.shelves.find().limit(3)"

# Postgres: tablas de comandos
docker exec booksocial-postgres psql -U booksocial -d booksocial -c "SELECT isbn, title, author_id FROM books LIMIT 5;"

# Redis: claves del rate-limiting (bucket por IP)
docker exec booksocial-redis redis-cli keys "*"
docker exec booksocial-redis redis-cli del "203.0.113.7"   # resetear un bucket de rate-limit
```

---

# Bloque 3 — Despliegue cloud con Terraform (Fase 13)

Despliegue del backend en **Google Cloud Platform** con **Terraform**, usando los servicios con **nivel gratis** de GCP: **Cloud SQL** (Postgres free tier), **Artifact Registry**, **Cloud Run** y **Cloud Shell/IAM**. El objetivo es probar el **camino 1** (Cloud Run). **Alcance A**: identity + gateway + Redis sidecar + Cloud SQL (Fase 13, primera parte). **Alcance B**: user-service + book-service con **MongoDB Atlas M0** y **CloudAMQP** como MongoDB/RabbitMQ externos gratuitos (segunda parte del tutorial).

> El despliegue se hizo como **aprendizaje paso a paso**: se generan recursos reales (nivel free tier), se validan y —cuando aplica— se importan a Terraform las piezas que el plan no llegó a gestionar.

## 3.0 — Preparación y conceptos Terraform

> Si es la **primera vez que usas Terraform**, lee primero este apartado (3.0.1). Es la base sobre la que se apoya todo el bloque: entender qué es el _estado_, un _provider_ y el ciclo `init → plan → apply` convierte el resto de la guía en sencillo.

### 3.0.1 — Terraform desde cero (apuntes para principiantes)

**¿Qué es Terraform?**

Terraform es una herramienta de **Infraestructura como Código (IaC)**: describes los recursos de la nube (máquinas, bases de datos, servicios, permisos) en ficheros de texto (_`.tf`_) y Terraform los **crea, modifica o elimina** hacia el proveedor (aquí, **Google Cloud**). En vez de pulsar botones en la consola de GCP, "declaras" lo que quieres y Terraform se encarga de materializarlo y mantenerlo en el tiempo.

Filosofía clave: **declarativo, no imperativo**. No dices "crea una instancia, luego añade una BD, luego un usuario"; dices "quiero una instancia con una BD y un usuario, con estos parámetros" y Terraform calcula el **plan** de acciones necesarias para llegar a ese estado.

**Los 3 elementos mentales para entender todo Terraform**

1. **El estado (`terraform.tfstate`)**: un **fichero JSON** donde Terraform guarda la "fotografía" de todo lo que ha creado (cada recurso, sus IDs reales en GCP y sus atributos). Es la memoria de Terraform: cuando vuelves a ejecutar `plan`, compara el **estado** (lo que existe) contra tu **configuración** (lo que quieres) para decidir cambios. Sin estado, Terraform no sabe qué crear/eliminar — de ahí que sea tan importante **no borrarlo** y protegerlo (se ignora en Git, ver `.gitignore`). Si se pierde, Terraform "pierde la cabeza" y crearía duplicados.

2. **El provider (`hashicorp/google`)**: el "puente" entre Terraform y un proveedor de nube. Define con qué API de GCP hablar, cómo autenticarse y qué tipos de recurso existen (`google_sql_database_instance`, `google_cloud_run_v2_service`, ...). Se descarga con `terraform init` y su versión se fija en el `.terraform.lock.hcl`.

3. **Los recursos (`resource "tipo" "nombre"`)**: cada bloque HCL que describe una pieza de infraestructura. Su **dirección** completa es `tipo.nombre` (p.ej. `google_sql_database_instance.postgres`) y es como Terraform se refiere a él en comandos (`terraform show google_sql_database_instance.postgres`), importaciones y referencias.

**La sintaxis HCL (HashiCorp Configuration Language)**

```hcl
resource "google_sql_database_instance" "postgres" {   # "dirección" = google_sql_database_instance.postgres
  name             = "booksocial-db"                    # argumento de tipo string
  database_version = "POSTGRES_16"
  settings {                                            # bloque anidado
    tier = "db-f1-micro"
  }
}
```

- **`resource`**: palabra clave → describe un recurso a crear.
- **`"google_sql_database_instance"`**: el **tipo** de recurso (definido por el provider).
- **`"postgres"`**: el **nombre lógico** local (solo importa dentro de tu configuración, no es el nombre real en GCP). El **nombre real** en GCP es el del argumento `name = "booksocial-db"`.
- **Argumentos** (`name = ...`), **bloques** anidados (`settings { ... }`). El orden de los argumentos dentro del bloque no importa.
- Puedes usar **expresiones** que se evalúan en tiempo de plan, como `"jdbc:postgresql://${var.db_host}:5432/booksocial"` (interpolación `${...}`) o `${var.project_id}`.

**Variables: `variables.tf`, `terraform.tfvars`, `sensitive`**

Separamos la **definición** (qué variable existe) de los **valores** (cuánto vale). Esto permite reutilizar la configuración sin exponer secretos:

```hcl
# variables.tf — define la variable (tipo, descripción, si es sensible)
variable "db_password" {
  type        = string
  sensitive   = true   # no se mostrará en plan/output
}

# terraform.tfvars — aporta el valor (NO se commitea)
db_password = "mi_secreto"
```

- Sin `sensitive = true`, Terraform **imprimiría el secreto en pantalla** durante `plan`/`apply`. Con él, lo enmascara como `(sensitive value)`.
- Los valores se pueden aportar por tres vías (en orden de prioridad): flags `-var`, el fichero `terraform.tfvars` (o `*.auto.tfvars`), o valores por defecto en `variables.tf`.
- Cuando una variable **no tiene default y no se aporta valor**, `plan` la pide por teclado o falla. Esto fuerza a definirla en `tfvars`.
- ⚠️ `terraform.tfvars` está en el `.gitignore`: no se sube a Git porque contiene secretos. El `.terraform.lock.hcl` **sí** se commitea (bloquea versiones de provider para reproducibilidad).

**Dependencias implícitas y explícitas**

Terraform **ordena solo** las creaciones siguiendo las referencias:

```hcl
resource "google_sql_database" "booksocial" {
  name     = "booksocial"
  instance = google_sql_database_instance.postgres.name   # referencia al recurso padre
}
```

- **Implícita** (más común y recomendada): al escribir `google_sql_database_instance.postgres.name`, Terraform entiende que la BD depende de la instancia y la crea **después**. Es la forma idónea de expresar orden.
- **Explícita**: `depends_on = [google_sql_database_instance.postgres]` cuando la relación no se puede deducir de un argumento pero el orden importa.

**`terraform init → plan → apply`: la rutina que no falla**

```
terraform init    # 1ª vez y cada vez que cambian providers/modules. Descarga providers, prepara backend.
terraform plan    # calcula los cambios (dry-run, NO aplica). Muestra + crear, ~ modificar, - eliminar.
terraform apply   # aplica el plan. Pide "yes" (o -auto-approve para no preguntar).
terraform output  # muestra las salidas definidas en outputs.tf (gateway_url, identity_url...).
```

- **`plan` es gratis y sin riesgo**: te dice exactamente qué hará `apply`, antes de tocar nada. **Debe darte total confianza** antes de aplicar.
- `apply` también recalcula y muestra el plan; **siempre revisa** qué va a crear/modificar/eliminar (`+`,`~`,`-`) antes de escribir `yes`.

**`terraform import` y por qué existe**

`terraform import <dirección> <id-en-gcp>` **adopta** un recurso que ya existe en GCP pero que aún no está en el estado de Terraform. Sirve para "ponerse al día": si creaste algo a mano (o Terraform se quedó colgado y ya está creado en GCP), importas su ID real y Terraform pasa a gestionarlo desde entonces, sin duplicarlo. En esta guía se usó dos veces (Cloud SQL y para adoptar piezas). **Importante**: importar no modifica el recurso, solo introduce su estado; luego un `plan` mostrará si hay diferencias que corregir.

**`terraform untaint` y `terraform destroy`**

- `untaint <dirección>`: si un `apply` falló a medias, Terraform marca el recurso como _tainted_ (sospechoso) y en el siguiente plan lo **reescribe**. `untaint` quita esa marca para _forzar_ la recreación intencionada.
- `destroy`: elimina **todo** lo gestionado (con `deletion_protection = false`). ⚠️ Destructivo e irreversible (borra BDs, datos...). Nunca en producción.

**El bucle total del Bloque 3 (visión global)**

Todos los `apply`/`plan` de esta guía siguen este flujo: editas `.tf` → `terraform validate` (sintaxis) → `terraform plan` (qué hará) → `terraform apply` (lo hace) → verificas con `curl`/`gcloud` que el servicio responde. Cuando cambias solo valores (`tfvars`), el plan muestra `~` (modificaciones in-place) y reaplicar es suficiente.

**Referencia rápida de comandos usados en el Bloque 3**

| Comando | Qué hace |
| --- | --- |
| `terraform init` | Descarga providers, prepara `.terraform/` y el estado local |
| `terraform validate` | Valida sintaxis y estructura de los `.tf` (no conecta a GCP) |
| `terraform plan` | Calcula los cambios (dry-run) |
| `terraform apply` | Ejecuta los cambios (pide confirmación; `-auto-approve` no la pide) |
| `terraform apply -auto-approve` | Idem sin preguntar (útil en scripts) |
| `terraform output` / `terraform output gateway_url` | Muestra las salidas definidas en `outputs.tf` |
| `terraform import <addr> <gcp-id>` | Adopta un recurso ya existente en GCP hacia el estado |
| `terraform show <addr>` | Inspecciona un recurso del estado |
| `terraform untaint <addr>` | Quita la marca "tainted" para gestionar una recreación |
| `terraform state list` | Lista todos los recursos del estado |
| `terraform destroy` | Elimina todo lo gestionado (¡cuidado!) |
| `terraform fmt` | Formatea los `.tf` con estilo estándar |

### Estructura de carpetas

```
infrastructure/terraform/environments/dev/
  provider.tf        # terraform block + provider google
  variables.tf       # declaración de variables
  main.tf            # recursos (Cloud SQL, registry, Cloud Run, IAM)
  outputs.tf         # salidas (project_id, gateway_url, identity_url)
  terraform.tfvars   # valores reales + secretos (NO se commitea)
  .terraform.lock.hcl  # SÍ se commitea (fija versiones de providers)
```

Los ficheros `.tf` usan la sintaxis HashiCorp Configuration Language (HCL). El ciclo de vida básico se ha detallado en [3.0.1](#301--terraform-desde-cero-apuntes-para-principiantes); en resumen:

```powershell
terraform init     # 1ª vez: descarga providers y prepara el estado
terraform plan     # calcula el plan (sin aplicar): `+` crear, `-` eliminar, `~` cambiar
terraform apply    # aplica el plan (pide confirmación o `-auto-approve`)
terraform output   # muestra las salidas definidas en outputs.tf
terraform import <address> <id>  # adopta un recurso ya existente en el estado
terraform untaint <address>      # quita la marca "tainted" tras un apply fallido
```

### Provisionar el acceso a GCP

1. Crear el proyecto y vincular billing (consola GCP).
2. Instalar Google Cloud SDK y autenticarse con tu cuenta:

```powershell
gcloud auth login --no-browser        # --no-browser si el puerto local está ocupado
gcloud config set project booksocial-infra
gcloud auth application-default login # genera el ADC que usará Terraform (application_default_credentials.json)
```

3. Habilitar las APIs necesarias:

```powershell
gcloud services enable run.googleapis.com sqladmin.googleapis.com artifactregistry.googleapis.com compute.googleapis.com iam.googleapis.com cloudresourcemanager.googleapis.com
```

### `provider.tf`

```hcl
terraform {
  required_version = ">= 1.9"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}
```

- `required_version` y `required_providers`: fijan versiones reproducibles.
- El provider usa la credencial ADC (Application Default Credentials) generada con `gcloud auth application-default login`.

### `variables.tf`

```hcl
variable "project_id" {
  description = "ID del proyecto GCP"
  type        = string
}

variable "region" {
  description = "Región donde se despliega"
  type        = string
  default     = "europe-west1"
}

variable "db_password" {
  description = "Contraseña del usuario de la BD (la pasamos con -var o tfvars)"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "APP_JWT_SECRET compartido por los servicios"
  type        = string
  sensitive   = true
}

variable "db_host" {
  description = "IP pública del Cloud SQL"
  type        = string
}

variable "frontend_url" {
  description = "URL a la que apunta FRONTEND_URL (de momento localhost, luego el de Cloud Run)"
  type        = string
  default     = "http://localhost:4200"
}

variable "mongo_uri" {
  description = "URI de MongoDB Atlas (con el nombre de la BD en el path)"
  type        = string
  sensitive   = true
}

variable "rabbitmq_uri" {
  description = "URI de CloudAMQP (amqps://user:pass@host[:port]/[vhost])"
  type        = string
  sensitive   = true
}

variable "google_books_api_key" {
  description = "API key de Google Books (book-service)"
  type        = string
  sensitive   = true
  default     = ""
}
```

> `sensitive = true` evita que el valor se muestre en el plan/outputs. Los valores se pasan con `terraform.tfvars` (ignorado por Git).

### `terraform.tfvars` (no se commitea)

```hcl
project_id = "booksocial-infra"
region     = "us-central1"
db_host    = "34.59.171.207"

db_password = "TU_CONTRASEÑA_BD"
jwt_secret  = "TU_SECRETO_JWT_BASE64URL"
```

> **Ojo**: `APP_JWT_SECRET` debe ser una cadena **base64url sin espacios**. Si contiene un espacio, `JwtService` lanza `Illegal base64 character: ' '` al arrancar el contenedor.

## 3.1 — Cloud SQL Postgres (free tier)

El tier `db-f1-micro` es el más barato de Cloud SQL (casi gratuito para desarrollo). Definición en `main.tf`:

```hcl
resource "google_sql_database_instance" "postgres" {
  name             = "booksocial-db"
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = "db-f1-micro"
    disk_size         = 20
    disk_type         = "PD_SSD"
    availability_type = "ZONAL"

    ip_configuration {
      authorized_networks {
        name  = "public-access"
        value = "0.0.0.0/0"
      }
      # Para pruebas de aprendizaje. NO usar en producción.
    }

    database_flags {
      name  = "cloudsql.iam_authentication"
      value = "off"
    }
  }

  deletion_protection = false
}

resource "google_sql_database" "booksocial" {
  name     = "booksocial"
  instance = google_sql_database_instance.postgres.name
}

resource "google_sql_user" "booksocial_user" {
  name     = "booksocial"
  instance = google_sql_database_instance.postgres.name
  password = var.db_password
}
```

Conceptos:

- **Dependencia explícita**: `databases` y `users` referencian `google_sql_database_instance.postgres.name` → Terraform crea la instancia primero.
- **`authorized_networks 0.0.0.0/0`**: permite conexiones desde cualquier IP (Cloud Run accede por Internet). Es **solo para aprendizaje** — en producción se usaría el Cloud SQL Auth Proxy / connector.
- **`deletion_protection = false`**: permite destruir la instancia con `terraform destroy` (en producción siempre `true`).
- **Truco de la contraseña**: el `google_sql_user` recoge la contraseña del `tfvars`. Si aplicas una vez y luego cambias la contraseña, **Terraform la actualizará in-place** (el estado no conoce la contraseña real).

> **Lección operativa**: el primer `apply` de Cloud SQL quedó colgado esperando el polling (la operación estaba DONE en GCP). Se resolvió **importando** los recursos existentes: `terraform import google_sql_database_instance.postgres booksocial-infra:us-central1:booksocial-db` (y lo mismo para la BD y el usuario). Tras el import, `terraform plan` mostró `No changes`.

## 3.2 — Artifact Registry + imágenes Docker

### Repositorio de imágenes

```hcl
resource "google_artifact_registry_repository" "apps" {
  location      = var.region
  repository_id = "apps"
  description   = "Imágenes de los servicios de BookSocial"
  format        = "DOCKER"
}
```

### Build y push

Autenticación de Docker contra el registry y build de cada servicio **desde la raíz del monorepo** (mismo contexto que usa el Dockerfile):

```powershell
gcloud auth configure-docker us-central1-docker.pkg.dev

docker build -f identity-service/Dockerfile -t us-central1-docker.pkg.dev/booksocial-infra/apps/identity:latest .
docker build -f gateway/Dockerfile      -t us-central1-docker.pkg.dev/booksocial-infra/apps/gateway:latest .
docker push us-central1-docker.pkg.dev/booksocial-infra/apps/identity:latest
docker push us-central1-docker.pkg.dev/booksocial-infra/apps/gateway:latest
```

La convención de tags es `us-central1-docker.pkg.dev/<project>/apps/<servicio>:latest` y Cloud Run referencia exactamente esa imagen.

## 3.3 — Cloud Run: identity con Redis sidecar

Cloud Run con **múltiples contenedores** permite acompañar la app de un **sidecar** (aquí, Redis para el rate-limiting). Regla de oro: **exactamente un contenedor con puerto expuesto**.

```hcl
resource "google_cloud_run_v2_service" "identity" {
  name = "identity"
  location = var.region
  deletion_protection = false   # el default de Cloud Run v2 es true: sin esto, destroy falla

  template {
    scaling {
      min_instance_count = 0     # escala a cero (ahorro)
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/identity:latest"

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env {
        name  = "SPRING_DATASOURCE_URL"
        value = "jdbc:postgresql://${var.db_host}:5432/booksocial"
      }
      env {
        name  = "SPRING_REDIS_HOST"
        value = "localhost"
      }
      env {
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "SPRING_DATASOURCE_PASSWORD"
        value = var.db_password
      }
      env {
        name  = "FRONTEND_URL"
        value = var.frontend_url
      }
      env {
        name  = "SERVER_PORT"
        value = "8080"
      }
    }

    containers {
      image   = "redis:7-alpine"
      command = ["redis-server"]
    }
  }
}
```

Cuatro ajustes que son fruto de errores reales (ver Apéndice A):

1. **`SPRING_DATASOURCE_PASSWORD`**: la app trae por defecto `password: booksocial`, pero el usuario de Cloud SQL tiene la de `var.db_password` → sin esto, `FATAL: password authentication failed`.
2. **`SERVER_PORT=8080`**: `application.yml` fija `server.port: 8081`, pero Cloud Run sondea el puerto `$PORT` (8080). Si el contenedor escucha en otro puerto, el **startup probe falla** y Cloud Run mata la instancia.
3. **`command = ["redis-server"]`** en el sidecar: la imagen `redis:7-alpine` ejecuta un entrypoint script que usa `su-exec` para bajar a usuario `redis`; en el sandbox de Cloud Run eso falla con `redis-server: I/O error`. Lanzando el binario directamente, funciona.
4. **`deletion_protection = false`**: obligatorio si quieres poder hacer `terraform destroy`.

## 3.4 — Cloud Run: gateway + IAM público

```hcl
resource "google_cloud_run_v2_service" "gateway" {
  name = "gateway"
  location = var.region
  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0
    }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/gateway:latest"

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env {
        name  = "APP_JWT_SECRET"
        value = var.jwt_secret
      }
      env {
        name  = "IDENTITY_SERVICE_URI"
        value = google_cloud_run_v2_service.identity.uri
      }
      env {
        name  = "USER_SERVICE_URI"
        value = google_cloud_run_v2_service.user.uri   # tras desplegar user/book (sección 3.6)
      }
      env {
        name  = "BOOK_SERVICE_URI"
        value = google_cloud_run_v2_service.book.uri   # tras desplegar book
      }
    }
  }
}
```

- **Referencia entre recursos**: el gateway recibe la URL de identity como `google_cloud_run_v2_service.identity.uri` (atributo que Terraform rellena con la URL `xxx.a.run.app` generada al crear el servicio).
- **Sin login**: por defecto Cloud Run exige token IAM. Exponer `allUsers` con rol `roles/run.invoker` habilita el acceso HTTP público:

```hcl
resource "google_cloud_run_v2_service_iam_member" "public" {
  for_each = {
    identity     = google_cloud_run_v2_service.identity.name
    gateway      = google_cloud_run_v2_service.gateway.name
    user-service = google_cloud_run_v2_service.user.name
    book-service = google_cloud_run_v2_service.book.name
  }
  project  = var.project_id
  location = var.region
  name     = each.value
  role     = "roles/run.invoker"
  member   = "allUsers"
}
```

- **Salidas** en `outputs.tf`:

```hcl
output "gateway_url" {
  value = google_cloud_run_v2_service.gateway.uri
}

output "identity_url" {
  value = google_cloud_run_v2_service.identity.uri
}
```

### Quirk conocido: "Provider produced inconsistent final plan"

Al crear **gateway e identity en el mismo apply**, `IDENTITY_SERVICE_URI` se referencia a un atributo aún desconocido durante el plan → el provider lo planifica como `""` y al aplicar, con el valor real, el plan "no correlaciona". **Solución operativa**: reaplicar. Como identity ya queda creado en el primer apply, en el segundo su `uri` ya está en el estado y todo correlaciona.

## 3.5 — Despliegue y verificación end-to-end

```powershell
terraform init
terraform plan
terraform apply
terraform output gateway_url identity_url
```

Resultado esperado (Fase 13, alcances A + B):

| Servicio      | URL                                        |
| ------------- | ------------------------------------------ |
| gateway       | `https://gateway-h6b4lrpgmq-uc.a.run.app`  |
| identity      | `https://identity-h6b4lrpgmq-uc.a.run.app` |
| user-service  | `https://user-service-h6b4lrpgmq-uc.a.run.app` |
| book-service  | `https://book-service-h6b4lrpgmq-uc.a.run.app`  |

Estado de los servicios:

```powershell
gcloud run services list --region us-central1 --format="table(SERVICE,URL,STATUS)"
```

### Health y flujo de auth

```powershell
# Health de identity (raíz, público)
(Invoke-RestMethod -Uri "https://identity-h6b4lrpgmq-uc.a.run.app/actuator/health").Content
```

> El health raíz reporta `status: DOWN` porque el **indicador de SMTP falla** (no hay servidor de correo configurado). DB y Redis están UP; es inocuo para desarrollo. Se puede ocultar con `management.health.mail.enabled: false`.

Registro y login (¡importante el JSON desde `Invoke-RestMethod`, no `curl.exe`! — ver Apéndice A):

```powershell
$r = Invoke-RestMethod -Method Post -Uri "https://identity-h6b4lrpgmq-uc.a.run.app/auth/register" -ContentType "application/json" -Body '{"email":"test3@test.com","password":"Test1234!","firstName":"Ana","lastName":"Lopez","birthDate":"1992-05-14"}'
$r | ConvertTo-Json -Depth 5

$r = Invoke-RestMethod -Method Post -Uri "https://gateway-h6b4lrpgmq-uc.a.run.app/auth/login" -ContentType "application/json" -Body '{"email":"admin@booksocial.com","password":"admin12345"}'
$r | ConvertTo-Json -Depth 5
```

Respuesta esperada: `{ accessToken, refreshToken, expiresIn: 900, tokenType: "Bearer" }`.

Endpoint protegido a través del gateway (prueba el filtro JWT + la inyección de headers `X-User-*`):

```powershell
$token = "TU_ACCESS_TOKEN"
Invoke-WebRequest -Uri "https://gateway-h6b4lrpgmq-uc.a.run.app/users/me" -Headers @{Authorization="Bearer $token"} | Select-Object -ExpandProperty Content
```

## 3.6 — user-service + book-service en Cloud Run (Mongo Atlas + CloudAMQP)

user-service y book-service necesitan **MongoDB** (read models CQRS) y **RabbitMQ** (broker de eventos). Para el despliegue cloud se usan **cuentas gratuitas externas**: **MongoDB Atlas** (cluster **M0**) y **CloudAMQP** (plan **lemur**, RabbitMQ 4). Este es el **alcance B** de la Fase 13.

### 3.6.1 — Cuentas externas gratuitas

| Servicio     | Cómo obtenerlo | Qué ofrece                                        |
| ------------ | -------------- | ------------------------------------------------- |
| MongoDB Atlas | https://www.mongodb.com/atlas → free M0 | Sierra M0 free tier, URI `mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/booksocial?retryWrites=true&w=majority&authSource=admin` |
| CloudAMQP    | https://www.cloudamqp.com → plan lemur | Instancia RabbitMQ, URI `amqps://<user>:<pass>@<host>[:<port>]/[<vhost>]` |

> **Ojo con la URI de Mongo**: el **nombre de la base de datos debe estar en el path** (`/booksocial`). Sin él, Spring lanza `java.lang.IllegalArgumentException: Database name must not be empty` y el contenedor de Cloud Run muere (`Error code 9`). Con Atlas M0 se añade `?authSource=admin` (usuario admin) y `retryWrites=true`.

### 3.6.2 — Descomponer la URI de CloudAMQP en `main.tf` (locals)

El `spring.rabbitmq.*` necesita **6 envs separados** (`HOST`, `PORT`, `USERNAME`, `PASSWORD`, `VIRTUAL_HOST`, `SSL_ENABLED`), pero CloudAMQP da una sola URI. Se descompone con `regex` en un bloque `locals`:

```hcl
locals {
  rabbitmq        = regex("^amqps?://([^:]+):([^@]+)@([^/:]+)(?::([0-9]+))?(?:/([^/]*))?$", var.rabbitmq_uri)
  rabbitmq_tls    = startswith(var.rabbitmq_uri, "amqps://")
  rabbitmq_port   = coalesce(local.rabbitmq[3], local.rabbitmq_tls ? "5671" : "5672")
  rabbitmq_vhost  = coalesce(local.rabbitmq[4], "/")
}
```

- `rabbitmq[0]` = user, `[1]` = password, `[2]` = host, `[3]` = puerto (**opcional**), `[4]` = vhost (**opcional**).
- El regex usa `[^/:]+` para el host (no `[^/]+`): si se captura con `[^/]+`, la ruta `/vhost` contamina el host.
- `coalesce` resuelve los grupos opcionales: si no hay puerto, `amqps` → `5671`, `amqp` → `5672`; si no hay vhost → `/`.

### 3.6.3 — Nuevas variables en `variables.tf`

```hcl
variable "mongo_uri" {
  description = "URI de MongoDB Atlas (con el nombre de la BD en el path)"
  type        = string
  sensitive   = true
}

variable "rabbitmq_uri" {
  description = "URI de CloudAMQP (amqps://user:pass@host[:port]/[vhost])"
  type        = string
  sensitive   = true
}

variable "google_books_api_key" {
  description = "API key de Google Books (book-service)"
  type        = string
  sensitive   = true
  default     = ""
}
```

Y en el `terraform.tfvars` (no se commitea):

```hcl
mongo_uri           = "mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/booksocial?retryWrites=true&w=majority&authSource=admin"
rabbitmq_uri        = "amqps://<user>:<pass>@<host>"
google_books_api_key = "AIza..."
```

### 3.6.4 — Cloud Run v2 para user-service y book-service

Ambos recursos son gemelos (puerto `8080`, `SERVER_PORT=8080`, 512Mi, min instances 0). La diferencia son las envs: los dos usan Postgres (`SPRING_DATASOURCE_URL`/`SPRING_DATASOURCE_PASSWORD`), `SPRING_MONGODB_URI` y las 6 de RabbitMQ (descompuestas del `locals`); book-service además añade `GOOGLE_BOOKS_API_KEY`.

```hcl
resource "google_cloud_run_v2_service" "user" {
  name                = "user-service"
  location            = var.region
  deletion_protection = false

  template {
    scaling { min_instance_count = 0 }

    containers {
      image = "us-central1-docker.pkg.dev/${var.project_id}/apps/user:latest"

      ports { container_port = 8080 }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      env { name = "SPRING_DATASOURCE_URL";      value = "jdbc:postgresql://${var.db_host}:5432/booksocial" }
      env { name = "SPRING_DATASOURCE_PASSWORD"; value = var.db_password }
      env { name = "APP_JWT_SECRET";             value = var.jwt_secret }
      env { name = "SPRING_MONGODB_URI";         value = var.mongo_uri }
      env { name = "SPRING_RABBITMQ_HOST";       value = local.rabbitmq[2] }
      env { name = "SPRING_RABBITMQ_PORT";       value = local.rabbitmq_port }
      env { name = "SPRING_RABBITMQ_USERNAME";   value = local.rabbitmq[0] }
      env { name = "SPRING_RABBITMQ_PASSWORD";   value = local.rabbitmq[1] }
      env { name = "SPRING_RABBITMQ_VIRTUAL_HOST"; value = local.rabbitmq_vhost }
      env { name = "SPRING_RABBITMQ_SSL_ENABLED";  value = tostring(local.rabbitmq_tls) }
      env { name = "SERVER_PORT";                value = "8080" }
    }
  }
}
```

El `book` es idéntico (`name = "book-service"`, imagen `apps/book:latest`) más la env `GOOGLE_BOOKS_API_KEY`.

> **Envs de Spring Boot 4.1**: Mongo usa el prefijo `spring.mongodb.*` → env `SPRING_MONGODB_URI` (el V3 `spring.data.mongodb.*` ya no). RabbitMQ usa `spring.rabbitmq.*` → `SPRING_RABBITMQ_HOST` etc. El **relaxed binding** de Spring Boot hace que una env var sobreescriba el valor hardcodeado de `application.yml` (p.ej. el `password: booksocial` se ve sustituido por `SPRING_DATASOURCE_PASSWORD`).

### 3.6.5 — Apply y verificación

```powershell
terraform validate
terraform plan
terraform apply
```

El primer `apply` puede fallar con `Error code 9` (el contenedor murió) si la URI de Mongo **no tiene la BD en el path**: se ve en los logs `java.lang.IllegalArgumentException: Database name must not be empty`. Se corrige la URI en `tfvars` y se reaplica (Cloud Run recrea los servicios; el estado converge).

Verificación E2E (JSON **desde archivo**, ver Apéndice A — el `curl.exe` en PowerShell rompe las comillas):

```powershell
# POST /books/{isbn} auto-importa desde Google Books (público, sin token)
curl.exe -s -w "`nHTTP %{http_code}`n" "https://book-service-h6b4lrpgmq-uc.a.run.app/books/9780061120084"

# Register vía gateway → tokens
curl.exe -s -w "`nHTTP %{http_code}`n" -X POST "https://gateway-h6b4lrpgmq-uc.a.run.app/auth/register" -H "Content-Type: application/json" -d "@body.json"

# Con el access token: materializar perfil, seguir a otro usuario, listar following
curl.exe -s "https://gateway-h6b4lrpgmq-uc.a.run.app/profiles/me" -H "Authorization: Bearer $token"
curl.exe -s -X POST "https://gateway-h6b4lrpgmq-uc.a.run.app/follows/6" -H "Authorization: Bearer $token" -H "Content-Length: 0"
curl.exe -s "https://gateway-h6b4lrpgmq-uc.a.run.app/follows/3/following" -H "Authorization: Bearer $token"
```

> **Detalle curioso + lección**: los `GET /follows/following` y `/follows/followers` NO existen como rutas propias: el `FollowController` las expone con **path variable** (`/follows/{userId}/followers`, `/follows/{userId}/following`). Pedir `/follows/following` devuelve `401 Authentication required` (Spring lanza el entry point al no coincidir ninguna ruta autenticada). Usar siempre `/follows/{userId}/...`.

> **POST sin body en Cloud Run**: los POST sin cuerpo (`POST /follows/6`) fallan con `411 Length Required` del balanceador de Google. Solución: `-H "Content-Length: 0"` (o `-d ""`).

## 3.7 — Qué queda fuera del alcance B

- **OAuth2 Google**: el container de identity necesita `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` reales + `redirect_uri` HTTPS (`https://<identity>.a.run.app/login/oauth2/code/google`). Sin ellos, el botón "Google" genera una URL con el placeholder `${GOOGLE_CLIENT_ID}`.
- **Frontend**: Bloque 4 (Cloud Run para el SPA, o bien Vercel/Netlify como alternativa gratuita).
- **resto de servicios** (review-service, shelf-service, social-service, notification-service): necesitan también Mongo + RabbitMQ (ya disponibles) + sus URIs de interconexión (`REVIEW_SERVICE_URI`, `SHELF_SERVICE_URI`, `SOCIAL_SERVICE_URI`, `NOTIFICATION_SERVICE_URI`) — misma receta que 3.6.4.
- **Backend state cloud**: el `.tfstate` vive en **local** (carpeta `dev`), protegido del Git por el `.gitignore`, pero **solo lo ve tu máquina**. Para trabajo en equipo hay que mover el estado a un **backend remoto**. Aprende cómo de forma pedagógica en [3.7.1](#371--backend-remoto-gcs-mover-el-estado-local-a-la-nube) justo debajo.

### 3.7.1 — Backend remoto (GCS): mover el estado local a la nube

**El problema**: el estado `terraform.tfstate` es un fichero **local**. Si eres solo tú, funciona. Pero si hay más personas (o quieres un historial seguro y compartido), cada una tendría **su propio estado**, y aplicar cambios desde dos sitios produciría conflictos, duplicados o borrados accidentales. El estado también es **sensible** (para los resources de tu nube) y conviene tenerlo a salvo y con versionado.

**La solución**: un **backend remoto**. Terraform, en vez de guardar el estado en `./terraform.tfstate`, lo guarda en un **bucket de GCS** (Google Cloud Storage). De modo que:

- El estado es **único y compartido** (todos leen/escriben el mismo fichero).
- Terraform lo **bloquea** mientras aplica (evita dos `apply` simultáneos corruptos).
- Puedes activar **versionado** del bucket: ante un estado roto, se recupera una versión anterior.

**Cómo activarlo** (adaptado a este proyecto):

1. Crear un bucket de GCS (manualmente una vez, con la CLI o la consola). Al crearlo fuera de Terraform se evita el "problema del huevo y la gallina" (Terraform no puede guardar el estado de su propio bucket en ese mismo bucket).

```powershell
gsutil mb -p booksocial-infra -l us-central1 -b on gs://tf-state-booksocial
gsutil versioning set on gs://tf-state-booksocial   # opcional, recomendado
```

2. Configurar el backend en `provider.tf`:

```hcl
terraform {
  backend "gcs" {
    bucket = "tf-state-booksocial"
    prefix = "terraform/environments/dev"
  }
  # ... el resto (required_version, required_providers) igual
}
```

3. Migrar el estado local existente cambiando el backend y re-inicializando:

```powershell
terraform init -migrate-state   # detecta el estado local y lo sube al bucket preguntando confirmación
```

> `-migrate-state` copia el estado local actual al bucket **sin borrar nada** y deja desde entonces la nube como fuente de verdad. Sin él, `terraform init` simplemente usa el backend declarado (y si este `prefix` ya tiene estado, lo usa).

**Consideraciones**

- **No se mezcla backend local con remoto "a medias"**: al cambiar el `terraform {}` block de backend, siempre hay que re-ejecutar `terraform init` (o `init -migrate-state`); Terraform lo avisa si lo olvidas.
- Los **secretos** del estado ya estaban protegidos del Git; al pasar a GCS asegúrate de que el bucket tenga **IAM restringido** (solo las cuentas del equipo) y, de nuevo, activa **versionado**.
- El `.gitignore` sigue ignorando `*.tfstate`: no cambia nada ahí, simplemente el fichero local deja de generarse (o queda obsoleto).
- Para una sola persona en modo aprendizaje (caso de esta guía), el backend local es suficiente; este apartado queda como **siguiente paso al trabajar en equipo**.

> **Nota (revisión posterior a la Fase 13B)** — se intentó desplegar los 8 servicios (identity, gateway, user, book, review, shelf, social, notification) en Cloud Run. El `db-f1-micro` de Cloud SQL **no admite más de ~2 servicios Spring+JPA simultáneos**: al arrancar varios en paralelo se agotan los slots reservados y aparece `FATAL: remaining connection slots are reserved for roles with privileges of the "pg_use_reserved_connections" role` / SQLState `53300` (`too_many_connections`). El flag `max_connections` **no es editable** en tier `shared-core`. Configuración gratuita estable desplegada:
>
> - **Con Postgres (máx. ~2)**: `identity` + `book-service`.
> - **Solo Mongo + Rabbit (no gastan slots Postgres)**: `social-service` + `notification-service` desplegados adicionalmente.
> - **Fuera de Cloud Run** (chocan con el límite de Postgres o no priorizados): `user-service`, `review-service`, `shelf-service`.

---

# Apéndice A — Errores típicos del despliegue cloud

| Síntoma | Causa raíz | Solución |
| --- | --- | --- |
| `Error: Provider produced inconsistent final plan` al aplicar gateway | `IDENTITY_SERVICE_URI` referencia `identity.uri`, desconocido en el mismo apply | Reaplicar: identity ya está en el estado y el valor correlaciona |
| `terminated: Application failed to start: Waited too long for connection to be ready` | La app espera a una dependencia (Red/BD) y el startup probe expira | Revisar logs de la revisión: `gcloud logging read "resource.type=cloud_run_revision AND resource.labels.revision_name=<rev>"` |
| `FATAL: password authentication failed for user "booksocial"` | La app usa su password por defecto; el Cloud SQL user tiene `var.db_password` | Añadir `SPRING_DATASOURCE_PASSWORD` al contenedor |
| `Tomcat started on port 8081` pero el probe falla | `server.port` ≠ `$PORT` de Cloud Run | `SERVER_PORT=8080` (o `server.port=${PORT}`) |
| `/usr/local/bin/docker-entrypoint.sh: redis-server: I/O error` | El entrypoint de `redis:7-alpine` usa `su-exec` (setuid), no permitido en el sandbox | `command = ["redis-server"]` en el sidecar |
| `Illegal base64 character: ' '` en `JwtService` | El `APP_JWT_SECRET` del tfvars contenía un espacio (o la clave `APP_JWT_SECRET=` de más) | Valor base64url limpio, sin espacios |
| `443` + "service seems down": `Ready` vacío al describir | La condición de Cloud Run v2 se llama `Active`, no siempre `Ready` | Comprobar con `gcloud run services list` o la URL directamente |
| 401 `{"error":"unauthorized",...}` al probar cualquier endpoint con body JSON con `curl.exe` en PowerShell | **PowerShell roba las comillas dobles** del JSON (bug de quoting) → body malformado (p.ej. `{email:cloud...}`) → `HttpMessageNotReadableException` en los logs del servicio | Escribir el JSON a un fichero y usar `curl.exe -d "@archivo.json"` (o `Invoke-RestMethod` con body en comillas simples) |
| `Error code 9` / `Database name must not be empty` al desplegar user/book | URI de MongoDB Atlas sin el nombre de BD en el path (`mongodb+srv://...@cluster.mongodb.net` sin `/booksocial`) | Añadir `/booksocial?retryWrites=true&w=majority&authSource=admin` a la URI y reaplicar |
| `GET /follows/following` → `401 Authentication required` | El controller usa path variable: los endpoints reales son `/follows/{userId}/followers` y `/follows/{userId}/following` | Usar la ruta con `{userId}` |
| `411 Length Required` en POST sin body (p.ej. `POST /follows/6`) | El balanceador de Google exige `Content-Length` | Añadir `-H "Content-Length: 0"` (curl) |
| `/actuator/health` → `DOWN` con `MongoCommandException error 8000 (AtlasError): not authorized on local` | Atlas M0 no autoriza al usuario en la BD `local` (el `MongoHealthIndicator` de Spring Boot 4.1 ejecuta `hello` contra `local`) | Inocuo: los endpoints de negocio funcionan; los indicadores Mongo/DB son aparte. Opcional: `management.health.mongodb.enabled: false` |
| Apply se queda colgado creando Cloud SQL | El polling del provider no detecta operaciones DONE | `terraform import` de los recursos y continuar |
| `FATAL: remaining connection slots are reserved for roles with privileges of the "pg_use_reserved_connections" role` (SQLState `53300`) al arrancar cualquier servicio con datasource | El tier `db-f1-micro`/shared-core de Cloud SQL admite muy pocas conexiones (~25 máx.); varios servicios Spring+JPA arrancando en paralelo llenan los slots | Reducir el número de servicios con Postgres desplegados a los que caben (≈2), o subir el tier de la instancia. `max_connections` **no es editable** en tier `shared-core`. Los servicios que **solo usan Mongo + Rabbit** (social, notification) **no** chocan con este límite y pueden desplegarse en paralelo |

### A.1 — Procedimiento general de diagnóstico: un servicio no despega en Cloud Run

Cuando un Cloud Run "se queda en el intento", lo más útil es **seguir este orden** (en vez de tocar el `.tf` a ciegas). La mayoría de errores del Apéndice se resuelven con el paso 2.

1. **¿Cuál es el estado?** `gcloud run services list --region us-central1`. Si el servicio no aparece con su URL o la condición no está `Active`, está fallando el arranque.

2. **Lee los logs del arranque** (la fuente de verdad de la causa raíz). Sustituye `<rev>` por la revisión que toca:

```powershell
# últimos logs de la revisión (más directo que darle vueltas a la consola)
gcloud beta run services logs tail --service <servicio> --region us-central1
# o por nombre de revisión
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.revision_name=<rev>"
```

3. **Diagnóstico de los 4 tramos típicos** (todos salen en los logs si los miras):
   - **El contenedor no arranca** (`Application failed to start`, `Error code 9`) → depende de la app: URI mal, secreto mal, password por defecto. Ver filas del Apéndice.
   - **El startup probe falla pero la app arrancó** (`Tomcat started on port 8081` + no `Ready`) → el puerto NO coincide con `$PORT` (8080). Env `SERVER_PORT=8080`.
   - **El entorno no tiene lo que la app espera** (Redis sidecar `I/O error`, Mongo `Database name must not be empty`) → ajustar el contenedor/sidecar, ver filas.
   - **El servicio está sano pero Cloud Run lo ve lejos** (límite de slots, `53300`) → problema de capacidad, no de código. Ver última fila.

4. **Aplica el cambio y verifica** (no asumas): edita `.tf` → `terraform plan` (confirma la `~`-modificación) → `terraform apply`. Cloud Run **recrea la revisión** sola; vuelve a leer los logs con el paso 2 para confirmar que el error cambió o desapareció.

5. **Verifica por red/HTPP**: cuando la revisión levante (paso 1 muestra `Active`), prueba la URL con `curl -w "%{http_code}"` y, si devuelve un `4xx`/`5xx`, repite el paso 2 (el `HttpMessageNotReadableException` u otros salen ahí). El `401 {"error":"unauthorized"}` con body JSON suele ser el truco de comillas de PowerShell (ver tabla), no un problema del servicio.

> **Regla de oro**: ante un fallo de Cloud Run, **casi siempre la causa está en los logs del contenedor**, no en el `.tf`. El `.tf` solo define el "quién y cómo" (envs, puerto, sidecar); los logs te dicen el "por qué muere". Orden recomendado: logs → ajuste de `.tf` → reapply → logs.

---

*Actualizado al cierre de la Fase 13 — alcances A (identity + gateway + Redis sidecar + Cloud SQL) y B (user-service + book-service + MongoDB Atlas M0 + CloudAMQP) desplegados y verificados E2E en GCP. Revisión posterior: tras probar los 8 servicios, se revirtió a una configuración estable en capa gratuita (identity + gateway + book-service, **más social-service + notification-service por ser solo Mongo+Rabbit**) por el límite de conexiones del `db-f1-micro` (ver nota en 3.7 y Apéndice A). Contenido pedagógico añadido para principiantes: [0.0](#00--qué-es-docker-apuntes-para-principiantes) (qué es Docker: imágenes, contenedores, volúmenes, puertos, Dockerfile, compose, contexto), [1.1.1](#111--el-ciclo-de-vida-de-maven-y-qué-hace-mvnw--b--pl-identity-service--am-package--dskiptests) (ciclo de vida de Maven y el comando del Dockerfile), [3.0.1](#301--terraform-desde-cero-apuntes-para-principiantes) (conceptos de Terraform), [3.7.1](#371--backend-remoto-gcs-mover-el-estado-local-a-la-nube) (backend remoto GCS) y [A.1](#a1--procedimiento-general-de-diagnóstico-un-servicio-no-despega-en-cloud-run) (diagnóstico).*
# BookSocial — Guía de Desarrollo (Backend)

Guía completa para construir **BookSocial**, una red social de libros con arquitectura de microservicios. El proyecto usa Java 21, Spring Boot 4.1.0, Spring Cloud Gateway, PostgreSQL, MongoDB, RabbitMQ y Docker.

> Para la interfaz de usuario (Angular), ver [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md).

---

## Cómo usar esta guía

Esta guía cubre exclusivamente el **backend** (servicios Java/Spring Boot, Gateway, Docker, CI). Para el frontend Angular, ver [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md).

La guía está organizada en **bloques cronológicos**: cada bloque se construye sobre el anterior, como un curso progresivo. Si empiezas desde cero, sigue el orden recomendado.

**Para aprender haciendo**: cada bloque incluye código real del proyecto, explicaciones de _por qué_ se toma cada decisión, y pasos de verificación. Ejecuta el código mientras lees.

**Para consultar después**: la tabla de contenidos te permite saltar directamente al bloque que necesites. Los apéndices al final consolidan patrones repetidos (seguridad, Docker).

**Nivel de detalle**: se mantiene el código esencial (entidades, servicios, controladores, configuración) con explicaciones conceptuales. Los patrones de seguridad repetidos en varios servicios se consolidan en el Apéndice A para evitar redundancia.

---

## Tabla de contenidos

| Bloque                                                                                                          | Tema                                   | Fase       |
| --------------------------------------------------------------------------------------------------------------- | -------------------------------------- | ---------- |
| [0. Build de backend](#bloque-0--build-de-backend-jdk-maven-y-parent-pom)                                       | JDK, Maven, Wrapper, Parent POM        | —          |
| [1. Identity Service](#bloque-1--identity-service)                                                              | Auth, JWT, OAuth2, roles, reset, rate-limit | Fase 1 |
| [2. API Gateway](#bloque-2--api-gateway)                                                                        | Enrutamiento, JWT, headers             | Fase 1     |
| [5. Errores y decisiones](#bloque-5--cierre-errores-resueltos-de-la-fase-1)                                     | Retrospectiva Fase 1                   | Fase 1     |
| [6. user-service](#bloque-6--fase-2-user-service-perfil-con-cqrs-dual-write)                                    | CQRS, follows, RabbitMQ, People        | Fase 2     |
| [7. book-service](#bloque-7--book-service-catálogo-de-libros-con-cqrs)                                          | Catálogo, búsqueda, roles              | Fase 3     |
| [8. review-service](#bloque-8--review-service-reseñas--primer-evento-cruzado)                                   | Eventos cruzados, stats                | Fase 4     |
| [9. shelf-service](#bloque-9--shelf-service-estantería-personal-del-usuario)                                    | Estantería, dual-write, evento cruzado | Fase 5     |
| [11. social + notification](#bloque-11--fase-9-feed-social-social-service--notificaciones-notification-service) | Feed por eventos, notificaciones STOMP | Fase 9     |
| [A. Apéndice: Seguridad](#apéndice-a--plantilla-de-seguridad-reutilizable)                                      | JwtService, filtros, config            | Referencia |
| [B. Decisiones de diseño](#apéndice-b--decisiones-de-diseño)                                                    | Resumen arquitectónico                 | Referencia |
| [C. Operación (backend)](#apéndice-c--operación-rápida-backend)                                                 | Redespliegue, dev local, logs          | Referencia |
| [D. RabbitMQ](#apéndice-d--rabbitmq-del-publisher-al-consumer)                                                  | Broker, eventos, colas                 | Referencia |
| [E. WebSocket (STOMP)](#apéndice-e--websocket-stomp-del-servidor-al-navegador)                                  | Push en tiempo real, WS                | Referencia |

---

## Arquitectura del sistema

```
                    ┌──────────────┐
                    │   Angular    │
                    │    (SPA)     │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │   Gateway    │
                    │   :8080      │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
   │  identity   │  │    user     │  │    book     │
   │  :8081      │  │  :8082      │  │  :8083      │
   └─────────────┘  └─────────────┘  └──────┬──────┘
                                            │
                   ┌────────────────────────┼────────────────────────┐
                   │                        │                        │
            ┌──────▼──────┐          ┌──────▼──────┐          ┌──────▼──────┐
            │   review    │          │   shelf     │          │   social    │
            │   :8084     │          │   :8085     │          │   :8086     │
            └─────────────┘          └─────────────┘          └──────┬──────┘
                                                                     │
                                                              ┌──────▼──────┐
                                                              │notification │
                                                              │   :8087     │
                                                              └─────────────┘
```

**Infraestructura**: PostgreSQL (datos relacionales), MongoDB (lecturas CQRS), RabbitMQ (eventos asíncronos).

**Patrón CQRS**: escrituras en PostgreSQL (command side), lecturas en MongoDB (query side). Sincronización vía dual-write o eventos RabbitMQ.

**Autenticación**: JWT stateless. El gateway valida tokens y reenvía headers de confianza (`X-User-Id`, `X-User-Email`, `X-User-Roles`).

---

## Bloque 0 — Build de backend: JDK, Maven y Parent POM

Este bloque establece las bases del proyecto en lo relativo al **backend**: herramientas de build (JDK/Maven/Wrapper) y el Parent POM raíz. La estructura de carpetas, el `.gitignore`, Docker Compose y el CI son infraestructura y están detallados en [GUIDE-INFRA.md](./GUIDE-INFRA.md) (aquí solo se resume lo imprescindible). Es el punto de partida para cualquier desarrollador que se una al proyecto.

**Objetivo**: tener el entorno de build del backend listo y entendible para que cualquier cambio se verifique automáticamente.

### 0.1 — Instalación de JDK 21 + Maven + wrapper mvnw

Para este proyecto se ha optado por usar **_Java 21 (LTS)_** como _runtime_ y **_Maven_** como _build tool_. **Spring Boot 4.1.0** exige Java 21+, por eso se ha establecido que el equipo tenga la variable de entorno `JAVA_HOME` apuntando al JDK 21 (la terminal apunta a esa versión de java).

```bash
java -version           # openjdk version "21.0.12"
echo $env:JAVA_HOME     # C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot
mvn -v                  # Apache Maven 3.9.16, Java version: 21.0.12
```

El **Maven Wrapper** (mvnw, mvnw.cmd, .mvn/wrapper/) es un script que descarga y ejecuta una versión concreta de Maven automáticamente sin depender de que Maven esté instalado globalmente en la máquina. Su propósito es que nadie necesite Maven instalado para compilar el proyecto: ni tu máquina, ni GitHub Actions, ni el contenedor Docker. Se genera con `mvn -N wrapper:wrapper`.

Sin Maven Wrapper, cada desarrollador tendría que instalar Maven en su entorno, dando lugar a posibles diferencias entre versiones y distintos comportamientos entre entornos. Con el Wrapper, el proyecto especifica qué versión se debe utilizar.

Cuando se usa `./mvnw` en vez de `mvn`, no se está ejecutando directamente un Maven instalado en el sistema.

```bash
./mvnw -B clean package
```

En el ejemplo anterior, el Wrapper:

1. Comprueba qué versión de Maven necesita el proyecto.
2. Si esa versión no está descargada, la descarga.
3. La guarda normalmente en el directorio de Maven Wrapper/cache del usuario.
4. Ejecuta esa versión.
5. Pasa los argumentos: `-B clean package`

La versión de maven del proyecto se especifica en `.mvn/wrapper/maven-wrapper.properties`

### 0.2 — Parent POM raíz (pom.xml)

El `pom.xml` de la raíz es el corazón del monorepo. Su función es centralizar la configuración común para `gateway`, `identity-service`, etc.

1. **`modelVersion`** es la versión del modelo de POM que entiende Maven. No significa que la versión de Maven ni con la versión de Spring Boot sean la 4.0.0.

```xml
<modelVersion>4.0.0</modelVersion>
```

2. **Parent de Spring Boot:** El proyecto hereda configuración de Spring Boot.

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.1.0</version>
  <relativePath/>
</parent>
```

El `<spring-boot-starter-parent>` proporciona, entre otras cosas:

- Versiones compatibles de muchas dependencias (_logback_, _Jackson_, _JPA_...);
- Configuración del maven-compiler-plugin;
- Configuración del spring-boot-maven-plugin;
- Valores por defecto de Maven relacionados con Spring Boot.

`<relativePath/>` indica que el pom.xml padre no se encuentra en un directorio local, si no en los repositorios de Maven.

Si no se indica, Maven intenta buscar el parent en `../pom.xml`. Por ejemplo, en el caso del pom de los microservicios, no hace falta indicarlo porque apunta por defecto al pom en la raíz del monorepo.

3. **Identidad del proyecto**

```xml
  <groupId>com.booksocial</groupId>
  <artifactId>booksocial-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>BookSocial</name>
  <description>Parent pom del monorepo BookSocial</description>
```

La parte más importante es `<packaging>pom</packaging>`. Esto indica que este proyecto **no genera un JAR**, sino que su función es coordinar otros módulos (gateway, identity-service...). Actúa como una "carpeta contenedora" con configuración compartida.

4. **Propiedades**: Aquí se centralizan los valores que se quieran reutilizar.

```xml
  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <spring-cloud.version>2025.1.2</spring-cloud.version>
  </properties>
```

En un proyecto Spring Boot moderno, normalmente `java.version` es la propiedad principal a mantener, y puede no ser necesario declarar `maven.compiler.source` y `maven.compiler.target` explícitamente si el parent/plugin ya los configura.

`<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` indica que los archivos fuente del proyecto utilizan **UTF-8**, evitando problemas con caracteres como `á é í ó ú ñ`.

`<spring-cloud.version>2025.1.2</spring-cloud.version>` nos permite establecer la versión en un único sitio, utilizándola después como `<version>${spring-cloud.version}</version>` en los diferentes microservicios.

5. **`dependencyManagement`** centraliza versiones de Spring Cloud para todos los módulos

```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
```

Esto no añade Spring Cloud como dependencia de tu aplicación, sino que importa un **BOM** (_Bill of Materials_).

El BOM indica que versiones utilizar para determinadas librerías de Spring Cloud. Por ejemplo, en un módulo podrías tener:

```xml
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId>
  </dependency>
```

En este caso, no hace falta especificar la versión, ya que está determinada por el BOM importado en el padre.

6. **`modules`** convierte tu proyecto en un **_Maven multi-module project_**.

```xml
  <modules>
    <module>gateway</module>
    <module>identity-service</module>
  </modules>
```

Aquí se declara qué carpetas son módulos Maven. Al ejecutar `mvnw clean verify` en la raíz, se compilan todos. Cada vez que se crea un servicio nuevo (user-service, book-service...), se añade aquí.

Los módulos deben declarar este POM como parent. Por ejemplo, `gateway/pom.xml`:

```xml
  <parent>
    <groupId>com.booksocial</groupId>
    <artifactId>booksocial-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </parent>

  <artifactId>gateway</artifactId>
```

### 0.3 — Estructura de carpetas y .gitignore

La estructura del monorepo, el `.gitignore` (incluida la política de **secretos**: `.env` por módulo que nunca entran en Git/imágenes) y el detalle de `infrastructure/` están documentados en [GUIDE-INFRA.md](./GUIDE-INFRA.md). Resumen aplicable al backend:

- Cada microservicio tiene **aislamiento total**: su propio código, `pom.xml`, `.env` y `Dockerfile`.
- La política de proyecto es que **ningún dato sensible viva en Git**: los `.env` de cada módulo se cargan en runtime y nunca entran en las imágenes Docker.

### 0.4 — infrastructure/docker-compose.yml

El archivo `infrastructure/docker-compose.yml` inicial define únicamente la infraestructura necesaria para ejecutar el proyecto en local:

- **PostgreSQL 16** — base de datos relacional.
- **MongoDB 8** — base de datos NoSQL.
- **RabbitMQ 4** — broker de mensajería.

Los microservicios **no se ejecutan todavía mediante Docker Compose**. En esta fase se ejecutan directamente desde el host. Los contenedores de las aplicaciones se incorporarán posteriormente, en la _Fase 1.7_.

Configuración inicial:

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

volumes:
  postgres-data:
  mongodb-data:
  rabbitmq-data:
```

### 0.4 — infrastructure/docker-compose.yml

El `infrastructure/docker-compose.yml` define la infraestructura local (PostgreSQL 16, MongoDB 8, RabbitMQ 4, y posteriormente Redis 7). Su contenido completo y los conceptos de **volúmenes**, **healthchecks** y **puertos publicados** están detallados en [GUIDE-INFRA.md](./GUIDE-INFRA.md).

Resumen para el backend:

- **Volúmenes**: los datos sobreviven al ciclo de vida del contenedor (p.ej. PostgreSQL se reconstruye pero `postgres-data` conserva los datos).
- **Healthchecks** (`pg_isready`, `mongosh ping`, `rabbitmq-diagnostics ping`): distinguen "contenedor arrancado" de "servicio listo"; base del `depends_on: condition: service_healthy`.
- **Puertos publicados**: mientras los servicios corren en el host se conectan a `localhost:5432`, `localhost:27017`, `localhost:5672` (y `15672` para la consola web de RabbitMQ).

### 0.5 — Workflow base de CI (ci.yml)

El proyecto tiene un workflow de Integración Continua en `.github/workflows/ci.yml`, con un job `build` (backend) y un job `frontend`. La **configuración completa** (secrets, servicio PostgreSQL de apoyo, job frontend, detalles del `-B clean verify` y el problema del `mvnw`/`exit code 126`) está documentada en [GUIDE-INFRA.md](./GUIDE-INFRA.md). Resumen del workflow base de backend:

```yml
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
      - uses: actions/setup-java@v5
        with:
          distribution: "temurin"
          java-version: "21"
          cache: maven
      - name: Build backend with Maven wrapper
        run: |
          chmod +x mvnw
          ./mvnw -B clean verify
```

Puntos clave:

- **Cuándo se ejecuta**: push a `main` y Pull Requests (evita que código que no compila o con tests fallidos llegue a `main`).
- **`./mvnw -B clean verify`**: usa el Maven Wrapper y `verify` corre el ciclo completo de Maven (incluidos los tests). El `-B` (batch mode) es apropiado para CI y el `chmod +x mvnw` evita el `exit code 126` al commitear desde Windows. Detalles del ciclo de vida en GUIDE-INFRA 1.1.1.

## Bloque 1 — Identity Service

El **Identity Service** es el microservicio responsable de todo lo relacionado con la identidad y autenticación de los usuarios de la aplicación. Es el bloque más extenso porque establece los patrones que los demás servicios reutilizarán.

**Por qué un servicio dedicado**: separar la autenticación del resto de la lógica de negocio permite escalar independientemente, aplicar políticas de seguridad específicas, y que otros servicios confíen en él sin reimplementar JWT.

**Lo que construiremos**: registro y login (email + contraseña), autenticación con Google OAuth2, sistema de roles (ADMIN, USER, MINOR_USER), tokens JWT con refresh rotation, y endpoints para consultar datos del usuario.

**Puertos**: Identity Service `8081`, Gateway `8080`. Flujo típico: `Angular → Gateway :8080 → Identity Service :8081`.

**Ficha del servicio**

|                 |                                                                                                                                                                  |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8081`                                                                                                                                                           |
| Persistencia    | PostgreSQL (`users`, `refresh_tokens`, roles)                                                                                                                    |
| Responsabilidad | Identidad y autenticación: registro, login email+password, login Google OAuth2, emisión/rotación de JWT, gestión de roles                                        |
| Endpoints clave | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/forgot-password`, `POST /auth/reset-password`, callback OAuth2 |
| Consumidores    | Todos los servicios (confían en los headers `X-User-*` que valida el gateway)                                                                                    |

### 1.0 — Creación del servicio y dependencias

El servicio se generó con **Spring Initializr** (start.spring.io) con Java 21 y Spring Boot 4.1.0, y su POM hereda del parent `booksocial-parent`. Por eso **no repite versiones**: las hereda del parent POM y del BOM de Spring Cloud (sección 0.2). Dependencias del `identity-service/pom.xml` y para qué sirve cada una:

| Dependencia                                | Para qué sirve                                                  |
| ------------------------------------------ | --------------------------------------------------------------- |
| `spring-boot-starter-webmvc`               | Controllers REST (`@RestController`) y servidor web embebido    |
| `spring-boot-starter-data-jpa`             | Hibernate (ORM) y repositorios Spring Data                      |
| `spring-boot-starter-validation`           | Bean Validation en los DTOs (`@NotBlank`, `@Email`, `@Size`...) |
| `spring-boot-starter-security`             | Spring Security: `SecurityFilterChain`, filtros, BCrypt         |
| `spring-boot-starter-oauth2-client`        | Login con Google (cliente OAuth2)                               |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson`  | Crear y validar JWT (JJWT 0.12.6)                               |
| `postgresql`                               | Driver JDBC de PostgreSQL                                       |
| `spring-boot-starter-actuator`             | `/actuator/health` para los healthchecks                        |
| `spring-boot-devtools` (runtime, opcional) | Recarga automática en desarrollo                                |

> Nota: los starters de test llevan sufijo `-test` (p.ej. `spring-boot-starter-security-test`, `spring-boot-starter-data-jpa-test`). Además, `jjwt-impl` y `jjwt-jackson` están en scope `runtime` porque solo se necesitan en ejecución, no al compilar el código.

**Clase principal**:

```java
@SpringBootApplication
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
```

`@SpringBootApplication` combina tres anotaciones:

- `@Configuration`: la clase puede declarar beans.
- `@EnableAutoConfiguration`: Spring Boot configura automáticamente lo que encuentra en el classpath (el DataSource si hay driver de BD, Spring Security, etc.).
- `@ComponentScan`: busca componentes (entidades, repositorios, servicios, controllers, configuraciones) en el paquete base `com.booksocial.identity` y sus subpaquetes.

El resto de clases del servicio "aparece" en el contexto de Spring porque vive bajo ese paquete base.

### 1.1 — `application.yml`

El `application.yml` contiene la **configuración externa** del Identity Service.

Una idea importante de Spring Boot es separar: **código de la aplicación → configuración → secretos**. El código Java no debería tener valores como contraseñas, claves JWT o secretos de Google escritos directamente.

#### Identidad del servicio

`spring.application.name` establece el nombre de la aplicación: _identity-service_.

```yml
spring:
  application:
    name: identity-service
```

Este nombre puede utilizarse posteriormente por herramientas del ecosistema Spring Cloud, especialmente cuando se utiliza descubrimiento de servicios, configuración distribuida, trazas, logs, etc.

#### Importación del `.env`

`spring.config.import` hace que Spring pueda cargar variables desde un archivo `.env`.

```yml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

- `optional`: indica que la existencia de este archivo no es obligatoria. Si el `.env` no se encuentra, la aplicación arrancará con normalidad sin lanzar un error de que falta el archivo.

- `file`: le dice al framework dónde buscar el archivo. En este caso, indica que debe buscarlo en el sistema de archivos.

- `.env`: nombre exacto del archivo que la aplicación va a intentar localizar y leer.

- `[.properties]`: Spring Boot sabe cómo leer archivos con extensiones `.properties` o `.yml`/`.yaml` por defecto. Como `.env` no es una extensión que reconozca nativamente, esta sintaxis entre corchetes le dice al sistema que lea este archivo y lo procese igual que los archivos `.properties` (formato `CLAVE=VALOR`).

El `.env` contiene información sensible y debe estar incluido en `.gitignore`.

#### Configuración de PostgreSQL

```yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/booksocial}
    username: booksocial
    password: booksocial
```

Spring Boot utiliza esta información para crear el **DataSource** (componente encargado de proporcionar conexiones con _PostgreSQL_).

- La URL [jdbc:postgresql://localhost:5432/booksocial](jdbc:postgresql://localhost:5432/booksocial) se interpreta como:

  ```
    jdbc       → Java Database Connectivity
    postgresql → driver/base de datos utilizada
    localhost  → servidor
    5432       → puerto de PostgreSQL
    booksocial → nombre de la base de datos
  ```

- `${VARIABLE:default}` significa: "utiliza `SPRING_DATASOURCE_URL` si existe; de lo contrario, utiliza `jdbc:postgresql://localhost:5432/booksocial`".
  - Por ejemplo, en local `SPRING_DATASOURCE_URL` puede no existir; entonces Spring utiliza `jdbc:postgresql://localhost:5432/booksocial`.
  - Pero en Docker Compose puede existir: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/booksocial` y entonces utiliza esa URL.

  Esto permite utilizar el mismo código y el mismo **JAR** tanto localmente como dentro de Docker.

#### Hibernate

**Hibernate** es la implementación **JPA** utilizada para mapear objetos Java a tablas de la base de datos.

```yml
spring:
  jpa:
    hibernate:
      ddl-auto: update

    open-in-view: false
```

Con `ddl-auto: update`, Hibernate analiza las entidades (clases de Java anotadas con _@Entity_) y puede crear o modificar las tablas necesarias. Por ejemplo:

```java
@Entity
@Table(name = "users")
public class User {}    // → CREATE TABLE users (...);
```

`update` es cómodo durante **desarrollo** porque permite modificar entidades y dejar que Hibernate actualice el esquema. Pero para **producción** es preferible utilizar herramientas de migración como _Flyway_ o _Liquibase_ porque permiten controlar las versiones explícitamente y saber exactamente qué cambios se han aplicado a la base de datos.

Hibernate compara el estado actual de tus entidades Java con el esquema de la base de datos y genera el código SQL (DDL) necesario para "actualizarla". Sin embargo, tiene limitaciones graves:

- No elimina ni renombra columnas: Por seguridad, Hibernate nunca hace un `DROP COLUMN`. Si renombras un campo en Java de `nombre` a `primerNombre`, Hibernate creará una nueva columna _primerNombre_ vacía y dejará la antigua _nombre_ intacta, ensuciando la base de datos.

- Sin control de versiones: No hay un registro histórico de cómo evolucionó la base de datos a lo largo del tiempo. Si un cambio rompe algo, no hay forma fácil de saber qué script se ejecutó.

- En producción, añadir una columna a una tabla con millones de registros puede **bloquear la tabla** durante minutos u horas. Hibernate lo ejecutará sin avisar, provocando caídas del sistema.

- El SQL generado depende del dialecto y de la versión exacta de Hibernate. Lo que funciona en el entorno de pruebas podría generar un script ligeramente distinto en producción.

Tanto **Flyway** como **Liquibase** introducen el concepto de Control de Versiones para Base de Datos. En lugar de que el ORM (Hibernate) decida qué hacer, tú escribes scripts exactos (migraciones) que representan cada cambio. La herramienta lee estos scripts en un orden estricto, los aplica una sola vez y guarda un registro en la base de datos para saber exactamente en qué versión se encuentra.

`open-in-view`
Spring Boot puede mantener abierto el contexto de persistencia durante prácticamente todo el procesamiento de una petición HTTP. Al establecer `open-in-view: false` se evita ese comportamiento.

La consecuencia importante es que las operaciones con la base de datos deben realizarse dentro de las capas apropiadas, normalmente dentro del servicio/transacción. Esto hace más evidente cuándo una operación está accediendo a la base de datos y evita depender accidentalmente de una sesión JPA abierta durante la generación de la respuesta.

#### OAuth2 con Google

```yml
security:
  oauth2:
    client:
      registration:
        google:
          client-id: ${GOOGLE_CLIENT_ID}
          client-secret: ${GOOGLE_CLIENT_SECRET}
```

Aquí se configura _Google_ como **proveedor OAuth2**.

- `client-id`: Identifica la aplicación frente a Google. No es considerado un secreto.

- `client-secret`: Credencial privada de la aplicación; se obtiene de una variable de entorno. No debe publicarse.

#### Puerto

```yml
server:
  port: 8081
```

El Identity Service escucha en [http://localhost:8081](http://localhost:8081).

La arquitectura del proyecto reserva:

```
8080 → Gateway
8081 → Identity Service
```

Por tanto, una petición normal de la aplicación puede seguir este camino:

```
Angular → Gateway :8080 → Identity Service :8081
```

#### Configuración propia `app.*`

```yml
app:
  admin:
    email: admin@booksocial.com
    password: admin12345

  jwt:
    secret: ${APP_JWT_SECRET}
    access-token-ttl: 15m
    refresh-token-ttl: 7d
    issuer: booksocial-identity

  oauth2:
    frontend-redirect-uri: http://localhost:4200/oauth2/callback
```

`app` no es equivalente a `spring:`. Son simplemente dos namespaces de configuración diferentes.

Spring utiliza `spring.*` para su propia configuración, mientras que `app.*` es una configuración definida por nuestra propia aplicación. Podemos acceder a ella desde Java mediante mecanismos como `@Value` o, preferiblemente para configuraciones agrupadas, `@ConfigurationProperties`.

#### JWT

Esta es una de las partes fundamentales del sistema.

```yml
app:
  jwt:
    secret: ${APP_JWT_SECRET}
    access-token-ttl: 15m
    refresh-token-ttl: 7d
    issuer: booksocial-identity
```

##### Tokens:

Hay dos tokens:

- `Access Token (15 minutos)`: se utiliza para acceder a endpoints protegidos.
- `Refresh Token (7 días)`: se utiliza para obtener un nuevo access token cuando el anterior expira.

El **refresh token** sirve para renovar la sesión sin obligar al usuario a introducir nuevamente su contraseña. Si solo tuviesemos un JWT válido durante 7 días y alguien lo roba, podría usarlo durante 7 días. Con un _access token_ con un tiempo de vida más corto y un _refresh token_ largo y rotativo, evitamos este problema.

##### Issuer:

Identifica quién ha emitido el JWT. El **Identity Service** genera `iss = booksocial-identity` y el **Gateway** puede exigir que el token tenga ese issuer. Esto evita aceptar simplemente cualquier JWT firmado con la misma clave si no procede del emisor esperado.

##### OAuth2 redirect:

Después de autenticarse correctamente mediante _Google_, el **Identity Service** necesita devolver al usuario al **frontend** _Angular_.

```yml
app:
  oauth2:
    frontend-redirect-uri: http://localhost:4200/oauth2/callback
```

#### Actuator

```yml
management:
  endpoints:
    web:
      exposure:
        include: health
```

**Spring Boot Actuator** proporciona endpoints de monitorización. Aquí únicamente se expone: `/actuator/health`.

- `exposure`: sirve como un cortafuegos interno. Actuator tiene docenas de endpoints útiles (info, beans, env, metrics), pero por motivos de seguridad, `exposure` limita cuáles están expuestos a internet.
  - `include: health`: Le indica a Spring que solo habilite el endpoint `/actuator/health` a través de la web, bloqueando el acceso al resto. (en las versiones recientes de Spring Boot, este es el comportamiento por defecto).
    Su trabajo no es solo decir "la aplicación está encendida", sino verificar que todas sus dependencias vitales funcionan.

    Si se levanta el Identity Service y se accede a [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health), devuelve un JSON muy simple:

    ```JSON
    { "status": "UP" }
    ```

    Con una base de datos configurada (en este proyecto **PostgreSQL**), el endpoint `health` hará automáticamente un "ping" a la base de datos. Si la base de datos se cae, el estado cambiará automáticamente a:

    ```JSON
    { "status": "DOWN" }
    ```

### 1.2 — Dominio: `User`, `Role` y `RefreshToken`

#### `Role`

Un **enum** permite definir un conjunto cerrado de valores. En este caso, un usuario únicamente puede tener uno de los roles definidos.

```java
public enum Role {
    ADMIN,
    MODERATOR,
    USER,
    MINOR_USER
}
```

La ventaja de usar _enum_ es que Java puede controlar mejor los valores permitidos. Por ejemplo, `Role.ADMIN` es **válido**, pero `Role.SUPER_ADMIN` **no compilaría** si no existe ese valor.

#### Entidad `User`

```java
@Entity
@Table(name = "users")
public class User { ... }
```

- `@Entity` indica a _JPA/Hibernate_ que esta clase representa una **entidad persistente**, es decir, sus objetos pueden almacenarse en la base de datos.

- `@Table(name = "users")` indica que la tabla correspondiente se llama `users`.

La entidad contiene identificador, email, contraseña, datos personales, roles y datos relacionados con OAuth2:

---

##### Identificador

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

- `@Id` identifica el atributo `id` como la clave primaria.

- `GenerationType.IDENTITY` delega la generación del identificador a la base de datos. En **PostgreSQL**, esto puede corresponder a una **columna autogenerada**.

---

##### Email

```java
@Email
@Column(name = "email", unique = true, nullable = false)
private String email;
```

- `@Email` es una validación que comprueba que el valor tiene formato de email.

- `@Column` identifica propiedades de la columna en la base de datos.
  - `name = "email"`: nombre de la columna en la base de datos.
  - `unique = true`: La base de datos no permitirá dos usuarios con el mismo email.
  - `nullable = false`: El email es obligatorio. El email funciona como identificador natural del usuario dentro del sistema.

---

##### Contraseña

```java
@Column(name = "password_hash")
private String passwordHash;
```

Aquí no se guarda la contraseña. Si el usuario registra `MiPassword123`, la aplicación genera un **hash BCrypt** de la contraseña y almacena el resultado en base de datos.

**BCrypt** incorpora un **_salt_** y está diseñado para ser un hash lento, dificultando ataques de fuerza bruta. El campo puede quedar **sin contraseña** para usuarios creados mediante Google.

---

##### Datos personales

Estos datos permiten representar la información básica del usuario.

```java
private String firstName;
private String lastName;
private LocalDate birthDate;
```

`birthDate` tiene una función de negocio importante. La edad no se almacena directamente, es mejor almacenar la fecha de nacimiento, que permite calcular la edad y puede utilizarse para determinar el rol:

```java
/**
 * Calcula la edad actual de la persona en años basándose en su fecha de nacimiento y en la fecha actual del sistema.
 * Tiene en cuenta los años bisiestos y la duración exacta de los meses.
 *
 * @return La edad exacta en años. Devuelve {@code -1} si la fecha de nacimiento
 *         no está definida (OAuth2).
 */
public int getAge() {
  return birthDate == null ? -1 : Period.between(this.birthDate, LocalDate.now()).getYears();
}
```

---

##### Roles con `@ElementCollection`

```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "user_roles")
@Enumerated(EnumType.STRING)
private Set<Role> roles;
```

Un usuario puede tener varios roles, por eso se utiliza `Set<Role>` en lugar de `Role`.

No se guardan como una columna `users.roles`, sino en una tabla independiente:

<table>
    <tr><th colspan="2">users</th></tr>
    <tr><td>id</td><td>email</td></tr>
    <tr><td>1</td><td>javier@...</td></tr>
</table>

<table>
    <tr><th colspan="2">user_roles</th></tr>
    <tr><td>user_id</td><td>role</td></tr>
    <tr><td>1</td><td>USER</td></tr>
    <tr><td>2</td><td>ADMIN</td></tr>
</table>

`@ElementCollection` indica que es una colección de valores pertenecientes a la entidad `User`, no una entidad independiente.

`@Enumerated(EnumType.STRING)` hace que el rol se almacene como _ADMIN_, _USER_, _MODERATOR_... y no como _0_, _1_, _2_... Esto es importante porque los números dependen del orden del enum.

`fetch = FetchType.EAGER` indica que los roles se cargan junto con el usuario.

---

##### `enabled`

Permite indicar si la cuenta está habilitada. Si `enabled = true`, puede autenticarse; `enabled = false`, cuenta deshabilitada.

---

##### `googleId`

Guarda el **identificador que Google** proporciona al usuario. Esto permite reconocer que `Google account X` y `BookSocial User Y` pertenecen a la misma identidad. El claim persistente utilizado es `sub` (_Subject_).

---

##### Fechas

_Hibernate_ puede gestionar automáticamente los **_Timestamps_**, lo que permite saber cuándo se creó y cuándo se modificó una cuenta.

```java
@CreationTimestamp
private Instant createdAt;

@UpdateTimestamp
private Instant updatedAt;
```

---

#### `RefreshToken`

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {}
```

Los _refresh tokens_ tienen su propia tabla:

| refresh_tokens |
| -------------- |
| id             |
| token_hash     |
| user_id        |
| expires_at     |
| revoked        |
| created_at     |

Nunca se debe guardar el **refresh token original** :

```java
@Column(name = "token_hash", unique = true, nullable = false)
private String tokenHash;
```

La aplicación genera un _refresh token original_, pero almacena _SHA-256(refresh token)_. Una filtración de la base de datos no revela directamente los refresh tokens válidos.

```
TOKEN ORIGINAL → SHA-256 → HASH → Base de datos
```

##### Relación `User` → `RefreshToken`

Muchos refresh tokens pueden pertenecer a un usuario. Esto puede ocurrir, por ejemplo, si el usuario tiene varias sesiones/dispositivos.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

##### `expiresAt`

Indica cuándo deja de ser válido el refresh token.

```java
private Instant expiresAt;
```

##### `revoked`

Permite invalidar un refresh token antes de su fecha de expiración.

```java
private boolean revoked;
```

- `revoked = false` → todavía puede utilizarse.

- `revoked = true` → ya no puede utilizarse.

Esto es fundamental para logout y rotación.

### 1.3 - DTOs (_Data Transfer Objects_)

Los DTO sirven para definir qué información entra y sale de la API. Esto **evita exponer directamente las entidades JPA**.

```
HTTP Request
    ↓
RegisterRequest
    ↓
Service
    ↓
User
    ↓
Service
    ↓
UserResponse
    ↓
HTTP Response
```

- `RegisterRequest`: Contiene los datos necesarios para registrar: `email`, `password`, `firstName`, `lastName` y `birthDate`. Además incorpora **_Bean Validation_**:

  ```java
  @NotBlank
  @Email
  @Size
  @NotNull
  ```

  Esto significa que una petición inválida puede rechazarse antes de ejecutar la lógica de negocio.

- `LoginRequest`: Representa las credenciales proporcionadas por el usuario (`email`, `password`).
- `RefreshRequest`: Contiene `refreshToken`. Aunque el diseño principal utiliza una **cookie**, se mantiene la posibilidad de recibir el refresh token en el body por compatibilidad.

---

- `TokenResponse`: Devuelve `accessToken`, `refreshToken`, `expiresIn` y `tokenType`. `expiresIn` está expresado en segundos.

  ```json
  {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 900,       # 15 minutos → 15 × 60 = 900 segundos
    "tokenType": "Bearer"
  }
  ```

- `UserResponse`: Devuelve información pública del usuario: `id`, `email`, `firstName`, `lastName`, `age` y `roles`.
  **No devuelve el `passwordHash`**. Si un _controller_ devuelve accidentalmente una entidad `User`, podría terminar exponiendo información que no debería salir de la aplicación.

### 1.4 - Servicios: la lógica de negocio

#### Repositorios (Spring Data JPA)

Antes de los servicios conviene ver la capa de datos. `JpaRepository<User, Long>` proporciona por herencia el CRUD básico (`save`, `findById`, `findAll`, `delete`...). Los métodos de búsqueda personalizados solo hay que **declararlos**; Spring Data genera la implementación a partir del **nombre del método**:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);       // SELECT ... WHERE email = ?
    Optional<User> findByGoogleId(String googleId); // SELECT ... WHERE google_id = ?
    boolean existsByEmail(String email);            // SELECT EXISTS(SELECT 1 WHERE email = ?)
}

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    boolean existsByTokenHash(String tokenHash);
}
```

- `Optional` en la firma obliga a quien llama a decidir qué hacer si el resultado no existe (devolver `401`, lanzar excepción...), evitando el clásico `null`.
- `existsByEmail` se usa en el registro para comprobar duplicados **sin traer el objeto completo** a memoria.
- `findByTokenHash` se usa para validar/revocar refresh tokens buscando por el hash SHA-256 (sección `RefreshTokenService`).

#### `UserService`

##### `register()`

```
RegisterRequest
↓
¿Email existe?
↓
NO
↓
BCrypt(password)
↓
calcular edad
↓
asignar rol
↓
guardar User
```

1. **Comprobar email:** `existsByEmail(...)`.
   Si ya existe, devuelve `EmailAlreadyExistsException`. El _handler_ transforma posteriormente esa excepción en `HTTP 409 Conflict`.

   ```java
   @ExceptionHandler(EmailAlreadyExistsException.class)
       public ResponseEntity<Map<String, String>> emailAlreadyExists() {
           return ResponseEntity.status(HttpStatus.CONFLICT)
                   .body(Map.of("error", "email_already_exists"));
       }
   ```

2. **BCrypt:** `passwordEncoder.encode(password)`.

   El _password_ original nunca se guarda. La aplicación almacena únicamente el _hash_. En el login ocurre el proceso inverso:

   ```
   password enviada
   ↓
   BCrypt compara
   ↓
   passwordHash almacenado
   ```

   No se desencripta el hash: _BCrypt_ no cifra la contraseña para poder recuperarla; la hashea para poder comprobarla.

3. **Rol según edad**: `edad < 18 → MINOR_USER` / `edad >= 18 → USER`.
   Esto permite que posteriormente el sistema pueda aplicar restricciones diferentes a menores.

##### OAuth2: `linkOrCreateOAuthUser()`

Cuando Google autentica al usuario, el `Identity Service` recibe sus atributos. Primero obtiene el `sub`, el identificador estable del usuario dentro del proveedor.

```
¿Existe googleId/sub?
       │
   ┌───┴───┐
  Sí      No
  │        │
  ▼        ▼
devolver  ¿Existe email?
usuario      │
          ┌──┴──┐
         Sí     No
         │       │
         ▼       ▼
      vincular  crear
      Google    usuario
```

Así se **evita crear dos cuentas para una misma persona**. Por ejemplo, si realizamos un registro normal con email `javier@example.com` y posteriormente le damos a "Continuar con Google", si el email coincide, se vincula la cuenta de Google a la cuenta existente.

#### `AuthService`

Este servicio coordina la **autenticación**. No debería encargarse de almacenar directamente usuarios o refresh tokens; orquesta los componentes que tienen esas responsabilidades.

##### Login

Delega en **Spring Security**:

```java
  authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
  );
```

```
LoginRequest
↓
AuthService
↓
AuthenticationManager :: Spring Security
↓
DaoAuthenticationProvider :: Spring Security
↓
UserDetailsService :: Spring Security
↓
UserRepository
↓
User
↓
PasswordEncoder / BCrypt
↓
credenciales correctas
```

Si las credenciales son incorrectas, lanza un `AuthenticationException → 401`

##### Refresh Token

El flujo de renovación es:

```
refresh token
      ↓
parsear JWT
      ↓
¿type = refresh?
      ↓
validar hash
      ↓
¿revoked = false?
      ↓
¿no expirado?
      ↓
revocar anterior
      ↓
generar access nuevo
      ↓
generar refresh nuevo
```

Esto se denomina **_Refresh Token Rotation_**. La característica importante es que el refresh token utilizado queda invalidado.

```
Refresh A
↓
usar Refresh A
↓
Revocar A
↓
crear Refresh B
```

Si alguien intenta volver a utilizar Refresh A, devolverá un código de error **401**, ya que `revoked = true`.

##### Logout

El logout no necesita destruir el `access token`, ya que es de corta duración y stateless. En cambio, se revoca el `refresh token` y se elimina la **cookie**.

#### `RefreshTokenService`

Es el servicio que persiste y valida los refresh tokens. Su regla de oro: **en la base de datos nunca se guarda el token en claro**, solo su hash SHA-256:

```java
public void store(User user, String rawToken, Instant expiresAt) {
    RefreshToken rt = new RefreshToken();
    rt.setUser(user);
    rt.setTokenHash(hash(rawToken));
    rt.setExpiresAt(expiresAt);
    rt.setRevoked(false);
    rt.setCreatedAt(Instant.now());
    repository.save(rt);
}
```

El método privado `hash()`:

```java
private String hash(String rawToken) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hash);   // 64 caracteres hexadecimales
}
```

SHA-256 es un **hash** (unidireccional): no se puede recuperar el token a partir del hash. Si la base de datos se filtra, los atacantes obtienen hashes inútiles.

Operaciones:

- `store(user, token, expiresAt)`: guarda el hash al emitir tokens.
- `isValid(token)`: busca el hash en BD y comprueba `!revoked && expiresAt.isAfter(now)`:

  ```java
  return repository.findByTokenHash(hash(rawToken))
          .map(rt -> !rt.isRevoked() && rt.getExpiresAt().isAfter(Instant.now()))
          .orElse(false);
  ```

- `revoke(token)`: localiza por hash y marca `revoked = true` (se usa en el logout y en la rotación).

> El flujo al hacer login: se genera el token en claro → se devuelve al cliente → en BD se guarda **solo el hash**. Cada validación posterior parte del token que envía el cliente y compara su hash con el almacenado.

### 1.5 — Seguridad

#### `JwtService`

Su función es **crear y validar JWT**.

Tiene dos métodos conceptuales: `generateAccessToken(user)` y `generateRefreshToken(user)`.

Ambos generan JWT, pero con diferentes tipo (`type`) y tiempo de vida (`TTL`). Por ejemplo:

```
access:
  type = access
  exp = +15 min

refresh:
  type = refresh
  exp = +7 días
```

##### Claims del JWT

El token contiene información como:

```java
.subject(user.getEmail())
.claim("uid", user.getId())
.claim("roles", user.getRoles().stream().map(Enum::name).toList())
.claim("type", type)
.claim("jti", UUID.randomUUID().toString())
.issuer(issuer)
.issuedAt(now)
.expiration(new Date(now.getTime() + ttl.toMillis()))
```

Conceptualmente:

```json
{
  "sub": "javier@example.com",
  "uid": 15,
  "roles": ["USER"],
  "type": "access",
  "jti": "...",
  "iss": "booksocial-identity",
  "iat": "...",
  "exp": "..."
}
```

- `sub`: Es el sujeto del token (`sub = email`).
  En este proyecto se utiliza el email como identidad principal. Por eso, la invocación del método `authentication.getName()` utilizado en `UserController.me(...)` devuelve el email.

- `uid`: Permite identificar al usuario mediante su ID interno (`uid = user.getId()`).

- `roles`: Incluye la lista de roles del usuario, por ejemplo `["USER"]` o `["ADMIN", "USER"]`.
  El `Gateway` puede utilizar estos claims para aplicar autorización sin consultar al `Identity Service` en cada petición.

- `type`: Este claim diferencia **_access_** de **_refresh_**. Es importante porque ambos son JWT firmados por la misma aplicación.
  El sistema no debe aceptar un _refresh token_ como si fuera un _access token_. El `Gateway` debe aceptar únicamente tokens de tipo **_access_**.

- `jti`: Es un identificador único del token (`jti = UUID`). Permite distinguir un JWT concreto de otro.
  Por ejemplo: `550e8400-e29b-41d4-a716-446655440000`.

##### Firma HS256

El token se firma utilizando `HS256` y `APP_JWT_SECRET`. La clave debe ser conocida tanto por `Identity Service` como por el `Gateway`.

```
Identity Service
       │
       │ firma JWT
       ▼
      JWT
       │
       ▼
Gateway
       │
       │ verifica firma
       ▼
   acepta/rechaza
```

Esto permite que el `Gateway` pueda validar el token sin llamar al `Identity Service` en cada request. El sistema es **_stateless_** para la validación del _access token_.

##### ¿Cómo se ve un JWT por dentro?

Un JWT es un string con **tres partes separadas por puntos**: `header.payload.signature`. Las tres están codificadas en **base64url** (los signos `+`, `/`, `=` se sustituyen por `-`, `_`, y se omiten los rellenos).

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYXZpZXJA... .SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
└────────────┬───────────┘ └──────────┬───────────┘ └──────────────┬──────────────┘
        header (algoritmo)       payload (claims)           firma (HS256)
```

1. **Header**: indica el algoritmo de firma, `{"alg":"HS256"}`.
2. **Payload**: los claims (el JSON que se vio arriba: `sub`, `uid`, `roles`, `type`, `jti`, `iss`, `iat`, `exp`). **Ojo**: base64url **no es cifrado** — cualquiera puede decodificarlo y leer el payload. Por eso los tokens solo llevan datos no sensibles (email, id, roles), y nunca el `password_hash`.
3. **Firma**: se calcula como `HMAC-SHA256(header + "." + payload, APP_JWT_SECRET)`. Si alguien modifica el payload, la firma ya no coincide y `jwtService.parse()` lanza `JwtException`.

Este diseño explica por qué **no se puede confiar en el payload sin verificar la firma**: la firma es lo que garantiza que el token no fue alterado y que fue emitido por quien conoce el secret.

#### `JwtAuthFilter`

Este filtro se ejecuta en cada petición protegida. Busca: `Authorization: Bearer <token>`.

```
Request
  │
  ▼
¿Authorization?
  │
  ├── No → continúa
  │
  └── Sí
       │
       ▼
    extraer JWT
       │
       ▼
    validar firma
       │
       ▼
    validar expiración
       │
       ▼
    validar issuer
       │
       ▼
    comprobar type=access
       │
       ▼
    extraer subject + roles
       │
       ▼
    Authentication
       │
       ▼
SecurityContextHolder
```

##### `SecurityContextHolder`

Cuando el filtro crea `UsernamePasswordAuthenticationToken`, lo coloca en `SecurityContextHolder`. A partir de ese momento, **Spring Security** considera que la request tiene una **identidad autenticada**.

Por eso un controller puede hacer `authentication.getName()` y obtener `javier@example.com`. **No necesita volver a leer el JWT manualmente**.

Si el token está **expirado**, tiene una **firma o issuer incorrectos**, es de **tipo _refresh_** o está **mal formado**, el contexto de seguridad **no se autentica**.

`AuthenticationEntryPoint` devuelve `401 Unauthorized`:

```json
{
  "error": "unauthorized",
  "message": "Authentication required"
}
```

#### `SecurityConfig`

Este componente establece las reglas generales.

##### BCrypt

Se utiliza tanto para almacenar como para comprobar contraseñas.

```java
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
```

##### `UserDetailsService`

En este proyecto, se carga el usuario por **email** desde el `UserRepository` y lo mapea a **Spring Security** (`User.withUsername...`), usando el **passwordHash** como `password` y los **roles** como `authorities`.

```java
@Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return email -> repository.findByEmail(email)
                .map(user -> User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .disabled(!user.isEnabled())
                        .authorities(user.getRoles().stream()
                                .map(role -> "ROLE_" + role.name())
                                .toArray(String[]::new))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
```

##### `DaoAuthenticationProvider`

Es el componente que realiza la autenticación tradicional de usuario/password.

```java
@Bean
DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
  DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
  provider.setPasswordEncoder(passwordEncoder);
  return provider;
}
```

##### `AuthenticationManager`

Es la puerta de entrada para realizar una autenticación programáticamente.

```java
  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }
```

Por eso, `AuthService` utiliza `AuthenticationManager` para solicitar a **Spring Security** que autentique un email y contraseña determinados.

##### `filterChain`: la cadena de filtros

`SecurityFilterChain` define las reglas de seguridad que se aplican a cada petición HTTP.

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http,
                                JwtAuthFilter jwtAuthFilter,
                                RestAuthenticationEntryPoint entryPoint,
                                OAuth2AuthenticationSuccessHandler successHandler,
                                OAuth2AuthenticationFailureHandler failureHandler) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .oauth2Login(o -> o.successHandler(successHandler).failureHandler(failureHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout",
                            "/auth/forgot-password", "/auth/reset-password",
                            "/oauth2/authorization/**", "/login/oauth2/code/**",
                            "/actuator/health").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

- **`csrf` desactivado**: la API se protege con tokens (`Authorization: Bearer`), no con cookies de sesión. El CSRF protege contra formularios forjados que aprovechan la cookie de sesión del navegador; ese mecanismo no existe aquí, así que desactivarlo es correcto.
- **`sessionCreationPolicy(IF_REQUIRED)`**: clave para OAuth2. Spring Security solo crea sesión HTTP si hace falta — y hace falta en el flujo de Google, porque el parámetro `state` (protección anti-CSRF de OAuth2) se guarda en la sesión durante el login. Por eso no se usa `STATELESS` como en el gateway.
- **`exceptionHandling`**: cualquier petición no autenticada responde a través de `RestAuthenticationEntryPoint` (JSON 401 en lugar de la página HTML por defecto de Spring Security).
- **`oauth2Login`**: conecta los handlers de éxito y fallo personalizados (sección 1.6).
- **`authorizeHttpRequests`**: `/auth/*`, las rutas de OAuth2 (`/oauth2/authorization/**`, `/login/oauth2/code/**`) y `/actuator/health` son públicas. Todo lo demás (`/users/**`) exige autenticación.
- **`addFilterBefore(jwtAuthFilter, ...)`**: inserta el filtro JWT antes del filtro de usuario/contraseña, de modo que la autenticación por token se resuelve antes de decidir el acceso.

#### `TokenCookieService`

El access token viaja en el header `Authorization`. El **refresh token** viaja en una **cookie httpOnly** que el navegador gestiona automáticamente:

```java
public ResponseCookie create(String refreshToken) {
    return ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)      // JS no puede leerla → inmune a ataques XSS
            .secure(false)       // false en local (http); en producción con https iría true
            .sameSite("Lax")     // no se envía en peticiones cross-site
            .path("/")           // se envía a todas las rutas del dominio
            .maxAge(jwtService.refreshTokenTtl().getSeconds())
            .build();
}
```

- `httpOnly`: el JavaScript del navegador no puede acceder a la cookie, por lo que un script malicioso (XSS) no puede robarla.
- `sameSite("Lax")`: la cookie no se envía en peticiones iniciadas desde otros sitios, reduciendo el riesgo de CSRF.
- `clear()`: devuelve una cookie vacía con `maxAge(0)`, lo que hace que el navegador la elimine (se usa en el logout).

### 1.6 — OAuth2 con Google

El flujo ocurre **en el navegador**, no vía API JSON:

```
Navegador
  → GET /oauth2/authorization/google (Identity Service :8081)
  → Google (login + consentimiento)
  → GET /login/oauth2/code/google?code=... (Identity Service)
  → handler de éxito/fallo
```

#### `OAuth2AuthenticationSuccessHandler`

Cuando Google devuelve el usuario autenticado:

```java
OAuth2User oauth2User = oauthToken.getPrincipal();
User user = userService.linkOrCreateOAuthUser(oauth2User.getAttributes());
TokenResponse tokens = authService.issueTokens(user);

response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken()).toString());
response.sendRedirect(frontendRedirect + "#access_token=" + tokens.accessToken());
```

1. Obtiene los atributos del usuario de Google.
2. `linkOrCreateOAuthUser(attrs)`: vincula o crea la cuenta (sección 1.4).
3. `issueTokens(user)`: emite los JWT propios del servicio.
4. Fija la cookie `refresh_token`.
5. Redirige al frontend con el access token en el **fragmento** de la URL: `http://localhost:4200/oauth2/callback#access_token=...`.

> El fragmento `#...` no viaja al servidor, por lo que el access token no queda expuesto en logs ni en el historial de red; Angular lo lee directamente desde el navegador.

#### `OAuth2AuthenticationFailureHandler`

Si el usuario cancela el consentimiento o el flujo falla:

```java
response.sendRedirect(frontendRedirect + "#error=access_denied");
```

El frontend recibe `#error=access_denied` y puede mostrar el mensaje correspondiente.

> Nota importante: `/oauth2/authorization/google` **no** pasa por el gateway; el frontend llama directamente a `:8081`. Que el gateway devuelva `401` en esa ruta es un comportamiento esperado y correcto, no un bug.

### 1.7 — Controllers, seed admin y manejo de errores

#### `AuthController` (`/auth`)

Expone los endpoints de autenticación:

| Endpoint                     | Descripción                                                                                                       |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `POST /auth/register`        | Registra un usuario, devuelve `201` + tokens + cookie refresh                                                     |
| `POST /auth/login`           | Autentica, devuelve `200` + tokens + cookie refresh                                                               |
| `POST /auth/refresh`         | Rota el refresh token (desde cookie **o** body) y emite un par nuevo                                              |
| `POST /auth/logout`          | Revoca el refresh token, limpia la cookie, devuelve `204`                                                         |
| `POST /auth/forgot-password` | Solicita restablecimiento: genera token (hash SHA-256) y envía email con enlace (devuelve `200` siempre)          |
| `POST /auth/reset-password`  | Cambia la contraseña con un token válido, no usado y no caducado (`INVALID_TOKEN`/`EXPIRED_TOKEN`/`ALREADY_USED`) |

Detalle de `/auth/refresh` y `/auth/logout`: admiten el refresh token desde la **cookie** `refresh_token` o desde el **body** JSON (`RefreshRequest`), lo que mantiene compatibilidad con clientes que no usan cookies:

```java
String refreshToken = refreshTokenCookie != null
        ? refreshTokenCookie
        : (request != null ? request.refreshToken() : null);
```

#### `UserController` (`/users`)

```java
@GetMapping("/me")
public UserResponse me(Authentication authentication) {
    return userService.toResponse(
            userService.findByEmail(authentication.getName()).orElseThrow());
}
```

`authentication.getName()` devuelve el **email** (el `sub` del JWT). El usuario se busca en BD y se devuelve como `UserResponse`. Es el endpoint "¿quién soy?" y el que cualquier otro microservicio podrá invocar (a través del gateway) para conocer al usuario autenticado.

#### `AdminDataInitializer`

`ApplicationRunner`: al arrancar la aplicación, si no existe el usuario con el email de `app.admin.email`, crea el **administrador** con roles `ADMIN` + `USER`. Es un seed de desarrollo para tener una cuenta privilegiada desde el primer momento.

```java
admin.setRoles(Set.of(Role.ADMIN, Role.USER));
```

#### `GlobalExceptionHandler`

`@RestControllerAdvice` centraliza la transformación de excepciones en **JSON uniforme**:

| Excepción                         | HTTP | Respuesta                                      |
| --------------------------------- | ---- | ---------------------------------------------- |
| `EmailAlreadyExistsException`     | 409  | `{"error":"email_already_exists"}`             |
| `InvalidRefreshTokenException`    | 401  | `{"error":"invalid_refresh_token"}`            |
| `AuthenticationException`         | 401  | `{"error":"bad_credentials"}`                  |
| `MethodArgumentNotValidException` | 400  | `{"error":"validation_failed","fields":{...}}` |

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                    f -> f.getField(),
                    f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid",
                    (a, b) -> a));
    return ResponseEntity.badRequest()
            .body(Map.of("error", "validation_failed", "fields", fields));
}
```

Sin este handler, un error de validación (p.ej. un JSON mal formado) podía caer en el `AuthenticationEntryPoint` y devolver un `401` confuso en lugar de un `400`. Con él, el frontend puede interpretar cualquier respuesta.

### 1.8 — Flujos completos

#### Registro

```
POST /auth/register { email, password, firstName, lastName, birthDate }
→ validación bean (@NotBlank, @Email, @Size, @NotNull, @Past)
→ ¿email ya existe? → 409 email_already_exists
→ BCrypt(password)
→ edad < 18 ? MINOR_USER : USER
→ guardar User
→ emitir access (15 min) + refresh (7 días)
→ guardar hash SHA-256 del refresh
→ Set-Cookie: refresh_token (httpOnly, SameSite=Lax)
→ 201 + TokenResponse
```

#### Login

```
POST /auth/login { email, password }
→ AuthenticationManager.authenticate(...)   // Spring Security
→ DaoAuthenticationProvider + UserDetailsService + BCrypt
→ ¿credenciales incorrectas? → 401 bad_credentials
→ emitir tokens + cookie
→ 200 + TokenResponse
```

#### Refresh con rotación

```
POST /auth/refresh (cookie o body con refresh token)
→ parsear JWT → ¿firma válida? → ¿issuer correcto? → ¿type == refresh?
→ ¿hash en BD? → ¿revoked == false? → ¿no expirado?
→ revocar el refresh usado (revoked = true)
→ emitir par nuevo + nueva cookie
```

Si alguien reutiliza un refresh ya rotado → `401 invalid_refresh_token`.

#### Logout

```
POST /auth/logout (cookie o body)
→ revocar el refresh token
→ Set-Cookie: refresh_token con maxAge(0) → el navegador la elimina
→ 204 No Content
```

El access token no se invalida en el servidor: es de corta duración (15 min) y stateless; expira solo.

#### Login con Google

```
Navegador → :8081/oauth2/authorization/google
→ Google (login + consentimiento)
→ :8081/login/oauth2/code/google
→ OAuth2AuthenticationSuccessHandler
   → linkOrCreateOAuthUser(attrs)   // por sub, luego por email
   → issueTokens(user) + cookie refresh
   → redirect a :4200/oauth2/callback#access_token=...
```

#### Acceso a un endpoint protegido

```
GET /users/me  con  Authorization: Bearer <access>
→ JwtAuthFilter: validar firma + issuer + type=access
→ SecurityContextHolder autenticado
→ UserController.me() → authentication.getName() → email
→ 200 UserResponse
```

### 1.9 — Reset de contraseña

Recuperación de contraseña por email. Vive en identity-service porque es el **dueño de las credenciales** (almacena `password_hash`, emite tokens): debe ser el único que pueda re-encontrar una contraseña. Expone dos endpoints `permitAll` que no requieren autenticación, porque en este flujo el usuario **aún no está logueado** (ha olvidado su contraseña).

**Qué aporta**: un mecanismo seguro de recuperación sin contraseña temporal en claro ni adivinable, con token de un solo uso, caducidad y hash en BD.

**Seguridad — el token nunca vive en claro en BD**:

- Se genera un token aleatorio de **32 bytes** (`SecureRandom`) y se envía **en crudo solo por email**.
- En base de datos solo se guarda el **hash SHA-256** del token. Así, un acceso a la BD no permite forjar enlaces de reset ni escalar privilegios.
- El token es de **un solo uso** (`used`) y **caduca a los 30 minutos** (`expiresAt`).

#### La entidad `PasswordResetToken`

```java
@Entity
@Table(name = "password_reset_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "tokenHash"))
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    // getters y setters
}
```

La `unique constraint` sobre `tokenHash` refuerza que cada token generado es distinto y no se puede reutilizar el mismo hash.

#### El repositorio

```java
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUserId(Long userId);
}
```

- `findByTokenHash` valida el token entrante (se busca por el hash, nunca por el valor en claro).
- `deleteByUserId` invalida los tokens previos de un usuario al pedir un nuevo reset.

#### El servicio `PasswordResetService`

```java
@Service
@Transactional
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;

    private final String from;
    private final String resetBaseUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder encoder,
                                JavaMailSender mailSender,
                                @Value("${app.mail.from}") String from,
                                @Value("${app.mail.reset-base-url}") String resetBaseUrl) {
        // asignaciones
    }

    public void requestReset(String email) {
        // Siempre termina sin error, aunque el email no exista (no revelar existencia).
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isEmpty()) return;

        User user = byEmail.get();
        tokenRepository.deleteByUserId(user.getId());

        String raw = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        token.setUsed(false);
        tokenRepository.save(token);

        sendEmail(user.getEmail(), raw);
    }

    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN"));
        if (token.isUsed()) throw new AlreadyUsedTokenException("ALREADY_USED");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new ExpiredTokenException("EXPIRED_TOKEN");

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN"));

        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

Puntos clave:

- **`requestReset` nunca falla** aunque el email no exista (anti-fingerprinting): no revela si una cuenta está registrada. Es la misma técnica que "email ya enviado" de los proveedores grandes.
- **Se borran tokens previos** (`deleteByUserId`) para que un segundo envío invalide el anterior.
- **`resetPassword` valida en orden**: hash existe → no usado → no caducado. Alcanzado, re-encodea con BCrypt (`PasswordEncoder`) y marca el token como usado para impedir reutilización.
- Si el token no existe, el código de error es el mismo `INVALID_TOKEN` tanto para un token inventado como para un user id inexistente: no se diferencia entre ambos casos.

#### DTOs de entrada

```java
public record ForgotPasswordRequest(@NotBlank @Email String email) {}

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {}
```

- `forgot-password` solo pide el email.
- `reset-password` pide el token (del query string) y la contraseña nueva, con un mínimo de 8 caracteres consistente con el registro.

#### Endpoints (`AuthController`, `@RequestMapping("/auth")`)

```java
@PostMapping("/forgot-password")
public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestReset(request.email());
    return ResponseEntity.ok().build();
}

@PostMapping("/reset-password")
public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.ok().build();
}
```

Ambos devuelven `200 OK` sin cuerpo. En `forgot-password` esto es deliberado: devuelve `200` **tanto si el email existe como si no**, de modo que un atacante no puede inferir si una cuenta está registrada observando el código de estado.

#### Permitir sin autenticación (`SecurityConfig`)

```java
.requestMatchers(
        "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout",
        "/auth/forgot-password", "/auth/reset-password",
        "/oauth2/authorization/**", "/login/oauth2/code/**",
        "/actuator/health").permitAll()
```

Los dos endpoints de reset se añaden a la lista de rutas públicas. El interceptor Angular tampoco les agrega `Authorization`, al estar fuera del flujo autenticado.

#### Manejo de errores (`GlobalExceptionHandler`)

| Excepción                   | HTTP  | `error` del body |
| --------------------------- | ----- | ---------------- |
| `InvalidTokenException`     | `400` | `INVALID_TOKEN`  |
| `ExpiredTokenException`     | `400` | `EXPIRED_TOKEN`  |
| `AlreadyUsedTokenException` | `400` | `ALREADY_USED`   |

```java
@ExceptionHandler(InvalidTokenException.class)
public ResponseEntity<Map<String, String>> invalidToken(InvalidTokenException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "INVALID_TOKEN"));
}
```

Las tres excepciones se mapean a `400` con un código estable en el campo `error`. El `message` interno de la excepción **no se expone** en la respuesta (no filtrar por qué falló el token). El frontend discrimina estos códigos para mostrar estados distintos.

#### El email (`templates/password-reset-email.html`)

`sendEmail` sustituye el placeholder `{{RESET_URL}}` por el enlace real y lo envía con `JavaMailSender`:

```java
private void sendEmail(String to, String rawToken) {
    try {
        String resetUrl = resetBaseUrl + "/reset-password?token=" + rawToken;
        ClassPathResource resource = new ClassPathResource("templates/password-reset-email.html");
        String html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8)
                .replace("{{RESET_URL}}", resetUrl);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject("BookSocial - Reset your password");
        helper.setText(html, true);
        mailSender.send(message);
    } catch (MessagingException | IOException e) {
        throw new RuntimeException("Failed to send reset email.", e);
    }
}
```

La plantilla es HTML con CSS inline (compatibilidad con clientes de correo) e incluye un botón "Reset your password" más un fallback del enlace como texto plano, indicando que el enlace es válido **30 minutos** y que puede ignorarse si no se solicitó.

#### Configuración (`.env` + `application.yml`)

Hay **dos bloques `mail`** distintos:

| Bloque        | Propiedades                                        | Variables de entorno                                       |
| ------------- | -------------------------------------------------- | ---------------------------------------------------------- |
| `spring.mail` | Configura el `JavaMailSender` (host, puerto, SMTP) | `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_PORT` |
| `app.mail`    | Custom: `from` y `reset-base-url` (para el enlace) | `MAIL_FROM`, `FRONTEND_URL`                                |

```yaml
spring:
  mail:
    host: ${MAIL_HOST:}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    port: ${MAIL_PORT:587}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  mail:
    from: ${MAIL_FROM:no-reply@booksocial.com}
    reset-base-url: ${FRONTEND_URL:http://localhost:4200}
```

Las variables se cargan en el contenedor vía `env_file: ../identity-service/.env` (en `docker-compose.yml`). `FRONTEND_URL` (p. ej. `http://localhost:4200`) se mapea a `app.mail.reset-base-url` y es la base del `{{RESET_URL}}`.

#### Flujo completo

```
Login  →  "Forgot password?" → /forgot-password (Angular)
   │
   │ POST /auth/forgot-password { email }
   ▼
PasswordResetService.requestReset(email)
   → (email no existe?) → return (200 igualmente)
   → borrar tokens previos del usuario
   → generar token raw de 32 bytes (SecureRandom)
   → guardar hash SHA-256 + expiresAt = now + 30 min + used=false
   → enviar email con {FRONTEND_URL}/reset-password?token=<raw>
   ▼
Usuario abre el enlace → /reset-password?token=<raw> (Angular)
   │
   │ POST /auth/reset-password { token, newPassword }
   ▼
PasswordResetService.resetPassword(raw, newPassword)
   → hash(raw) existe?  → no → INVALID_TOKEN
   → used?              → sí → ALREADY_USED
   → expiresAt < now?   → sí → EXPIRED_TOKEN
   → re-encodear contraseña con BCrypt + marcar token usado
   → 200 OK → "Password updated"
```

#### Verificación

- `mvnw -pl identity-service compile` OK.
- `curl -X POST localhost:8081/auth/forgot-password -H "Content-Type: application/json" -d '{"email":"alguien@test.com"}'` → `200` (devuelve `200` tanto si el email existe como si no).
- Recibir el email con el enlace `…/reset-password?token=<32-bytes-hex>`; abrirlo en el navegador (usa `FRONTEND_URL`).
- `curl -X POST localhost:8081/auth/reset-password -H "Content-Type: application/json" -d '{"token":"<raw>","newPassword":"nueva1234"}'` → `200`; repetir → `400 {"error":"ALREADY_USED"}`.
- Con un token modificado → `400 {"error":"INVALID_TOKEN"}`.

> Las páginas Angular (`/forgot-password` y `/reset-password`) se describen en [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md).

### 1.10 — Rate-limiting con Redis + Bucket4j

Los endpoints públicos de autenticación (`/auth/login`, `/auth/register`, `/auth/forgot-password`, `/auth/reset-password`) son un **objetivo de abuso**: no requieren token y realizan operaciones costosas (consultas, hashing BCrypt, envío de emails, generación de tokens). Sin protección, un atacante puede lanzar **fuerza bruta** contra el login o **inundar** el `forgot-password` con emails.

**Solución elegida**: limitar la tasa de peticiones por IP con el algoritmo **token bucket**, implementado de forma **distribuida** con **Redis + Bucket4j**. Al ser distribuido, el límite se comparte entre todas las réplicas del `identity-service`, a diferencia de un límite en memoria que solo contaría por instancia.

**Por qué Redis + Bucket4j y no otra opción**:

| Opción                | Problema                                                                 |
| --------------------- | ------------------------------------------------------------------------ |
| En memoria (`Map`)    | No se comparte entre réplicas; cada instancia tendría su propio contador |
| A nivel de gateway    | No distingue por servicio y complica el gateway con estado               |
| **Redis + Bucket4j**  | Distribuido, atómico (CAS), simple de integrar con Spring                |

#### `token bucket` en 30 segundos

Un bucket con capacidad `N` y un relleno (refill) de `N` tokens por intervalo. Cada petición consume 1 token; si el bucket está vacío se rechaza con `429`. El relleno puede ser:
- **Greedy**: reparte los tokens de forma continua en el tiempo (más preciso para ráfagas).
- **Intervally**: repone los `N` tokens de golpe al cumplirse el intervalo.

Para login/register (protección antibrute-force) el relleno **greedy** es el adecuado.

#### Paso 1 — Añadir Redis a la infraestructura

Redis 7 se añadió al `infrastructure/docker-compose.yml` (con healthcheck) y el servicio `identity-service` se conecta a él. El detalle del servicio `redis` en compose está en [GUIDE-INFRA.md](./GUIDE-INFRA.md) → Bloque 0. Para el backend lo relevante es que **identity debe conocer el host de Redis** (Spring Boot mapea `SPRING_REDIS_HOST` a `spring.data.redis.host`) y esperar a que esté sano:

```yaml
  identity-service:
    environment:
      SPRING_REDIS_HOST: redis
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
```

> Spring Boot 4 usa `spring.data.redis.host`. La variable de entorno `SPRING_REDIS_HOST` se mapea automáticamente, igual que el resto de propiedades de configuración.

#### Paso 2 — Dependencias en `identity-service/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-core</artifactId>
    <version>8.19.0</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j_jdk17-lettuce</artifactId>
    <version>8.19.0</version>
</dependency>
```

| Dependencia                    | Para qué sirve                                       |
| ------------------------------ | ---------------------------------------------------- |
| `spring-boot-starter-data-redis` | Aporta el cliente Lettuce (y la versión de Spring Data Redis) |
| `bucket4j_jdk17-core`          | Núcleo de Bucket4j (algoritmo token bucket)          |
| `bucket4j_jdk17-lettuce`       | Integración de Bucket4j con Lettuce (almacén CAS en Redis) |

> **Importante sobre versiones**: a partir de Bucket4j **8.12**, cada cliente de almacén se distribuye como artefacto separado (`-lettuce`, `-jedis`, ...). Ya **no** existe `bucket4j-redis` como en versiones antiguas. Como `spring-boot-starter-data-redis` trae Lettuce, se usa `bucket4j_jdk17-lettuce`.

#### Paso 3 — Configuración en `application.yml`

```yaml
spring:
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}

app:
  rate-limit:
    requests: 5
    period-seconds: 60
```

`requests: 5` por `period-seconds: 60` → 5 peticiones por minuto por IP.

#### Paso 4 — `RateLimitService`

Servicio que construye el `ProxyManager` distribuido y expone `tryConsume(key)`:

```java
@Service
public class RateLimitService {

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, byte[]> connection;
    private final ProxyManager<String> proxyManager;

    private final int requests;
    private final Duration period;

    public RateLimitService(@Value("${spring.data.redis.host:localhost}") String host,
                            @Value("${spring.data.redis.port:6379}") int port,
                            @Value("${app.rate-limit.requests:5}") int requests,
                            @Value("${app.rate-limit.period-seconds:60}") int periodSeconds) {

        this.redisClient = RedisClient.create(
                RedisURI.builder().withHost(host).withPort(port).build());

        this.connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        this.proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(5)))
                .build();

        this.requests = requests;
        this.period = Duration.ofSeconds(periodSeconds);
    }

    private final class RateLimitConfig implements Supplier<BucketConfiguration> {
        @Override
        public BucketConfiguration get() {
            return BucketConfiguration.builder()
                    .addLimit(limit ->
                            limit.capacity(requests).refillGreedy(requests, period))
                    .build();
        }
    }

    public boolean tryConsume(String key) {
        Bucket bucket = proxyManager.builder().build(key, new RateLimitConfig());
        return bucket.tryConsume(1);
    }

    @PreDestroy
    public void close() {
        connection.close();
        redisClient.shutdown();
    }
}
```

Claves del código:

- `RedisClient.create(RedisURI...)` crea el cliente Lettuce apuntando al host/port de Redis.
- La conexión se tipa como `StatefulRedisConnection<String, byte[]>` con `RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)`: la **clave** del bucket es un `String`, el **valor** (el estado del bucket) se guarda como `byte[]` en Redis.
- `Bucket4jLettuce.casBasedBuilder(connection)` construye el `ProxyManager`. El sufijo **CAS** (Compare-And-Swap) es el mecanismo que garantiza la atomicidad al actualizar el bucket: si dos peticiones concurrentes compiten, una gana y la otra reintenta.
- `expirationAfterWrite(...)` limpia los buckets de Redis que llevan un tiempo sin uso (evita que la clave por IP se acumule indefinidamente). Es un método de `AbstractProxyManagerBuilder`, la clase base de la que hereda el builder.
- `proxyManager.builder().build(key, configSupplier)` **no crea un bucket nuevo cada vez**: devuelve el existente si ya hay uno para esa `key` (el `ProxyManager` se encarga del caché y de la creación bajo demanda).
- `limit.capacity(n).refillGreedy(n, duration)` es el builder **fluido** de `Bandwidth` (API no deprecada en 8.19).

#### Paso 5 — `RateLimitFilter`

Un filtro de seguridad que intercepta las peticiones a los endpoints de auth y consume un token por IP:

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/reset-password");

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        if (rateLimitService.tryConsume(clientIp)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"too_many_requests\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return (commaIndex > 0) ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
```

- `shouldNotFilter` devuelve `false` solo para las rutas de auth: el resto de peticiones pasan sin consumir token.
- La `@Component` lo convierte en bean; se inyecta por constructor el `RateLimitService`.
- Si no quedan tokens → `429 TOO_MANY_REQUESTS` con JSON.

#### Paso 6 — Registro en `SecurityConfig`

Se inyecta el filtro en el método `filterChain` y se añade en la cadena **antes** del `JwtAuthFilter`:

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http,
                                JwtAuthFilter jwtAuthFilter,
                                RateLimitFilter rateLimitFilter,
                                RestAuthenticationEntryPoint entryPoint,
                                OAuth2AuthenticationSuccessHandler successHandler,
                                OAuth2AuthenticationFailureHandler failureHandler) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .oauth2Login(o -> o.successHandler(successHandler).failureHandler(failureHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/auth/register", "/auth/login", "/auth/refresh", "/auth/logout",
                            "/auth/forgot-password", "/auth/reset-password",
                            "/oauth2/authorization/**", "/login/oauth2/code/**",
                            "/actuator/health").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

**Orden de la cadena de filtros** (relevante porque el rate-limit debe aplicarse **antes** de tocar lógica de seguridad/negocio):

```
RateLimitFilter → JwtAuthFilter → UsernamePasswordAuthenticationFilter
```

> **Ojo con Spring Security 7**: hay que usar `addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)`, **no** `addFilterBefore(rateLimitFilter, JwtAuthFilter.class)`. En Spring Boot 4 / Spring Security 7, `addFilterBefore` con una clase de filtro propia (como `JwtAuthFilter`) lanza el error `The Filter class ... does not have a registered order`, porque el sistema de filtros ahora ordena beans y solo reconoce las clases estándar del framework como puntos de inserción. Al apuntar a `UsernamePasswordAuthenticationFilter` (una clase estándar), el `RateLimitFilter` queda registrado en la misma posición que `JwtAuthFilter` (ambos antes de ese filtro), ejecutándose primero en la práctica.

Si el rate-limit se pusiera después del `JwtAuthFilter`, los endpoints públicos (`permitAll`) ya habrían pasado por autenticación innecesaria. Al ir el primero, se corta la petición antes de cualquier otro procesamiento cuando se supera el límite.

#### Verificación

1. `mvnw -pl identity-service compile` → OK.
2. Levantar Redis: `docker compose -f infrastructure/docker-compose.yml up -d redis`.
3. Rebuild y arrancar el `identity-service`:
   `docker compose -f infrastructure/docker-compose.yml up -d --build identity-service`.
4. Guarda el body de la petición en un archivo (evita problemas de escapado de JSON en PowerShell/curl inline):
   ```bash
   Set-Content -LiteralPath "$env:TEMP\forgot.json" -Value '{"email":"alguien@test.com"}' -NoNewline -Encoding ascii
   ```
5. **Limpia el bucket** de tu IP para partir de cero (las claves del rate-limiter viven en Redis con la IP como nombre):
   ```bash
   docker exec booksocial-redis redis-cli keys "*"
   docker exec booksocial-redis redis-cli del "172.18.0.1"
   ```
6. Lanza 6 peticiones rápidas seguidas:
   ```bash
   for ($i=1; $i -le 6; $i++) {
     $code = curl.exe -s -o NUL -w "%{http_code}" `
       -X POST "http://localhost:8081/auth/forgot-password" `
       -H "Content-Type: application/json" `
       --data-binary "@$env:TEMP\forgot.json"
     Write-Output "Peticion $i -> HTTP $code"
   }
   ```
   Resultado esperado: peticiones **1-5 → `200`** y la **6ª → `429`** con `{"error":"too_many_requests"}`.

> La IP se resuelve con `getClientIp()`: usa el **primer** elemento del header `X-Forwarded-For` si viene (lista de proxys, el primero es el cliente original) y, si no, cae a `request.getRemoteAddr()`. Así, tras un proxy/gateway real cada cliente tiene su propio bucket (sin el header, el `remoteAddr` sería la IP del proxy y todas las peticiones compartirían bucket). **Nota de seguridad**: aceptar `X-Forwarded-For` sin confiar en el proxy permite spoofear la IP (`change this header` para saltarse el límite); en producción hay que sanitizarlo en el proxy de confianza.

> **Depuración en Redis**: los buckets se guardan en Redis con la IP del cliente como clave. Puedes inspeccionarlos con `docker exec booksocial-redis redis-cli keys "*"` para confirmar que el rate-limiter distribuido está escribiendo, y borrar una clave con `del <ip>` para reiniciar el bucket de esa IP.

## Bloque 2 — API Gateway

El **API Gateway** es el punto único de entrada del sistema. Después de construir el Identity Service, el siguiente paso es crear una capa que proteja todos los servicios detrás de una única dirección. El gateway resuelve el problema de que Angular no debe hablar directamente con los microservicios internos.

**Qué aporta**: enrutamiento por ruta, validación JWT centralizada, inyección de headers de confianza (`X-User-Id`, `X-User-Email`, `X-User-Roles`), y aislamiento de la infraestructura interna. Sin gateway, cada servicio tendría que validar JWT por su cuenta.

**Patrón clave**: _strip-then-assert_ — el gateway elimina cualquier header `X-User-*` que el cliente envíe y los reconstruye a partir del JWT validado. Los servicios downstream confían en estos headers.

**Ficha del servicio**

|                 |                                                                                                                                              |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8080`                                                                                                                                       |
| Persistencia    | Ninguna (stateless, valida el JWT con su clave compartida)                                                                                   |
| Responsabilidad | Punto único de entrada: enrutamiento por ruta a cada microservicio, validación JWT centralizada, inyección de headers de confianza           |
| Rutas           | `/auth/**` → identity · `/users,/profiles,/follows/**` → user · `/books,/authors/**` → book · `/reviews/**` → review · `/shelves/**` → shelf |
| Seguridad       | GETs públicos en `/books`, `/authors`, `/shelves`; resto requiere token; entry point devuelve 401 JSON                                       |

### 2.1 — Qué es un API Gateway y por qué aquí

Un **API Gateway** es el **punto único de entrada** del sistema. El cliente (Angular) solo conoce una dirección (`:8080`) y nunca habla directamente con los microservicios internos. El gateway se encarga de:

- **Enrutar**: dirigir cada petición al servicio correspondiente según la ruta.
- **Seguridad**: validar el JWT en cada request y rechazar los no autenticados.
- **Cross-cutting**: inyectar headers de contexto del usuario (`X-User-*`) que los servicios downstream consumen para saber quién llama.

Esto aporta: los microservicios quedan **ocultos** detrás del gateway, la autenticación se aplica en **un solo lugar** (sin duplicarla en cada servicio) y, en el futuro, se pueden añadir rate limiting, logs o métricas centralizadas.

```
Angular :4200
   │
   ▼
Gateway :8080  ─── ruta /auth/**, /users/** ───▶ Identity Service :8081
   │
   └── valida JWT, añade headers X-User-*
```

> Decisión de implementación: este proyecto usa **Spring Cloud Gateway en modo WebMVC** (`spring-cloud-starter-gateway-server-webmvc`), en lugar de la variante reactiva (WebFlux). Es la versión para stacks servlet, más sencilla de integrar con Spring Security clásico y suficiente para el enrutamiento que se necesita.

#### Dependencias del gateway (`pom.xml`)

El POM del gateway es más pequeño que el de identity-service porque el gateway **no persiste nada** ni tiene lógica de negocio:

| Dependencia                                  | Para qué sirve                                            |
| -------------------------------------------- | --------------------------------------------------------- |
| `spring-cloud-starter-gateway-server-webmvc` | El propio gateway (rutas y proxy)                         |
| `spring-boot-starter-security`               | Filtro JWT, `SecurityFilterChain`, entry point 401        |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson`    | **Solo para verificar** la firma de los JWT (JJWT 0.12.6) |
| `spring-boot-starter-actuator`               | `/actuator/health`                                        |
| `spring-boot-starter-security-test`          | Tests                                                     |

> Las versiones de las dependencias Spring Cloud se resuelven por el BOM `spring-cloud-dependencies:2025.1.2` importado en el parent POM (sección 0.2), así que no aparecen `<version>`.

#### Clase principal

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

Mismo patrón que identity-service: `@SpringBootApplication` + `main`. El paquete base es `com.booksocial.gateway`, donde viven el `SecurityConfig`, el `JwtService` y el filtro.

### 2.2 — `application.yaml` del gateway

```yaml
spring:
  application:
    name: gateway

  config:
    import: "optional:file:.env[.properties]" # mismo patrón de secretos que identity-service

  threads:
    virtual:
      enabled: true # hilos virtuales (Java 21)

  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: identity # nombre de la ruta
              uri: ${IDENTITY_SERVICE_URI:http://localhost:8081}
              predicates:
                - Path=/auth/**,/users/**

server:
  port: 8080

app:
  jwt:
    secret: ${APP_JWT_SECRET} # MISMO secret que identity-service
    issuer: booksocial-identity # MISMO issuer

management:
  endpoints:
    web:
      exposure:
        include: health
```

- **`routes`**: define un enrutado único `identity`. El predicado `Path=/auth/**,/users/**` hace que toda petición que empiece por `/auth/` o `/users/` se reenvíe al `uri` (identity-service). Cuando se añadan más servicios (user-service, book-service...), se añadirán aquí nuevas rutas con su propio predicado.
- **`${IDENTITY_SERVICE_URI:...}`**: en local apunta a `http://localhost:8081`; en Docker Compose se sobreescribe a `http://identity-service:8081` (nombre del contenedor).
- **`app.jwt.secret` + `app.jwt.issuer`**: deben ser **idénticos** a los de identity-service. El gateway necesita el mismo secret para poder **verificar la firma** de los tokens y el mismo issuer para rechazar tokens de otros emisores.
- **`spring.threads.virtual.enabled`**: habilita los hilos virtuales de Java 21, el modelo de concurrencia recomendado para servidores con muchas peticiones bloqueantes (I/O).

### 2.3 — `JwtService` del gateway: solo verifica, no genera

La **diferencia clave** respecto al de identity-service: este `JwtService` **no genera tokens** (no tiene `generateAccessToken` ni `generateRefreshToken`). Su única responsabilidad es **validar** los que emitió identity-service:

```java
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)          // 1. comprueba la firma HS256
                .requireIssuer(issuer)    // 2. exige iss == booksocial-identity
                .build()
                .parseSignedClaims(token) // 3. valida expiración
                .getPayload();
    }
}
```

`parse()` falla (lanza `JwtException`) si la firma no coincide, si el issuer es otro o si el token está expirado. Gracias al **secret compartido**, el gateway valida el token **sin llamar al identity-service en cada request**: la validación del access token es totalmente **stateless**.

### 2.4 — `JwtAuthFilter` del gateway y el patrón _strip-then-assert_

El filtro se ejecuta en cada petición y hace dos cosas: **autenticar** y **propagar la identidad**:

```java
String header = request.getHeader("Authorization");

if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);

    try {
        Claims claims = jwtService.parse(token);

        if (JwtService.TYPE_ACCESS.equals(claims.get("type", String.class))) {
            List<?> roles = (List<?>) claims.get("roles");

            if (roles != null && !roles.isEmpty()) {
                userId = String.valueOf(claims.get("uid"));
                userEmail = claims.getSubject();
                userRoles = roles.stream().map(String::valueOf)
                        .collect(Collectors.joining(","));

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    } catch (JwtException | IllegalArgumentException e) {
        SecurityContextHolder.clearContext();
    }
}
```

1. Extrae el token del header `Authorization: Bearer ...`.
2. `jwtService.parse(token)` valida firma + issuer + expiración.
3. **Solo acepta tokens de tipo `access`**: un refresh token jamás se acepta como access.
4. Lee `uid`, `subject` (email) y `roles` del claim y construye las `ROLE_*`.
5. Puebla el `SecurityContextHolder` (igual que en identity-service).
6. Construye un `UserHeadersRequestWrapper` y continúa la cadena con la petición **envuelta**.

> **Patrón _strip-then-assert_** (quitar-y-afirmar): el gateway **elimina** cualquier header `X-User-*` que el cliente envíe (un cliente podría falsificarlos) y los **reemplaza** por los valores derivados del JWT ya verificado. Así, el cliente **no puede suplantar** una identidad, porque cualquier `X-User-Id` que ponga será descartado.

### 2.5 — `UserHeadersRequestWrapper`

Es un `HttpServletRequestWrapper` que intercepta la lectura de headers:

```java
public static final String USER_ID_HEADER = "X-User-Id";
public static final String USER_EMAIL_HEADER = "X-User-Email";
public static final String USER_ROLES_HEADER = "X-User-Roles";
```

- **`getHeader(name)`**: para los tres nombres `X-User-*` devuelve el valor **calculado del JWT** (o `null` si no hay token), ignorando el que pudiera traer la petición original.
- **`getHeaders(name)`**: igual, pero en formato lista.
- **`getHeaderNames()`**: elimina de la lista de headers originales cualquier `X-User-*` y añade los tres (solo si el usuario está autenticado).

Resultado: cuando la petición llega a identity-service, este recibe headers `X-User-Id`, `X-User-Email`, `X-User-Roles` **fiables** (derivados de un JWT verificado). Cualquier microservicio futuro podrá confiar en ellos sin revalidar el token.

> Si no hay token válido, los tres valores son `null` y el wrapper no añade ninguno; la petición sigue sin autenticar y el `SecurityConfig` la rechaza con `401` si la ruta lo requiere.

### 2.6 — `SecurityConfig` del gateway

```java
http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/shelves/**").permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

- **`STATELESS`**: el gateway no mantiene sesión HTTP (a diferencia de identity-service, que la necesita para OAuth2). Cada petición es independiente.
- **`permitAll`** para `/auth/**` (login, register, refresh, logout — no pueden exigir token) y `/actuator/health` (healthcheck de Docker).
- **GETs públicos**: `/books/**` y `/shelves/**` permiten acceso sin autenticación para búsquedas y catálogo público. Los POST/PUT/DELETE siguen requiriendo token.
- **Todo lo demás autenticado**: cualquier ruta nueva hacia otros microservicios estará protegida por defecto.
- `RestAuthenticationEntryPoint` es idéntico al de identity-service: responde `401` con JSON `{"error":"unauthorized","message":"Authentication required"}`.

### 2.7 — Flujo completo de una petición a través del gateway

```
Angular
  │  POST /auth/login { email, password }        (sin token)
  ▼
Gateway :8080
  │  ruta /auth/** → permitAll
  │  reenvía a http://localhost:8081/auth/login
  ▼
Identity Service :8081
  │  valida credenciales → emite access + refresh
  │  respuesta + Set-Cookie refresh_token
  ▼
Gateway → Angular (200 TokenResponse)

--- (después del login) ---

Angular
  │  GET /users/me   Authorization: Bearer <access>
  ▼
Gateway :8080
  │  JwtAuthFilter: firma + issuer + type=access ✔
  │  → X-User-Id / X-User-Email / X-User-Roles (del JWT)
  │  ruta /users/** → reenvía a :8081/users/me
  ▼
Identity Service :8081
  │  lee X-User-Email → busca el usuario → UserResponse
  ▼
Gateway → Angular (200 UserResponse)
```

---

## Bloque 5 — Cierre: errores resueltos de la Fase 1

Retrospectiva de los errores más relevantes encontrados durante la Fase 1 (Bloques 0-4), con su causa y solución directa. Los errores específicos de Docker/CI/operación están en [GUIDE-INFRA.md](./GUIDE-INFRA.md).

1. **`POST /auth/logout` devolvía `401`**
   - Causa: `/auth/logout` no estaba en `permitAll` y el cliente no envía access token al cerrar sesión (solo la cookie).
   - Solución: añadir `/auth/logout` a la lista de `permitAll` del `SecurityConfig`.

2. **`IncorrectResultSizeDataAccessException ... expected 1 but found 3` al completar el login de Google**
   - Causa: el vínculo leía el atributo con la clave errónea (`attrs.get("googleId")`); el identificador persistente del OIDC claim es **`sub`**.
   - Solución: leer `attrs.get("sub")` en `linkOrCreateOAuthUser`.

3. **`401` al probar `/auth/register` y `/auth/login` con `curl.exe` desde PowerShell 5.1**
   - Causa: PowerShell elimina las comillas dobles al pasar JSON como argumento → Spring recibe JSON inválido y el `RestAuthenticationEntryPoint` responde `401` en lugar de `400`.
   - Solución: usar `Invoke-RestMethod` (con `ConvertTo-Json`) o guardar el body en un archivo y llamar `curl --data-binary "@body.json"`.

4. **`#error=access_denied` con una segunda cuenta de Google**
   - Causa: la app OAuth2 está en modo **Testing** y esa cuenta no estaba en los **Test users** de la pantalla de consentimiento.
   - Solución: añadir la cuenta en OAuth consent screen → Audience → Test users.

5. **`Process completed with exit code 126` en Actions**
   - Causa: `./mvnw` sin bit de ejecución en el runner Linux (se commiteó desde Windows).
   - Solución: `chmod +x mvnw` antes de ejecutar el wrapper en el workflow.

6. **`GET /oauth2/authorization/google` devuelve `401` a través del gateway**
   - No es un bug: el frontend llama a esa ruta **directamente contra `:8081`**, no vía gateway.

---

## Bloque 6 — Fase 2: user-service (perfil con CQRS dual-write)

**Objetivo**: construir `user-service` (puerto `8082`), propietario del perfil de usuario y las amistades. Arquitectura **CQRS**: PostgreSQL como _command side_ (escrituras) y MongoDB como _query side_ (lecturas). En esta fase la sincronización es **dual-write** (ambas escrituras en la misma operación) y se migrará a eventos con RabbitMQ en la sub-fase 2.4.

**Ficha del servicio**

|                 |                                                                                                                         |
| --------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8082`                                                                                                                  |
| Persistencia    | PostgreSQL (`profiles`, `follows`) + MongoDB (lecturas: `profiles`, `user_links`)                                       |
| Responsabilidad | Perfil de usuario (bio, avatar, preferencias) y grafo social de amistades (seguir/dejar de seguir, listas y contadores) |
| Endpoints clave | `GET /users/me`, `/profiles/**`, `POST/DELETE /follows/{username}`, `GET /follows/**`                                   |
| Mensajería      | Publica y consume eventos de amistad por RabbitMQ (sincronización de contadores)                                        |

### 6.1 — Esqueleto del user-service

#### Creación del proyecto

Se genera con Spring Initializr (Java 21, Spring Boot 4.1.0) con los starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation` y `actuator` (más sus variantes `-test`). Se descomprime en `user-service/` y se elimina el `.gitkeep`.

El `pom.xml` se reemplaza para heredar del parent del monorepo y añadir jjwt:

```xml
<parent>
  <groupId>com.booksocial</groupId>
  <artifactId>booksocial-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</parent>
```

Se añade `<module>user-service</module>` a los `<modules>` del `pom.xml` raíz. El build del módulo usa el wrapper:

```powershell
.\mvnw.cmd -B -pl user-service -am package -DskipTests
```

#### Estructura de paquetes

```
user-service/
├── .env                              # APP_JWT_SECRET
├── Dockerfile
├── pom.xml
└── src/main/java/com/booksocial/user/
    ├── config/
    │   ├── SecurityConfig.java       # STATELESS + JWT filter
    │   └── RabbitConfig.java         # exchange, queues, bindings
    ├── domain/
    │   ├── Profile.java              # JPA entity (Postgres)
    │   ├── Follow.java               # JPA entity (Postgres)
    │   ├── ProfileNotFoundException.java
    │   ├── SelfFollowException.java
    │   ├── AlreadyFollowingException.java
    │   └── NotFollowingException.java
    ├── readmodel/
    │   ├── ProfileReadModel.java     # Mongo document
    │   ├── FollowReadModel.java      # Mongo document
    │   ├── ProfileReadModelRepository.java
    │   └── FollowReadModelRepository.java
    ├── repository/
    │   ├── ProfileRepository.java    # JPA
    │   └── FollowRepository.java     # JPA
    ├── security/
    │   ├── JwtService.java           # parse-only (no emite)
    │   ├── JwtAuthFilter.java        # OncePerRequestFilter
    │   └── RestAuthenticationEntryPoint.java
    ├── service/
    │   ├── ProfileService.java
    │   └── FollowService.java
    ├── events/
    │   ├── FollowedEvent.java        # record
    │   ├── UnfollowedEvent.java      # record
    │   ├── FollowEventPublisher.java # RabbitTemplate
    │   └── FollowEventConsumer.java  # @RabbitListener
    └── web/
        ├── ProfileController.java
        ├── FollowController.java
        ├── GlobalExceptionHandler.java
        └── dto/
            ├── ProfileResponse.java
            ├── FollowResponse.java
            └── UpdateProfileRequest.java
```

#### Configuración (`application.yaml`)

```yaml
spring:
  application:
    name: user-service
  config:
    import: "optional:file:.env[.properties]"
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/booksocial}
    username: booksocial
    password: booksocial
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  mongodb:
    uri: ${SPRING_MONGODB_URI:mongodb://booksocial:booksocial@localhost:27017/booksocial?authSource=admin}
  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:guest}
    password: ${SPRING_RABBITMQ_PASSWORD:guest}
server:
  port: 8082
app:
  jwt:
    secret: ${APP_JWT_SECRET}
    issuer: booksocial-identity
management:
  endpoints:
    web:
      exposure:
        include: health
```

Puntos clave:

- Puerto `8082`.
- `spring.config.import: optional:file:.env[.properties]` para cargar secretos desde `.env` (mismo mecanismo que el resto de servicios).
- Datasource PostgreSQL `booksocial`/`booksocial`, overridable con `SPRING_DATASOURCE_URL`.
- `spring.mongodb.uri` con default local y override por `SPRING_MONGODB_URI`.
- `app.jwt.secret` (mismo `APP_JWT_SECRET` que identity-service) e `issuer: booksocial-identity`.

> **Clave de Spring Boot 4.1**: el prefijo de configuración de MongoDB **cambió** de `spring.data.mongodb.*` a **`spring.mongodb.*`** (variable de entorno correspondiente `SPRING_MONGODB_URI`). Usar el antiguo no se aplica y el servicio queda conectando a `localhost:27017`.

#### Seguridad (valida JWT, no emite)

A diferencia del identity-service, `user-service` **no emite tokens**: solo valida los emitidos por identity-service usando el mismo secreto y el mismo `issuer`. Las 4 clases de seguridad (`JwtService`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`, `SecurityConfig`) son idénticas en todos los servicios downstream.

> **Consulta el [Apéndice A](#apéndice-a--plantilla-de-seguridad-reutilizable) para el código completo de las clases de seguridad.** En esta sección solo se documentan las diferencias (si las hubiera).

En user-service no hay diferencias: se copian las 4 clases tal cual.

#### Identidad del usuario: `X-User-Id`

El gateway aplica el patrón **strip-then-assert**: elimina cualquier `X-User-*` entrante y los reconstruye a partir del JWT validado. Los servicios downstream (user-service) confían en estos headers:

- `X-User-Id` (Long, id del usuario en identity-service)
- `X-User-Email`

Por eso los endpoints usan `@RequestHeader("X-User-Id")` en lugar de leer claims: la autenticación ya la hizo el gateway y estos headers son de confianza (la ruta directa a `:8082` solo se usa en desarrollo).

#### Ruta en el gateway y contenerización

El gateway enruta `/profiles/**` y `/follows/**` → `${USER_SERVICE_URI:http://localhost:8082}`. En `docker-compose.yml` el gateway recibe `USER_SERVICE_URI: http://user-service:8082` (dentro de la red Docker el hostname es el nombre del servicio, no `localhost`).

El `Dockerfile` del user-service espeja el del identity-service (build `maven:3.9-eclipse-temurin-21` + runtime `eclipse-temurin:21-jre` con curl), empaquetando con `-pl user-service -am package`.

El servicio `user-service` en compose:

- `env_file: ../user-service/.env` (APP_JWT_SECRET) + overrides `SPRING_DATASOURCE_URL` y `SPRING_MONGODB_URI` apuntando a `postgres`/`mongodb` por hostname.
- `depends_on` de `postgres` y `mongodb` con `service_healthy`.
- Healthcheck `curl -f http://localhost:8082/actuator/health`.

> **Mongo root y `authSource`**: el usuario `booksocial` de Mongo se crea como **root** (en la base `admin`). Al conectar con URI `mongodb://booksocial:booksocial@host:27017/booksocial`, el driver autentica contra `booksocial` y falla con `Authentication failed`. Solución: **`?authSource=admin`** al final de la URI.

### 6.2 — Perfil con CQRS dual-write

#### El modelo en dos lados

**Command side — `domain/Profile` (Postgres)**: entidad JPA con `userId` único, `email`, `displayName`, `bio`, `location`, `avatarUrl` y timestamps. Es la fuente de verdad de las escrituras.

```java
@Entity
@Table(name = "profiles", uniqueConstraints = @UniqueConstraint(columnNames = {"userId"}))
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private String email;

    private String displayName;
    private String bio;
    private String location;
    private String avatarUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public void touch() { this.updatedAt = Instant.now(); }
}
```

- `userId` es una **foreign key lógica** al usuario de identity-service (único, pero no declarado como FK de JPA porque vive en otra base de datos).
- `touch()` actualiza `updatedAt` antes de cada escritura.

**Query side — `readmodel/ProfileReadModel` (Mongo)**: documento con `_id` = `userId`, los mismos campos de presentación y los contadores desnormalizados (`followersCount`, `followingCount`, `postsCount`). Se construye para lecturas baratas (un solo fetch, sin joins):

```java
@Document(collection = "profiles")
public class ProfileReadModel {
    @Id
    private String id;            // String.valueOf(userId)

    private Long userId;
    private String email;
    private String displayName;
    private String bio;
    private String location;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private long followersCount;
    private long followingCount;
    private int postsCount;
}
```

> **Dual-write**: en esta fase el `ProfileService` escribe **en la misma operación** en Postgres (JPA) y en Mongo (upsert del read model). No hay transacción distribuida ni eventos todavía: se acepta una **consistencia eventual débil** (si una de las dos escrituras falla, puede haber desfase temporal). Esto se sustituirá por eventos RabbitMQ en 2.4 (sin Outbox, limitación documentada).

#### Repositorios

```java
// PostgreSQL (command side)
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserId(Long userId);
}

// MongoDB (query side)
public interface ProfileReadModelRepository extends MongoRepository<ProfileReadModel, String> {
    Optional<ProfileReadModel> findByUserId(Long userId);
    List<ProfileReadModel> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String displayName, String email);
}
```

Ambos exponen `findByUserId`, pero la fuente de datos es distinta: JPA para comandos, Mongo para lecturas.

#### `ProfileService`

```java
@Service @Transactional
public class ProfileService {
    private final ProfileRepository profileRepository;
    private final ProfileReadModelRepository readModelRepository;

    public ProfileResponse getOrCreate(Long userId, String email) {
        Profile profile = findOrCreateProfile(userId, email);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse update(Long userId, String email, UpdateProfileRequest request) {
        Profile profile = findOrCreateProfile(userId, email);
        updateProfile(profile, request);
        profile.touch();
        profileRepository.save(profile);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse getByUserId(Long userId) {
        ProfileReadModel readModel = readModelRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile profile = profileRepository.findByUserId(userId).orElse(null);
                    if (profile == null) {
                        return placeholderReadModel(userId);   // transitorio: NO se persiste
                    }
                    return upsertReadModel(profile);
                });

        if (isSyntheticEmail(readModel.getEmail())) {
            readModel.setEmail(null);                          // purga correos falsos legados
            readModelRepository.save(readModel);
        }

        return toResponse(readModel);
    }

    private ProfileReadModel placeholderReadModel(Long userId) {
        ProfileReadModel readModel = new ProfileReadModel(userId, null);
        readModel.setDisplayName("user-" + userId);
        return readModel;
    }

    private Profile findOrCreateProfile(Long userId, String email) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> createProfile(userId, email));

        if (email != null && !email.isBlank()
                && (profile.getEmail() == null || profile.getEmail().isBlank()
                || isSyntheticEmail(profile.getEmail()))) {
            profile.setEmail(email);                           // repara con el correo real de identity
            if (("user-" + userId).equals(profile.getDisplayName())) {
                profile.setDisplayName(null);                  // deja que se derive del email
            }
            profile.touch();
            profileRepository.save(profile);
            upsertReadModel(profile);
        }
        return profile;
    }

    private Profile createProfile(Long userId, String email) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setEmail(email);
        return profileRepository.save(profile);
    }

    private void updateProfile(Profile profile, UpdateProfileRequest request) {
        if (request.displayName() != null) profile.setDisplayName(request.displayName());
        if (request.bio() != null)          profile.setBio(request.bio());
        if (request.location() != null)     profile.setLocation(request.location());
        if (request.avatarUrl() != null)    profile.setAvatarUrl(request.avatarUrl());
    }

    private ProfileReadModel upsertReadModel(Profile profile) {
        ProfileReadModel readModel = readModelRepository.findByUserId(profile.getUserId())
                .orElseGet(() -> new ProfileReadModel(profile.getUserId(), profile.getEmail()));
        readModel.setUserId(profile.getUserId());
        readModel.setEmail(isSyntheticEmail(profile.getEmail()) ? null : profile.getEmail());
        readModel.setDisplayName(deriveDisplayName(profile.getDisplayName(), profile.getEmail()));
        readModel.setBio(profile.getBio());
        readModel.setLocation(profile.getLocation());
        readModel.setAvatarUrl(profile.getAvatarUrl());
        readModel.setUpdatedAt(profile.getUpdatedAt());
        return readModelRepository.save(readModel);
    }

    private String deriveDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) return displayName;
        if (email != null) {
            int at = email.indexOf('@');
            if (at > 0) return email.substring(0, at);
        }
        return displayName;
    }

    private ProfileResponse toResponse(ProfileReadModel rm) {
        String email = isSyntheticEmail(rm.getEmail()) ? null : rm.getEmail();
        return new ProfileResponse(
                rm.getUserId(), email,
                deriveDisplayName(rm.getDisplayName(), email),
                rm.getBio(), rm.getLocation(), rm.getAvatarUrl(),
                rm.getFollowersCount(), rm.getFollowingCount(), rm.getPostsCount(),
                rm.getCreatedAt(), rm.getUpdatedAt());
    }

    private boolean isSyntheticEmail(String email) {
        return email != null && email.endsWith("@booksocial.local");
    }
}
```

- `getOrCreate(userId, email)`: si no existe el perfil en Postgres, lo crea; luego hace `upsertReadModel` (actualiza los campos de presentación del documento Mongo y lo guarda).
- `update(userId, email, request)`: crea el perfil si faltaba (misma semántica on-demand), aplica los campos del DTO (solo los no nulos) y vuelve a sincronizar el read model.
- `getByUserId(userId)`: **lee de Mongo** y, si el documento no existe, lo materializa on-demand desde Postgres; si tampoco hay perfil en Postgres devuelve un **read model transitorio** (`displayName: "user-{userId}"`, `email: null`) **sin persistirlo**.
- `findOrCreateProfile`: además de buscar+crear, **repara** el perfil con el email real de identity (`X-User-Email`) cuando está vacío, y si el email en Mongo aún es sintético (`@booksocial.local`) lo purga a `null` (compatibilidad hacia atrás); deja el `displayName` derivable del email si seguía siendo `user-{id}`.
- `searchProfiles(query)`: búsqueda en Mongo por `displayName` o `email` (insensible a mayúsculas) para el directorio **People** (`GET /profiles/search?q=`).
- `upsertReadModel`: si el documento Mongo no existe, lo crea con los campos base; si existe, actualiza todos los campos. Es idempotente. El `displayName` se persiste con `deriveDisplayName`, de modo que si el usuario no fijó un nombre propio se usa la parte local del email (p. ej. `social2@test.com` → `social2`). **Nunca persiste un email sintético** (`isSyntheticEmail` → `null`).
- `deriveDisplayName`: devuelve el `displayName` explícito si no está en blanco; si no, deriva la parte local del email (`email.substring(0, indexOf('@'))`). También se aplica en `toResponse` por si el read model persistido aún trae `null`.
- `toResponse`: mapea el read model a `ProfileResponse` usando `deriveDisplayName`, garantizando un `displayName` siempre poblado para el frontend, y **oculta cualquier email sintético** (lo devuelve como `null`).

#### `ProfileController`

```java
@RestController @RequestMapping("/profiles")
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader("X-User-Id") Long userId,
                              @RequestHeader("X-User-Email") String email) {
        return profileService.getOrCreate(userId, email);
    }

    @PutMapping("/me")
    public ProfileResponse updateMe(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Email") String email,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(userId, email, request);
    }

    @GetMapping("/{userId}")
    public ProfileResponse byUserId(@PathVariable Long userId) {
        return profileService.getByUserId(userId);
    }

    @GetMapping("/search")
    public List<ProfileResponse> searchUsers(@RequestParam("q") String q) {
        return profileService.searchProfiles(q);
    }
}
```

El `GlobalExceptionHandler` traduce `ProfileNotFoundException` a `404` JSON y los fallos de validación a `400` JSON.

#### DTOs

```java
public record UpdateProfileRequest(
    @Size(max = 60)  String displayName,
    @Size(max = 200) String bio,
    @Size(max = 80)  String location,
    @Size(max = 500) String avatarUrl) {}

public record ProfileResponse(Long userId, String email, String displayName, String bio,
    String location, String avatarUrl, long followersCount, long followingCount,
    int postsCount, Instant createdAt, Instant updatedAt) {}
```

Los DTOs son **records** — la forma idiomática en Java 21. `UpdateProfileRequest` usa `@Size` para validar la longitud de cada campo en la capa API; los mismos límites pueden replicarse en los formularios Angular como defensa en profundidad.

#### Verificación E2E (Docker)

Con un token real vía gateway (`POST /auth/login` → `accessToken`):

```
GET  /profiles/me   -> materializa el perfil on-demand (userId, email real, contadores a 0)
PUT  /profiles/me   -> actualiza displayName/bio/location/avatarUrl
GET  /profiles/10   -> devuelve el perfil leído de Mongo
GET  /profiles/search?q=javier  -> directorio People: lista perfiles por displayName/email (Mongo)
GET  /profiles/999  -> devuelve 200 {"displayName":"user-999","email":null} (read model transitorio,
                       SIN perfil falso persistido y SIN email inventado)
```

Se comprueba el dual-write consultando las dos bases directamente:

```sql
SELECT user_id, email, display_name FROM profiles;
```

```javascript
db.profiles.find({}, { userId: 1, displayName: 1, _id: 0 }).toArray();
```

### 6.3 — Errores encontrados en la Fase 2 (con solución directa)

1. **user-service conecta a `localhost:27017` aunque `SPRING_DATA_MONGODB_URI` está definida**
   - Causa: en Spring Boot 4.1 el prefijo de Mongo es `spring.mongodb.*`, no `spring.data.mongodb.*`; la variable correcta es `SPRING_MONGODB_URI`.
   - Solución: renombrar la propiedad en `application.yaml` y la variable en `docker-compose.yml`.

2. **`MongoCommandException ... AuthenticationFailed` (error 18)**
   - Causa: el usuario `booksocial` es root y se autentica contra `admin`; sin `?authSource=admin` el driver autentica contra la DB del URI (`booksocial`).
   - Solución: añadir `?authSource=admin` a la URI de Mongo.

### 6.4 — Amistades (follows)

#### Modelo dual

Igual que el perfil, el follow vive en los dos lados:

**Command side — `domain/Follow` (Postgres)**: entidad JPA con `followerId` y `followeeId`, unique constraint sobre la pareja `(followerId, followeeId)` y `createdAt`. Semántica: `followerId` sigue a `followeeId` (`follower → followee`).

```java
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "followeeId"}))
public class Follow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long followerId;

    @Column(nullable = false)
    private Long followeeId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
```

**Query side — `readmodel/FollowReadModel` (Mongo)**: documento con `_id` = `"<followerId>:<followeeId>"` (misma invariante de unicidad que Postgres, así Mongo tampoco admite duplicados), los dos ids y `createdAt`. Las listas de seguidores/siguiendo se leen de esta colección.

```java
@Document(collection = "follows")
public class FollowReadModel {
    @Id
    private String id;         // followerId + ":" + followeeId
    private Long followerId;
    private Long followeeId;
    private Instant createdAt;
}
```

#### Repositorios

```java
// PostgreSQL — solo escrituras y validaciones de integridad
public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}

// MongoDB — lecturas y contadores
public interface FollowReadModelRepository extends MongoRepository<FollowReadModel, String> {
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    Optional<FollowReadModel> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    List<FollowReadModel> findByFollowerId(Long followerId);
    List<FollowReadModel> findByFolloweeId(Long followeeId);
    long countByFollowerId(Long followerId);      // para contadores
    long countByFolloweeId(Long followeeId);
}
```

Los repos Mongo exponen métodos `countBy*` que se usan para **recalcular** contadores (no incrementar), haciendo la operación idempotente.

#### `FollowService`

```java
@Service @Transactional
public class FollowService {
    private final FollowRepository followRepository;
    private final FollowReadModelRepository followReadModelRepository;
    private final FollowEventPublisher eventPublisher;

    public FollowResponse follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) throw new SelfFollowException();
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, targetUserId))
            throw new AlreadyFollowingException(followerId, targetUserId);

        Follow follow = followRepository.save(new Follow(followerId, targetUserId));
        eventPublisher.publishFollowed(follow.getFollowerId(), follow.getFolloweeId());  // async RabbitMQ
        return toResponse(follow.getFollowerId(), follow.getFolloweeId(), follow.getCreatedAt());
    }

    public void unfollow(Long followerId, Long targetUserId) {
        Follow follow = followRepository
            .findByFollowerIdAndFolloweeId(followerId, targetUserId)
            .orElseThrow(() -> new NotFollowingException(followerId, targetUserId));
        followRepository.delete(follow);
        eventPublisher.publishUnfollowed(follow.getFollowerId(), follow.getFolloweeId());  // async RabbitMQ
    }

    public List<FollowResponse> followers(Long userId) {
        return followReadModelRepository.findByFolloweeId(userId).stream()
                .map(f -> toResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt()))
                .toList();  // solo Mongo (CQRS read)
    }

    public List<FollowResponse> following(Long userId) {
        return followReadModelRepository.findByFollowerId(userId).stream()
                .map(f -> toResponse(f.getFollowerId(), f.getFolloweeId(), f.getCreatedAt()))
                .toList();
    }

    private FollowResponse toResponse(Long followerId, Long followeeId, Instant createdAt) {
        return new FollowResponse(followerId, followeeId, createdAt);
    }
}
```

- `follow(followerId, targetUserId)`: rechaza el **self-follow** (`SelfFollowException` → 400) y el **duplicado** (`AlreadyFollowingException` → 409); en éxito escribe en Postgres y publica evento RabbitMQ.
- `unfollow(followerId, targetUserId)`: si no existe la relación lanza `NotFollowingException` → 404; si existe, borra en Postgres y publica evento.
- `followers(userId)` / `following(userId)`: leen **solo de Mongo** (ruta de lectura del CQRS).

Los **contadores** viven en el `ProfileReadModel` y se **recalculan** (no incrementan) en el consumidor: así un redelivery del broker no desvía los contadores.

#### `FollowController` (`/follows`)

```java
@RestController @RequestMapping("/follows")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{targetUserId}") @ResponseStatus(HttpStatus.CREATED)
    public FollowResponse follow(@RequestHeader("X-User-Id") Long followerId,
                                 @PathVariable Long targetUserId) {
        return followService.follow(followerId, targetUserId);
    }

    @DeleteMapping("/{targetUserId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@RequestHeader("X-User-Id") Long followerId,
                         @PathVariable Long targetUserId) {
        followService.unfollow(followerId, targetUserId);
    }

    @GetMapping("/{userId}/followers")
    public List<FollowResponse> followers(@PathVariable Long userId) {
        return followService.followers(userId);
    }

    @GetMapping("/{userId}/following")
    public List<FollowResponse> following(@PathVariable Long userId) {
        return followService.following(userId);
    }
}
```

| Método   | Ruta                          | Descripción                      |
| -------- | ----------------------------- | -------------------------------- |
| `POST`   | `/follows/{targetUserId}`     | Seguir a un usuario (201)        |
| `DELETE` | `/follows/{targetUserId}`     | Dejar de seguir (204)            |
| `GET`    | `/follows/{userId}/followers` | Lista quién sigue a este usuario |
| `GET`    | `/follows/{userId}/following` | Lista a quién sigue este usuario |

El gateway ya enruta `/follows/**` → user-service, por lo que no hay que tocar su configuración para esta sub-fase.

#### Excepciones

| Excepción                   | HTTP | Mensaje                            |
| --------------------------- | ---- | ---------------------------------- |
| `SelfFollowException`       | 400  | `"Cannot follow yourself"`         |
| `AlreadyFollowingException` | 409  | `"User X already follows Y"`       |
| `NotFollowingException`     | 404  | `"User X does not follow Y"`       |
| `ProfileNotFoundException`  | 404  | `"Profile not found for userId X"` |

#### Verificación E2E (Docker)

Con dos cuentas reales (`A` = e2e.final@test.com, `B` = cuenta registrada para el test):

```
POST   /follows/{B}         -> 201 {"followerId":10,"followeeId":19,...}
POST   /follows/{B} (rep.)  -> 409
POST   /follows/{A}         -> 400 (self-follow)
GET    /follows/{A}/following -> [ {followeeId: B} ]
GET    /follows/{B}/followers -> [ {followerId: A} ]
GET    /profiles/{A}        -> followingCount=1 ; GET /profiles/{B} -> followersCount=1
DELETE /follows/{B}         -> 204 ; repetición -> 404
```

Se comprueba el dual-write limpiando la relación en ambas bases (`SELECT count(*) FROM follows;` y `db.follows.countDocuments()` → 0 tras el unfollow).

### 6.5 — Eventos RabbitMQ (sincronización asíncrona de amistades)

En esta sub-fase la relación de amistad deja de escribirse en Mongo de forma síncrona: el comando escribe en Postgres y **publica un evento**; un **consumidor interno** actualiza Mongo y los contadores (eventual consistency).

#### Broker y configuración

- Dependencia `spring-boot-starter-amqp`. En Spring Boot 4.1 RabbitMQ **conserva** el prefijo `spring.rabbitmq.*` (env `SPRING_RABBITMQ_HOST`, etc.).
- Credenciales por defecto `guest`/`guest`: la imagen oficial de RabbitMQ trae `loopback_users.guest = false`, así que `guest` puede conectar desde cualquier contenedor de la red.

```java
@Configuration
public class RabbitConfig {
    public static final String EXCHANGE        = "booksocial.events";
    public static final String FOLLOWED_QUEUE   = "user-service.follows.followed";
    public static final String UNFOLLOWED_QUEUE = "user-service.follows.unfollowed";
    public static final String FOLLOWED_KEY     = "follow.followed";
    public static final String UNFOLLOWED_KEY   = "follow.unfollowed";

    @Bean TopicExchange eventsExchange() { ... }
    @Bean Queue followedQueue()           { ... }   // durable
    @Bean Queue unfollowedQueue()         { ... }   // durable
    @Bean Binding followedBinding()       { ... }
    @Bean Binding unfollowedBinding()     { ... }
    @Bean MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.user.events");
    }
}
```

Topología:

```
[booksocial.events] (Topic Exchange)
    ├── routing key "follow.followed"   → [user-service.follows.followed]
    └── routing key "follow.unfollowed" → [user-service.follows.unfollowed]
```

> **Una cola por evento**: si dos `@RabbitListener` escuchan la misma cola, RabbitMQ reparte los mensajes entre ellos al azar (no rutea por tipo). Con una cola por evento, cada listener recibe un tipo concreto y la deserialización es segura.

> **Converter en Spring AMQP 4.x**: `Jackson2JsonMessageConverter` está deprecado (marcado para borrar); el reemplazo es **`JacksonJsonMessageConverter`**, con el mismo constructor de trusted packages: `new JacksonJsonMessageConverter("com.booksocial.user.events")`.

#### _Events_

```java
public record FollowedEvent(Long followerId, Long followeeId, Instant occurredAt) {
    public FollowedEvent(Long followerId, Long followeeId) {
        this(followerId, followeeId, Instant.now());
    }
}

public record UnfollowedEvent(Long followerId, Long followeeId, Instant occurredAt) {
    public UnfollowedEvent(Long followerId, Long followeeId) {
        this(followerId, followeeId, Instant.now());
    }
}
```

Ambos son **records** con constructor de conveniencia que establece `occurredAt` de forma automática.

#### _Publisher_

```java
@Component
public class FollowEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishFollowed(Long followerId, Long followeeId) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.FOLLOWED_KEY,
                new FollowedEvent(followerId, followeeId));
    }

    public void publishUnfollowed(Long followerId, Long followeeId) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.UNFOLLOWED_KEY,
                new UnfollowedEvent(followerId, followeeId));
    }
}
```

Se publica **dentro de la misma transacción**: si `convertAndSend` falla, la excepción propaga y Postgres revierte → no hay evento fantasma. El único hueco es commit-tras-publish (la limitación del "sin Outbox").

#### _Consumer_

```java
@Component
public class FollowEventConsumer {
    private final FollowReadModelRepository followReadModelRepository;
    private final ProfileReadModelRepository profileReadModelRepository;

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void onFollowed(FollowedEvent event) {
        followReadModelRepository.save(new FollowReadModel(event.followerId(), event.followeeId()));
        syncCounters(event.followerId(), event.followeeId());
        log.info("Processed FollowedEvent: {} -> {}", event.followerId(), event.followeeId());
    }

    @RabbitListener(queues = RabbitConfig.UNFOLLOWED_QUEUE)
    public void onUnfollowed(UnfollowedEvent event) {
        followReadModelRepository.findByFollowerIdAndFolloweeId(event.followerId(), event.followeeId())
            .ifPresent(followReadModelRepository::delete);
        syncCounters(event.followerId(), event.followeeId());
        log.info("Processed UnfollowedEvent: {} -> {}", event.followerId(), event.followeeId());
    }

    private void syncCounters(Long followerId, Long followeeId) {
        profileReadModelRepository.findByUserId(followerId).ifPresent(p -> {
            p.setFollowingCount(followReadModelRepository.countByFollowerId(followerId));
            profileReadModelRepository.save(p);
        });
        profileReadModelRepository.findByUserId(followeeId).ifPresent(p -> {
            p.setFollowersCount(followReadModelRepository.countByFolloweeId(followeeId));
            profileReadModelRepository.save(p);
        });
    }
}
```

Los contadores se **recalculan** con `countByFollowerId`/`countByFolloweeId` en lugar de incrementar: así un redelivery del broker (at-least-once) no desvía los contadores. Es la diferencia clave con el dual-write síncrono anterior.

> `FollowService.follow`/`unfollow` ahora solo escriben en Postgres y publican el evento; `followers`/`following` siguen leyendo Mongo (consistencia eventual).

#### Verificación E2E (Docker)

```
POST   /follows/{B}   -> 201
(espera ~1-2s por el consumidor)
Mongo: db.follows -> { _id: '10:19' }
contadores: A.following=1, B.followers=1
DELETE /follows/{B}  -> 204  ->  Mongo: 0 documentos, contadores a 0
```

En el broker se comprueba que las colas existen y quedan drenadas (0 mensajes) y los bindings del exchange:

```
rabbitmqctl list_bindings   # booksocial.events -> user-service.follows.followed (follow.followed)
                            # booksocial.events -> user-service.follows.unfollowed (follow.unfollowed)
```

Y en los logs del servicio: `Processed FollowedEvent: 10 -> 19` / `Processed UnfollowedEvent: 10 -> 19`.

### Decisiones de diseño de la Fase 2 (resumen)

- **user-service propietario del perfil**: Postgres para comandos, Mongo para lecturas (CQRS con dual-write en esta sub-fase).
- **Validación JWT delegada al gateway + headers de confianza**: el servicio downstream confía en `X-User-Id`/`X-User-Email` (patrón strip-then-assert).
- **Creación on-demand del perfil**: `GET/PUT /profiles/me` materializan el perfil en el primer acceso; no hace falta endpoint de alta.
- **Amistades sincronizadas por eventos**: `follow`/`unfollow` escriben en Postgres y publican `FollowedEvent`/`UnfollowedEvent`; un consumidor actualiza Mongo y los contadores de forma idempotente (recalculados con `countBy*`). Sin Outbox (limitación documentada: hueco commit-tras-publish). El perfil permanece en dual-write.

### 6.6 — Directorio People: búsqueda de perfiles y perfil público

El directorio **People** permite buscar usuarios por nombre o email (directorio) y consultar su perfil público. Vive en el user-service porque es el **propietario de los perfiles** (read model en Mongo) y de las amistades (`follows`, sección 6.4). El gateway enruta `/profiles/**` y `/follows/**` hacia user-service (`:8082`).

**Qué aporta**: un punto de descubrimiento social — buscar a otros lectores, ver su perfil público y (combinado con 6.4) seguirles. El `ProfileController` expone el perfil propio (`/profiles/me`), el perfil público de un usuario (`/profiles/{id}`) y la búsqueda (`/profiles/search`).

#### Endpoints (`ProfileController`)

```java
@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse me(@RequestHeader("X-User-Id") Long userId,
                              @RequestHeader("X-User-Email") String email) {
        return profileService.getOrCreate(userId, email);
    }

    @PutMapping("/me")
    public ProfileResponse updateMe(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Email") String email,
                                    @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(userId, email, request);
    }

    @GetMapping("/{userId}")
    public ProfileResponse byUserId(@PathVariable("userId") Long userId) {
        return profileService.getByUserId(userId);
    }

    @GetMapping("/search")
    public List<ProfileResponse> searchUsers(@RequestParam("q") String q) {
        return profileService.searchProfiles(q);
    }
}
```

| Método | Ruta                     | Descripción                          | Identifica al usuario        |
| ------ | ------------------------ | ------------------------------------ | ---------------------------- |
| `GET`  | `/profiles/me`           | Perfil propio (lo crea si no existe) | `X-User-Id` + `X-User-Email` |
| `PUT`  | `/profiles/me`           | Actualizar perfil propio             | `X-User-Id` + `X-User-Email` |
| `GET`  | `/profiles/{userId}`     | Perfil público de un usuario         | `userId` (path)              |
| `GET`  | `/profiles/search?q=...` | Buscar perfiles por nombre o email   | `q` (query param)            |

> Nota del ruteo del gateway: la API del identity pasa a `/api/users/me` con `pathRewrite` `/api/users → /users`, de modo que la ruta SPA `/users` (People) **no colisiona** con el proxy; las llamadas a `/profiles/**` y `/follows/**` van directas a user-service.

#### El read model de perfil en Mongo (`ProfileReadModel`)

La búsqueda y el perfil que se muestran se leen del **read model** en Mongo (colección `profiles`, `_id = String.valueOf(userId)`):

```java
@Document(collection = "profiles")
public class ProfileReadModel {
    @Id
    private String id;          // String.valueOf(userId)
    private Long userId;
    private String email;       // null si es sintético (@booksocial.local)
    private String displayName;
    private String bio;
    private String location;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private long followersCount;   // sincronizado vía eventos (6.5)
    private long followingCount;   // sincronizado vía eventos (6.5)
    private int postsCount;
    // getters y setters
}
```

Campos destacados:

| Campo                                      | Origen                                                               |
| ------------------------------------------ | -------------------------------------------------------------------- |
| `userId`                                   | Id del usuario en el identity-service                                |
| `email`                                    | `null` si el perfil aún tiene el email sintético `@booksocial.local` |
| `followersCount`/`followingCount`          | Contadores actualizados por el `FollowEventConsumer` (sección 6.5)   |
| `displayName`/`bio`/`location`/`avatarUrl` | Datos públicos editables por el usuario                              |

#### El repositorio de búsqueda

```java
public interface ProfileReadModelRepository extends MongoRepository<ProfileReadModel, String> {
    Optional<ProfileReadModel> findByUserId(Long userId);
    List<ProfileReadModel> findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String displayName, String email);
}
```

`findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase` es el motor de la barra "People": busca con `CONTAINS` case-insensitive por `displayName` **o** por email. Al pasar `q=''`, devuelve todos los perfiles (lista inicial del directorio).

#### El servicio `ProfileService` (búsqueda y perfil público)

```java
@Service
@Transactional
public class ProfileService {

    public ProfileResponse getOrCreate(Long userId, String email) {
        Profile profile = findOrCreateProfile(userId, email);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse update(Long userId, String email, UpdateProfileRequest request) {
        Profile profile = findOrCreateProfile(userId, email);
        // aplica displayName/bio/location/avatarUrl no nulos
        profile.touch();
        profileRepository.save(profile);
        return toResponse(upsertReadModel(profile));
    }

    public ProfileResponse getByUserId(Long userId) {
        ProfileReadModel readModel = readModelRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Profile profile = profileRepository.findByUserId(userId).orElse(null);
                    if (profile == null) {
                        return placeholderReadModel(userId);   // perfil "fantasma", sin email falso
                    }
                    return upsertReadModel(profile);
                });

        if (isSyntheticEmail(readModel.getEmail())) {
            readModel.setEmail(null);          // no exponer el email sintético al público
            readModelRepository.save(readModel);
        }
        return toResponse(readModel);
    }

    public List<ProfileResponse> searchProfiles(String query) {
        return readModelRepository
                .findByDisplayNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream().map(this::toResponse).toList();
    }

    private ProfileReadModel placeholderReadModel(Long userId) {
        ProfileReadModel readModel = new ProfileReadModel(userId, null);
        readModel.setDisplayName("user-" + userId);
        return readModel;
    }
    // isSyntheticEmail(email): email != null && email.endsWith("@booksocial.local")
}
```

Puntos clave:

- **`searchProfiles(q)`** lee solo del read model en Mongo (consultas de lectura baratas y rápidas) y devuelve `List<ProfileResponse>`.
- **`getByUserId`** es el perfil público: prioriza Mongo, si no existe cae a JPA, y si tampoco crea un **placeholder** con nombre `user-{id}` — así un usuario que aparece como actor en el feed o que no ha completado su perfil igualmente tiene una ficha pública.
- **Los emails sintéticos `@booksocial.local` no se exponen**: en `getByUserId` se setean a `null` antes de responder, y `toResponse` también los filtra. La lógica de perfil transitorio (no persistido) se documenta en la sección 6.2; aquí solo se asegura que el directorio público nunca devuelva ese email falso.
- `GET /profiles/me` **materializa** el perfil: `getOrCreate` lo crea en JPA + Mongo si aún no existe. El frontend lo llama (fire-and-forget) al iniciar sesión para que los recién registrados aparezcan en People sin tener que editar antes su perfil (ver su `AuthService.applyToken()`).

#### La respuesta `ProfileResponse`

```java
public record ProfileResponse(
        Long userId, String email, String displayName,
        String bio, String location, String avatarUrl,
        long followersCount, long followingCount, int postsCount,
        Instant createdAt, Instant updatedAt
) {}
```

#### Manejo de errores

En `GlobalExceptionHandler` del user-service:

| Excepción                   | HTTP  | `error` del body |
| --------------------------- | ----- | ---------------- |
| `ProfileNotFoundException`  | `404` | `not_found`      |
| `SelfFollowException`       | `400` | `bad_request`    |
| `AlreadyFollowingException` | `409` | `conflict`       |
| `NotFollowingException`     | `404` | `not_found`      |

La búsqueda y los follows ya existían en la Fase 2; aquí se usan como base del directorio. El botón Follow del frontend reutiliza los endpoints de la sección 6.4 (`POST/DELETE /follows/{targetUserId}`).

#### Verificación

- `mvnw -pl user-service compile` OK.
- `curl "localhost:8080/profiles/search?q=alice"` → lista de perfiles que contienen "alice" en nombre o email.
- `curl "localhost:8080/profiles/search?q="` → todos los perfiles (directorio inicial).
- `curl "localhost:8080/profiles/2"` → ficha pública del usuario 2; comprobar que **ningún** perfil expone un email `@booksocial.local`.
- `curl "localhost:8080/profiles/me"` con header `X-User-Id: 1, X-User-Email: a@b.com` → materializa el perfil.

> El frontend de People (ruta `/users`, perfil de usuario `/users/:id`, botón Follow, `follow.service`) se describe en [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md).

---

## Bloque 7 — book-service (catálogo de libros con CQRS)

La Fase 3 replica el patrón de la Fase 2 en un nuevo microservicio: **Postgres para comandos, Mongo para lecturas/búsquedas**, con una entidad `Author` independiente integrada con **Open Library API** para enriquecer el catálogo con datos biográficos de autores. El servicio también integra **Google Books API** para auto-import de libros por ISBN.

**Ficha del servicio**

|                 |                                                                                                                                                             |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8083`                                                                                                                                                      |
| Persistencia    | PostgreSQL (`books`, `authors`) + MongoDB (lecturas: `books`, `authors`)                                                                                    |
| Responsabilidad | Catálogo CQRS: búsqueda local y externa, alta ADMIN, auto-import por ISBN desde Google Books; fichas y obras de autores vía Open Library con cache en Mongo |
| Endpoints clave | `GET /books/search`, `GET /books/search/full`, `GET /books/{isbn}`, `/authors/**` (search, `id/{authorId}`, detalle, works), `POST /books`                  |
| Mensajería      | Publica `BookCreatedEvent` → lo consumen review-service y shelf-service                                                                                     |

### 7.1 — Esqueleto del book-service

#### Creación y estructura

Mismo procedimiento que 6.1, con estas variantes:

- Generado con Spring Initializr (Java 21, Spring Boot 4.1.0) con los starters `webmvc`, `data-jpa`, `data-mongodb`, `security`, `validation` y `actuator` (más `-test`). Parent `booksocial-parent`, módulo `book-service` en el POM raíz.
- Puerto **`8083`**.
- Dependencia `spring-boot-starter-amqp` (para publicar eventos a RabbitMQ).

```
book-service/
├── .env                              # APP_JWT_SECRET, GOOGLE_BOOKS_API_KEY
├── Dockerfile
├── pom.xml
└── src/main/java/com/booksocial/book/
    ├── BookServiceApplication.java   # @EnableConfigurationProperties
    ├── config/
    │   ├── SecurityConfig.java       # parse-only JWT (copiado de user-service)
    │   ├── RabbitConfig.java         # exchange + MessageConverter (sin colas)
    │   ├── GoogleBooksProperties.java
    │   ├── OpenLibraryProperties.java
    │   └── BookDataSeeder.java       # CommandLineRunner — 8 libros de ejemplo
    ├── domain/
    │   ├── Author.java               # JPA entity (Postgres authors)
    │   ├── Book.java                 # JPA entity (Postgres books, FK → authors)
    │   ├── BookNotFoundException.java
    │   ├── BookAlreadyExistsException.java
    │   └── ForbiddenException.java
    ├── readmodel/
    │   ├── AuthorReadModel.java      # Mongo document (authors)
    │   ├── AuthorReadModelRepository.java
    │   ├── BookReadModel.java        # Mongo document (_id = isbn)
    │   └── BookReadModelRepository.java
    ├── repository/
    │   ├── AuthorRepository.java     # JPA
    │   └── BookRepository.java       # JPA
    ├── security/
    │   ├── JwtService.java
    │   ├── JwtAuthFilter.java
    │   └── RestAuthenticationEntryPoint.java
    ├── service/
    │   ├── BookService.java
    │   ├── AuthorService.java
    │   ├── google/
    │   │   ├── GoogleBooksClient.java
    │   │   ├── GoogleBooksMapper.java
    │   │   └── GoogleBooksResponse.java
    │   └── openlibrary/
    │       ├── OpenLibraryClient.java
    │       ├── OpenLibraryMapper.java
    │       ├── OpenLibraryResponse.java
    │       ├── AuthorDetailResponse.java
    │       └── WorksResponse.java
    ├── events/
    │   ├── BookCreatedEvent.java     # record (isbn, title, authorName, authorId)
    │   └── BookEventPublisher.java   # RabbitTemplate
    └── web/
        ├── BookController.java
        ├── AuthorController.java
        ├── GlobalExceptionHandler.java
        └── dto/
            ├── AuthorResponse.java
            ├── BookResponse.java
            └── CreateBookRequest.java
```

#### Configuración

`application.yaml` idéntico al de user-service cambiando el nombre del servicio y el puerto:

```yaml
server:
  port: 8083
spring:
  application:
    name: book-service
  # ... mismo patrón: .env import, datasource, mongodb, rabbitmq, jwt
app:
  google-books:
    api-key: ${GOOGLE_BOOKS_API_KEY:}
    api-url: https://www.googleapis.com/books/v1/
  open-library:
    api-url: https://openlibrary.org
    user-agent: booksocial/1.0 (javierincio.dev@gmail.com)
```

Gateway: ruta `Path=/books/**,/authors/**` → `${BOOK_SERVICE_URI:http://localhost:8083}` y variable `BOOK_SERVICE_URI: http://book-service:8083` en el compose del gateway.

Dockerfile y servicio de compose espejo de user-service (healthcheck curl a `/actuator/health` en `8083`, `depends_on` postgres y mongodb `service_healthy`).

#### Seguridad

La seguridad es **parse-only**: `JwtService`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`, `SecurityConfig` (consultar el [Apéndice A](#apéndice-a--plantilla-de-seguridad-reutilizable)). El control de acceso a `POST /books` se hace en el **controlador** leyendo el header `X-User-Roles` del gateway, no en SecurityConfig.

### 7.2 — Catálogo CQRS con búsqueda

#### Command side — `domain/Author` (Postgres)

```java
@Entity
@Table(name = "authors")
public class Author {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "open_library_id", unique = true)
    private String openLibraryId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "birth_date")
    private String birthDate;

    @Column(name = "death_date")
    private String deathDate;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "top_subjects")
    private String topSubjects; // JSON serializado

    @Column(name = "work_count")
    private Integer workCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Author() {}  // JPA requiere constructor vacío
}
```

> **Por qué no getters/setters**: el GUIDE muestra los campos relevantes. El código real incluye getters/setters completos (JavaBean convention). Se omiten aquí por brevedad, igual que en `Book`.

`Author` es una entidad independiente con datos biográficos procedentes de Open Library. `openLibraryId` es único y se usa como clave de cache. Los autores se crean bajo demanda cuando se importa un libro desde Google Books o cuando se busca un autor en Open Library.

#### Command side — `domain/Book` (Postgres)

```java
@Entity
@Table(name = "books", uniqueConstraints = @UniqueConstraint(columnNames = "isbn"))
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(name = "author_id")
    private Long authorId;          // FK lógica → authors.id

    @Column(columnDefinition = "text")
    private String description;

    private String coverUrl;
    private Integer publishedYear;
    private String category;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Book(String isbn, String title, Long authorId, String description,
                String coverUrl, Integer publishedYear, String category) {
        this.isbn = isbn;
        this.title = title;
        this.authorId = authorId;
        this.description = description;
        this.coverUrl = coverUrl;
        this.publishedYear = publishedYear;
        this.category = category;
        this.createdAt = Instant.now();
    }
}
```

`isbn` tiene `uniqueConstraints` en la tabla; `createdAt` se fija en el constructor y no se actualiza (`updatable = false`). `authorId` es la FK lógica a `authors.id` — no es una FK de JPA con `@ManyToOne` porque se resuelve por código (on-demand) y la relación es ligera.

#### Query side — `readmodel/BookReadModel` (Mongo)

```java
@Document(collection = "books")
public class BookReadModel {
    @Id
    private String isbn;     // ISBN como _id de Mongo
    private String title;
    private String authorName;
    private String authorId;  // String.valueOf(author.id)
    private String description;
    private String coverUrl;
    private Integer publishedYear;
    private String category;
    private Instant createdAt;
}
```

`isbn` como `_id` de Mongo: las búsquedas por ISBN son directas. `authorName` y `authorId` se desnormalizan del `Author` para que las lecturas desde Mongo no necesiten joins.

#### Query side — `readmodel/AuthorReadModel` (Mongo)

```java
@Document(collection = "authors")
public class AuthorReadModel {
    @Id
    private String openLibraryId;     // Id de OpenLibrary como _id de Mongo
    private String name;
    private String bio;
    private String birthDate;
    private String deathDate;
    private String photoUrl;
    private List<String> topSubjects;
    private Integer workCount;
    private Instant cachedAt;

    public AuthorReadModel() {}  // Mongo requiere constructor vacío
}
```

#### Repositorios

```java
// PostgreSQL — Authors
public interface AuthorRepository extends JpaRepository<Author, Long> {
    // SELECT * FROM authors WHERE open_library_id = :openLibraryId;
    Optional<Author> findByOpenLibraryId(String openLibraryId);

    // SELECT * FROM authors WHERE LOWER(name) LIKE LOWER('%' || :name || '%')
    List<Author> findByNameContainingIgnoreCase(String name);
}

// PostgreSQL — Books
public interface BookRepository extends JpaRepository<Book, Long> {
    // SELECT * FROM books WHERE isbn = :isbn;
    Optional<Book> findByIsbn(String isbn);

    // SELECT EXISTS (SELECT 1 FROM books WHERE isbn = :isbn);
    boolean existsByIsbn(String isbn);
}

// MongoDB — Author read model
public interface AuthorReadModelRepository extends MongoRepository<AuthorReadModel, String> {
    // db.authors.find({ name: { $regex: "<name>", $options: "i" } })
    List<AuthorReadModel> findByNameContainingIgnoreCase(String name);
}

// MongoDB — Book read model
public interface BookReadModelRepository extends MongoRepository<BookReadModel, String> {
  /*
    db.books.find({
      $or: [
          { title: { $regex: "<title>", $options: "i" } },
          { authorName: { $regex: "<author>", $options: "i" } }
      ]
  })
  */
  List<BookReadModel> findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(
        String title, String authorName);
}
```

La consulta derivada de Spring Data genera un `$regex` en Mongo con `i` (case-insensitive) sobre título **o** nombre de autor.

#### `BookService`

```java
@Service @Transactional
public class BookService {
    private final BookRepository bookRepository;
    private final BookReadModelRepository readModelRepository;
    private final BookEventPublisher bookEventPublisher;
    private final GoogleBooksClient googleBooksClient;
    private final GoogleBooksMapper googleBooksMapper;
    private final AuthorRepository authorRepository;

    public BookResponse create(CreateBookRequest request) {
        if (bookRepository.existsByIsbn(request.isbn()))
            throw new BookAlreadyExistsException(request.isbn());

        Book book = bookRepository.save(new Book(
            request.isbn(), request.title(), Long.valueOf(request.authorId()),
            request.description(), request.coverUrl(),
            request.publishedYear(), request.category()));

        Author author = resolveAuthor(book.getAuthorId());

        BookResponse response = toResponse(upsertReadModel(book));
        bookEventPublisher.publishBookCreated(
            book.getIsbn(), book.getTitle(), author.getName(), author.getId().toString());
        return response;
    }

    public BookResponse findByIsbn(String isbn) {
        return readModelRepository.findById(isbn)
            .map(this::toResponse)
            .orElseGet(() -> {
                // Auto-import desde Google Books si no existe en BD
                GoogleBooksResponse.Volume volume = googleBooksClient.findByIsbn(isbn);
                if (volume == null) throw new BookNotFoundException(isbn);

                Book book = googleBooksMapper.toBook(volume);
                BookResponse response = toResponse(upsertReadModel(book));

                Author author = resolveAuthor(book.getAuthorId());
                bookEventPublisher.publishBookCreated(
                    book.getIsbn(), book.getTitle(), author.getName(), author.getId().toString());
                return response;
            });
    }

    public List<BookResponse> search(String q) {
        return readModelRepository
            .findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase(q, q)
            .stream().map(this::toResponse).toList();
    }

    public List<BookResponse> searchExternal(String q) {
        List<BookResponse> dbResults = this.search(q);
        Set<String> dbIsbns = dbResults.stream()
                .map(BookResponse::isbn)
                .collect(Collectors.toSet());
        List<BookResponse> googleResults = googleBooksClient.search(q).stream()
                .filter(v -> googleBooksMapper.extractIsbn(v.volumeInfo()) != null)
                .filter(v -> !dbIsbns.contains(googleBooksMapper.extractIsbn(v.volumeInfo())))
                .map(googleBooksMapper::toReadModel)
                .map(this::toResponse)
                .toList();

        return Stream.concat(dbResults.stream(), googleResults.stream())
                .toList();
    }

    private BookReadModel upsertReadModel(Book book) {
        Author author = resolveAuthor(book.getAuthorId());
        BookReadModel readModel = new BookReadModel(
            book.getIsbn(), book.getTitle(), author.getName(),
            book.getAuthorId().toString(), book.getDescription(),
            book.getCoverUrl(), book.getPublishedYear(), book.getCategory());
        return readModelRepository.save(readModel);
    }

    private BookResponse toResponse(BookReadModel readModel) {
        String authorName = readModel.getAuthorId() != null
            ? resolveAuthor(Long.valueOf(readModel.getAuthorId())).getName()
            : readModel.getAuthorName();
        return new BookResponse(
            readModel.getIsbn(), readModel.getTitle(), authorName,
            readModel.getAuthorId(), readModel.getDescription(),
            readModel.getCoverUrl(), readModel.getPublishedYear(),
            readModel.getCategory(), readModel.getCreatedAt());
    }

    private Author resolveAuthor(Long authorId) {
        return authorRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found with ID: " + authorId));
    }
}
```

- `create()`: verifica unicidad por ISBN en Postgres, resuelve el `Author` por `authorId`, guarda, hace upsert del _read model_ con `authorName`+`authorId` y publica evento `BookCreatedEvent` con los 4 campos.
- `findByIsbn()`: primero intenta Mongo; si no existe, auto-importa desde Google Books (que a su vez crea el Author via `GoogleBooksMapper`).
- `search()`: búsqueda en Mongo por título o nombre de autor.
- `searchExternal()`: combina `search()` (BD local) con resultados de Google Books **sin persistencia**. Antes de mapear, filtra los volúmenes de Google sin ISBN (evita `isbn: null` en el cliente) y descarta los que ya salieron en los resultados de la BD (dedupe por ISBN).
- `upsertReadModel()`: resuelve el `Author` por FK para enriquecer el read model.
- `toResponse()`: **null-safe** — solo llama a `resolveAuthor()` cuando el read model tiene `authorId`; si es `null` (resultados efímeros de Google Books) usa directamente el `authorName` del read model. Ver error registrado en 7.4.

#### `AuthorService`

```java
@Service @Transactional
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final AuthorReadModelRepository readModelRepository;
    private final OpenLibraryClient openLibraryClient;
    private final OpenLibraryMapper mapper;

    public AuthorService(AuthorRepository authorRepository,
                         AuthorReadModelRepository readModelRepository,
                         OpenLibraryClient openLibraryClient,
                         OpenLibraryMapper mapper) {
        this.authorRepository = authorRepository;
        this.readModelRepository = readModelRepository;
        this.openLibraryClient = openLibraryClient;
        this.mapper = mapper;
    }

    public List<AuthorResponse> searchAuthors(String query) {
        List<AuthorReadModel> localAuthors = readModelRepository.findByNameContainingIgnoreCase(query);
        if (!localAuthors.isEmpty()) return localAuthors.stream().map(this::toResponse).toList();

        List<AuthorReadModel> openLibraryAuthors = openLibraryClient.searchAuthors(query)
                    .docs()
                    .stream()
                    .map(mapper::toReadModel)
                    .toList();

        return readModelRepository.saveAll(openLibraryAuthors).stream().map(this::toResponse).toList();
    }

    public AuthorResponse getAuthor(String openLibraryId) {
        AuthorReadModel cached = readModelRepository.findById(openLibraryId).orElse(null);
        if (cached != null && cached.getBio() != null && !cached.getBio().isBlank()) {
            return toResponse(cached);
        }

        AuthorDetailResponse olAuthor = openLibraryClient.getAuthor(openLibraryId);
        if (olAuthor == null) {
            return cached != null ? toResponse(cached) : null;
        }

        String photoUrl = cached != null && cached.getPhotoUrl() != null
                ? cached.getPhotoUrl()
                : mapper.coverUrl(openLibraryId);
        AuthorReadModel merged = new AuthorReadModel(openLibraryId, olAuthor.name(), olAuthor.bioText(),
                olAuthor.birthDate(), olAuthor.deathDate(), photoUrl,
                cached != null ? cached.getTopSubjects() : null,
                cached != null ? cached.getWorkCount() : null);
        readModelRepository.save(merged);
        return toResponse(merged);
    }

    public AuthorResponse getAuthorById(Long authorId) {
        Author author = authorRepository.findById(authorId).orElse(null);
        if (author == null) return null;

        String openLibraryId = author.getOpenLibraryId();
        if (openLibraryId == null || openLibraryId.isBlank()) {
            openLibraryId = resolveOpenLibraryIdByName(author.getName());
        }
        if (openLibraryId == null) {
            return new AuthorResponse(null, author.getName(), author.getBio(),
                    author.getBirthDate(), author.getDeathDate(), author.getPhotoUrl(),
                    null, author.getWorkCount());
        }
        return getAuthor(openLibraryId);
    }

    private String resolveOpenLibraryIdByName(String name) {
        List<AuthorReadModel> cached = readModelRepository.findByNameContainingIgnoreCase(name);
        String fromCache = cached.stream()
                .filter(rm -> rm.getName() != null && rm.getName().equalsIgnoreCase(name))
                .map(AuthorReadModel::getOpenLibraryId)
                .findFirst()
                .orElse(null);
        if (fromCache != null) return fromCache;

        List<AuthorReadModel> fetched = openLibraryClient.searchAuthors(name)
                .docs()
                .stream()
                .map(mapper::toReadModel)
                .toList();
        readModelRepository.saveAll(fetched);
        return fetched.stream()
                .filter(rm -> rm.getName() != null && rm.getName().equalsIgnoreCase(name))
                .map(AuthorReadModel::getOpenLibraryId)
                .findFirst()
                .orElse(null);
    }

    public WorksResponse getAuthorWorks(String openLibraryId) {
        return openLibraryClient.getWorks(openLibraryId);
    }

    public AuthorResponse createAuthor(String name) {
        Author author = authorRepository.save(new Author(name));
        return new AuthorResponse(null, author.getName(), null, null, null, null, null, null);
    }

    private AuthorResponse toResponse(AuthorReadModel rm) {
        return new AuthorResponse(
                rm.getOpenLibraryId(), rm.getName(), rm.getBio(),

                  rm.getBirthDate(), rm.getDeathDate(), rm.getPhotoUrl(),
                rm.getTopSubjects(), rm.getWorkCount());
    }
}
```

- `searchAuthors()`: busca en Mongo por nombre; si no hay resultados locales, consulta Open Library, mapea con `OpenLibraryMapper` y guarda en Mongo. Devuelve `AuthorResponse` (DTO) en vez de `AuthorReadModel`.
- `getAuthor()`: **lectura siempre desde Mongo**. Si el cache ya tiene bio, se sirve tal cual; si falta bio (los docs creados por búsqueda no la traen), consulta Open Library, **mergea** conservando subjects/workCount del cache y guarda solo en Mongo.
- `getAuthorById()`: resuelve un autor por su **PK interna** (`authors.id`, la que exponen los libros como `authorId`). Postgres solo sirve de lookup para obtener el `openLibraryId`; si el autor no lo tiene (autores del seeder), lo resuelve por nombre con `resolveOpenLibraryIdByName()`; la ficha completa sale siempre de Mongo vía `getAuthor()`.

- `resolveOpenLibraryIdByName()`: exact-match por nombre contra el cache Mongo; si no existe, busca en Open Library, cachea los resultados en Mongo y devuelve el id del match exacto. Devuelve `null` si Open Library no conoce al autor (la ficha se degrada a "solo nombre").
- `getAuthorWorks()`: proxy directo a Open Library `/authors/{id}/works.json`.
- `createAuthor()`: crea `Author` solo en Postgres (usado por `GoogleBooksMapper`). No escribe en Mongo — no tiene `openLibraryId` ni datos biográficos.
- `toResponse()`: convierte `AuthorReadModel` → `AuthorResponse` (DTO público). El controller nunca expone `AuthorReadModel` ni `Author` entity.

#### `BookController` y `AuthorController`

```java
@RestController @RequestMapping("/books")
public class BookController {
    private final BookService bookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Valid @RequestBody CreateBookRequest request) {
        if (!isAdmin(roles)) throw new ForbiddenException("ADMIN required");
        return bookService.create(request);
    }

    @GetMapping("/{isbn}")
    public BookResponse getBook(@PathVariable String isbn) {
        return bookService.findByIsbn(isbn);
    }

    @GetMapping("/search")
    public List<BookResponse> searchBooks(@RequestParam String q) {
        return bookService.search(q);
    }

    @GetMapping("/search/full")
    public List<BookResponse> searchBooksFull(@RequestParam String q) {
        return bookService.searchExternal(q);
    }

    private boolean isAdmin(String roles) {
        return roles != null && Arrays.stream(roles.split(","))
                .map(String::trim).anyMatch("ADMIN"::equals);
    }
}
```

```java
@RestController @RequestMapping("/authors")
public class AuthorController {
    private final AuthorService authorService;

    @GetMapping("/search")
    public List<AuthorResponse> searchAuthors(@RequestParam String q) {
        return authorService.searchAuthors(q);
    }

    @GetMapping("/id/{authorId}")
    public AuthorResponse getAuthorById(@PathVariable Long authorId) {
        return authorService.getAuthorById(authorId);
    }

    @GetMapping("/{openLibraryId}")
    public AuthorResponse getAuthor(@PathVariable String openLibraryId) {
        return authorService.getAuthor(openLibraryId);
    }

    @GetMapping("/{openLibraryId}/works")
    public WorksResponse getAuthorWorks(@PathVariable String openLibraryId) {
        return authorService.getAuthorWorks(openLibraryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse createAuthor(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestBody String name) {
        if (!isAdmin(roles)) throw new ForbiddenException("ADMIN required");
        return authorService.createAuthor(name);
    }

    private boolean isAdmin(String roles) {
        return roles != null && Arrays.stream(roles.split(","))
                .map(String::trim).anyMatch("ADMIN"::equals);
    }
}
```

| Método | Ruta                    | Auth                                 | Descripción                              |
| ------ | ----------------------- | ------------------------------------ | ---------------------------------------- |
| `POST` | `/books`                | `X-User-Roles` debe contener `ADMIN` | Crear libro (201)                        |
| `GET`  | `/books/{isbn}`         | Público                              | Buscar por ISBN (auto-importa, 200)      |
| `GET`  | `/books/search?q=`      | Público                              | Búsqueda solo en BD local (200)          |
| `GET`  | `/books/search/full?q=` | Público                              | Búsqueda BD + Google Books (200)         |
| `GET`  | `/authors/search?q=`    | Público                              | Buscar autores (BD local + Open Library) |
| `GET`  | `/authors/{olId}`       | Público                              | Detalle de autor (Open Library)          |
| `GET`  | `/authors/{olId}/works` | Público                              | Obras de un autor (Open Library)         |
| `POST` | `/authors`              | ADMIN                                | Crear autor manualmente (201)            |

> **Roles vía header de confianza**: el gateway reconstruye `X-User-Roles` a partir del claim `roles` del JWT validado (mismo patrón strip-then-assert que `X-User-Id`). El control de permisos downstream es `roles != null && Arrays.stream(roles.split(",")).map(String::trim).anyMatch("ADMIN"::equals)`.

#### DTOs

```java
public record CreateBookRequest(
    @NotBlank @Size(max = 20) String isbn,
    @NotBlank String title,
    @NotBlank String authorName,
    String authorId,
    String description,
    String coverUrl,
    Integer publishedYear,
    String category) {}

public record BookResponse(String isbn, String title, String authorName, String authorId,
    String description, String coverUrl, Integer publishedYear, String category,
    Instant createdAt) {}
```

#### `BookDataSeeder` (CommandLineRunner)

```java
@Component
@ConditionalOnProperty(name = "app.seed.books", havingValue = "true", matchIfMissing = true)
public class BookDataSeeder implements CommandLineRunner {
    private final BookRepository bookRepository;
    private final BookReadModelRepository readModelRepository;
    private final BookEventPublisher bookEventPublisher;
    private final AuthorRepository authorRepository;

    private record SeedBook(String isbn, String title, String authorName, String description,
                            String coverUrl, Integer publishedYear, String category) {}

    private static final List<SeedBook> books = List.of(
        // 8 libros de ejemplo con authorName
    );

    @Override @Transactional
    public void run(String... args) {
        if (bookRepository.count() > 0) return;
        for (SeedBook seed : books) {
            Author author = findOrCreateAuthor(seed.authorName());
            Book book = bookRepository.save(new Book(
                seed.isbn(), seed.title(), author.getId(), seed.description(),
                seed.coverUrl(), seed.publishedYear(), seed.category()));
            readModelRepository.save(new BookReadModel(
                seed.isbn(), seed.title(), author.getName(), author.getId().toString(),
                seed.description(), seed.coverUrl(), seed.publishedYear(), seed.category()));
            bookEventPublisher.publishBookCreated(
                book.getIsbn(), book.getTitle(), author.getName(), author.getId().toString());
        }
    }

    private Author findOrCreateAuthor(String name) {
        return authorRepository.findByNameContainingIgnoreCase(name)
            .stream().findFirst()
            .orElseGet(() -> { Author a = new Author(); a.setName(name); return authorRepository.save(a); });
    }
}
```

Si `bookRepository.count() == 0`, crea primero los `Author` y luego los libros, escribiendo **ambos lados** (Postgres + Mongo) y publicando eventos con los 4 campos (`isbn`, `title`, `authorName`, `authorId`).

> **Ordering del seeder**: en un entorno limpio, el seeder publica eventos que review-service consumirá si su cola ya está declarada. Si review-service arranca después, las colas durables en RabbitMQ mantienen los mensajes hasta que se conecte.

### 7.3 — Integración con APIs externas: Google Books + Open Library

En esta sub-fase el catálogo se enriquece con dos integraciones externas:

1. **Google Books API**: búsqueda de libros y auto-import por ISBN.
2. **Open Library API**: búsqueda de autores, datos biográficos y obras.

#### Configuración

```yaml
app:
  google-books:
    api-key: ${GOOGLE_BOOKS_API_KEY:}
    api-url: https://www.googleapis.com/books/v1/
  open-library:
    api-url: https://openlibrary.org
    user-agent: booksocial/1.0 (javierincio.dev@gmail.com)
```

```java
@ConfigurationProperties(prefix = "app.google-books")
public record GoogleBooksProperties(String apiKey, String apiUrl) {}

@ConfigurationProperties(prefix = "app.open-library")
public record OpenLibraryProperties(String apiUrl, String userAgent) {}
```

La API key de Google es **opcional**: sin ella se permiten 100 peticiones/día; con ella hasta 1000. Open Library no requiere API key pero exige un `User-Agent` identificativo. Se configuran en `BookServiceApplication`:

```java
@EnableConfigurationProperties({GoogleBooksProperties.class, OpenLibraryProperties.class})
public class BookServiceApplication { ... }
```

#### `GoogleBooksClient`

```java
@Component
public class GoogleBooksClient {
    private final RestClient restClient;
    private final GoogleBooksProperties props;

    // search(query): bucle de 3 intentos — si la petición falla (p.ej. 503),
    // espera 500ms y reintenta (hasta 2 reintentos) antes de devolver List.of()
    public List<GoogleBooksResponse.Volume> search(String query) { ... }
    public GoogleBooksResponse.Volume findByIsbn(String isbn) { ... }
}
```

Usa `RestClient` (nuevo en Spring Boot 3.2+) contra la API de Google. Los endpoints son:

| Método Google Books          | Descripción                  |
| ---------------------------- | ---------------------------- |
| `GET /volumes?q={query}`     | Búsqueda de libros por query |
| `GET /volumes?q=isbn:{isbn}` | Busca un libro por su isbn   |

**¿Por qué el retry solo en `search`?**
La Google Books API devuelve de forma intermitente `503 Service Unavailable (backendFailed)` — un fallo transitorio del lado de Google (se ha observado que ~60-70% de las peticiones fallan durante episodios de degradación, tanto con key como sin ella). Como `search` alimenta la búsqueda en vivo del catálogo (`GET /books/search/full`), un único intento haría que las búsquedas devolvieran "sin resultados" la mayoría de las veces. Con **3 intentos separados por 500ms**, la probabilidad de obtener resultados aumenta significativamente por búsqueda. En cambio, `findByIsbn` no lo necesita: se invoca una sola vez por ISBN (auto-import bajo demanda, no en tiempo real ante el usuario) y su fallo es recuperable con un simple reintento de la petición HTTP original.

Los errores se registran con `log.warn`/`log.error` y se degradan a lista vacía o `null`: un fallo de Google **nunca** debe romper el endpoint del catálogo local, que siempre funciona aunque Google esté caído.

#### `GoogleBooksMapper`

```java
@Component
public class GoogleBooksMapper {
    private final AuthorRepository authorRepository;

    public Book toBook(Volume volume) {
        // Persiste Author (findOrCreateAuthor) → devuelve Book con authorId
    }

    public BookReadModel toReadModel(Volume volume) {
        // SIN persistencia → devuelve BookReadModel con authorId = null
    }

    public Author findOrCreateAuthor(String name) { ... }

    public String extractIsbn(VolumeInfo info) { ... }         // ISBN_13 preferido, fallback ISBN_10
    public Integer extractYear(String date) { ... }            // Extrae año de publishedDate en formato: [YYYY-MM-DD], [YYYY-MM] o [YYYY]
    public String extractAuthorName(VolumeInfo info) { ... }   // info.authors().getFirst() o "Unknown"
    public String extractCategory(VolumeInfo info) { ... }     // info.categories().getFirst()
    public String extractCoverUrl(VolumeInfo info) { ... }     // info.imageLinks().thumbnail()
}
```

- `toBook()`: persiste el `Author` via `findOrCreateAuthor()` y devuelve `Book` con `authorId` válido. Se usa en `findByIsbn` (auto-import) y `create`.
- `toReadModel()`: construye `BookReadModel` directamente desde la respuesta de Google **sin tocar Postgres**. El `authorId` es `null` ya que es solo para visualización. Se usa en `searchExternal`.
- Métodos `extract*`: públicos y reutilizables, centralizan toda la lógica de mapeo de `VolumeInfo`.

#### `OpenLibraryClient`

```java
@Component
public class OpenLibraryClient {
    private final RestClient restClient;  // baseUrl: https://openlibrary.org

    public OpenLibraryResponse searchAuthors(String query) { ... }
    public AuthorDetailResponse getAuthor(String openLibraryId) { ... }
    public WorksResponse getWorks(String openLibraryId) { ... }
}
```

Usa `RestClient` contra la API de Open Library. Los endpoints son:

| Método Open Library                  | Descripción                           |
| ------------------------------------ | ------------------------------------- |
| `GET /search/authors.json?q={query}` | Búsqueda de autores por nombre        |
| `GET /authors/{id}.json`             | Detalle de autor (bio, fechas, fotos) |
| `GET /authors/{id}/works.json`       | Lista de obras del autor              |

La API no requiere key pero limita a ~3 req/s; se identifica con `User-Agent: booksocial/1.0 (booksocial@email.com)`.

**Ojo con el campo `bio`**: Open Library lo devuelve en dos formas distintas según el autor:

```json
"bio": "texto plano..."                              // algunos autores
"bio": {"type": "/type/text", "value": "texto..."}   // otros autores
```

Por eso `AuthorDetailResponse` declara `bio` como `Object` (no como `String`) y expone un normalizador:

```java
public record AuthorDetailResponse(
    @JsonProperty("key") String key,
    @JsonProperty("name") String name,
    @JsonProperty("bio") Object bio,     // String o Map {type,value} según el autor
    ...) {

    public String bioText() {
        if (bio == null) return null;
        if (bio instanceof String text) return text.isBlank() ? null : text;
        if (bio instanceof Map<?, ?> map) {
            Object value = map.get("value");
            return value == null || value.toString().isBlank() ? null : value.toString();
        }
        return null;
    }
}
```

#### `OpenLibraryMapper`

```java
@Component
public class OpenLibraryMapper {
    public AuthorReadModel toReadModel(AuthorDoc doc) {
        String openLibraryId = extractKey(doc.key());                // "/authors/OL123A" → "OL123A"
        String photoUrl = coverUrl(openLibraryId);                   // https://covers.openlibrary.org/a/olid/OL123A-L.jpg
        return new AuthorReadModel(openLibraryId, doc.name(), ...);  // La API no devuelve bio (null)
    }
}
```

#### Modelo de cache Open Library

Los autores de Open Library se cachean en **Mongo** (`authors`, read model con `_id` = `openLibraryId`), y ahí viven todas sus lecturas:

- Búsqueda (`searchAuthors`) → Open Library → guardar en Mongo; búsquedas siguientes → leer de Mongo.
- Ficha (`getAuthor`) → si el cache ya tiene bio, se sirve de Mongo; si falta bio, una única llamada a Open Library completa el documento (merge conservando subjects/workCount).
- Resolución por nombre (`resolveOpenLibraryIdByName`, usada por `getAuthorById`) → exact-match contra Mongo; si no está, Open Library + cache.

Postgres `authors` queda reservado a los **autores locales** creados por escritura: seeder, `POST /authors` (ADMIN) y auto-import de Google Books (`findOrCreateAuthor`). Estos autores no tienen `openLibraryId` ni datos biográficos hasta que alguien visita su ficha, momento en el que se enriquecen desde Open Library vía Mongo.

#### SecurityConfig del book-service

```java
.requestMatchers("/actuator/health").permitAll()
.requestMatchers(HttpMethod.GET, "/books/**").permitAll()
.requestMatchers(HttpMethod.GET, "/authors/**").permitAll()
.anyRequest().authenticated()
```

Los GETs son públicos (sin auth). Solo `POST /books` y `POST /authors` requieren **autenticación** y **rol ADMIN**.

### 7.4 — Errores encontrados en la Fase 3 (con solución directa)

1. **`NumberFormatException: Cannot parse null string` (HTTP 500) en `GET /books/search/full`**
   - Síntoma: cada vez que Google Books devolvía resultados, `searchExternal` terminaba en 500 y el frontend mostraba "búsqueda fallida"; cuando Google fallaba (503), la búsqueda devolvía vacío. Parecía un fallo aleatorio.
   - Causa: `GoogleBooksMapper.toReadModel()` construye read models efímeros con `authorId = null` (los resultados de Google no se persisten), pero `BookService.toResponse()` hacía `resolveAuthor(Long.valueOf(readModel.getAuthorId()))` sin condición → `Long.valueOf(null)` lanza `NumberFormatException`.
   - Solución: hacer `toResponse` **null-safe** — solo resolver el `Author` en BD cuando hay `authorId`; si es `null`, usar directamente el `authorName` que ya trae el read model.
   - Lección: cuando una misma clase (`toResponse`) sirve a datos persistentes y a datos efímeros de una API externa, todos los campos derivados de FK deben tratarse como opcionales.

2. **Búsquedas "sin resultados" aleatorias: `503 backendFailed` intermitente de Google Books**
   - Síntoma: `googleBooksClient.search()` fallaba ~60-70% de las veces con `HttpServerErrorException$ServiceUnavailable: 503 ... "reason": "backendFailed"`, tanto con API key como sin ella y desde host y contenedor por igual. Verificado con wget dentro del contenedor: mismo endpoint alternaba 200/503 en segundos.
   - Causa: degradación transitoria del lado de Google (no de la app). El cliente original tragaba la excepción y devolvía lista vacía → UX de "no encuentra nada".
   - Solución: bucle de **retry con 2 reintentos tras 500ms** en `search()` (ver sección 7.3 para el porqué). Adicionalmente se filtraron los volúmenes sin ISBN y se deduplicó contra los resultados locales para no mostrar basura al usuario.

3. **Doble barra potencial en la URL de Google Books**
   - Causa: `api-url: https://www.googleapis.com/books/v1/` (barra final) combinado con `.path("/volumes")` (barra inicial) puede producir `/books/v1//volumes`.
   - Solución: normalizar la base sin barra final en `application.yaml`.

4. **Fichas de autor siempre incompletas: Jackson no parsea la bio polimórfica de Open Library**
   - Síntoma: `GET /authors/{id}` devolvía fichas sin bio aunque el autor la tuviera en Open Library; en el log aparecía `Error fetching author details` de forma silenciosa.
   - Causa: Open Library devuelve `"bio"` a veces como string y a veces como objeto `{"type": "/type/text", "value": "..."}`. El record declaraba `String bio`, Jackson lanzaba `MismatchedInputException`, el `catch` del client lo tragaba y devolvía `null`. Además, el cache Mongo creado por búsqueda nunca tiene bio, así que `getAuthor` servía el cache vacío para siempre.
   - Solución doble: (1) `bio` tipado como `Object` + helper `bioText()` que normaliza ambas formas (ver 7.3); (2) `getAuthor()` ahora detecta cache sin bio, consulta Open Library y **mergea** conservando subjects/workCount.
   - Lección: ante APIs externas, los campos pueden variar de tipo entre documentos; un `catch(Exception)` que devuelve `null` convierte un error de parseo en datos silenciosamente ausentes.

5. **F5 en una página del frontend devolvía `401 unauthorized`: colisión de prefijos en el proxy de desarrollo**
   - Síntoma: refrescar `http://localhost:4200/author/5` mostraba `{"error":"unauthorized","message":"Authentication required"}` en vez de la página.
   - Causa: el matching del proxy es por prefijo. La clave `/auth` matcheaba también `/author/...`, así que el dev server enviaba la navegación del navegador al gateway (:8080), donde esa ruta HTTP no existe y su SecurityConfig respondía 401. Las rutas `/book/:isbn` sobrevivían al F5 porque `/book` no es prefijo de ninguna clave.
   - Solución: claves del proxy como regex con frontera (`"^/auth(/|$)"`, ver sección 3.1). Verificado: navegación `Accept: text/html` sirve el `index.html` de la SPA; las llamadas API siguen llegando al gateway.
   - Lección: cuando las rutas de SPA y los prefijos de API comparten servidor de desarrollo, ancla los prefijos del proxy o cualquier ruta futura con ese prefijo romperá el refresh.

### Decisiones de diseño de la Fase 3 (resumen)

- **Author como entidad independiente**: `Author` vive en Postgres (`authors`) y Mongo (`authors`), con `openLibraryId` como clave de cache. Se crea bajo demanda (auto-import mínimo desde Google Books (`name` + `createdAt`), búsqueda en Open Library, o manual via `POST /authors`).
- **Book con FK a Author**: `Book.authorId` (Long) es la FK lógica a `authors.id`. No se usa `@ManyToOne` porque la relación es ligera y se resuelve por código.
- **Dual APIs externas**: Google Books para libros (búsqueda + auto-import por ISBN), Open Library para autores (búsqueda + datos biográficos + obras). Cada una cubre un dominio distinto.
- **Cache de autores en Open Library en Mongo**: la búsqueda y la ficha cachean/leen siempre en Mongo (CQRS puro); Postgres queda para autores locales creados por escritura. Los autores sin `openLibraryId` se enriquecen bajo demanda resolviendo su id por nombre. Rate limit ~3 req/s identificado con `User-Agent`.
- **Búsqueda derivada en Mongo**: una única consulta derivada sobre título/autor es suficiente para esta fase; si hiciera falta ranking o tolerancia a errores, se migraría a un índice `text` de Mongo.
- **Réplica del esqueleto**: el coste de crear un microservicio nuevo bajó respecto a la Fase 2: copiar la seguridad parse-only y el patrón de compose ya está estandarizado.

---

## Bloque 8 — review-service (reseñas + primer evento cruzado)

La Fase 4 introduce dos novedades respecto a las anteriores: (1) un **evento cruzado** entre servicios (`book-service` publica → `review-service` consume) y (2) el primer modelo de lectura con **datos desnormalizados por eventos** en vez de dual-write directo.

**Ficha del servicio**

|                 |                                                                                                                                                |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8084`                                                                                                                                         |
| Persistencia    | PostgreSQL (`reviews`, comandos) + MongoDB (lecturas: `reviews`, `book_refs` — catálogo local desnormalizado)                                  |
| Responsabilidad | Reseñas con rating 1-5 y comentario; rating medio + nº de reseñas agregados por libro                                                          |
| Endpoints clave | `GET /reviews/books/{isbn}`, `GET /reviews/books/{isbn}/summary`, `GET /reviews/me`, `GET /reviews/users/{userId}`, `POST/PUT /reviews/{isbn}` |
| Mensajería      | Consume `BookCreatedEvent` para mantener su catálogo local (`book_refs`) sin llamar a book-service                                             |

### 8.1 — Esqueleto del review-service

#### Creación y estructura

Mismo procedimiento que 7.1, con estas variantes:

- Puerto **`8084`**. Dependencias: los mismos 6 starters + `spring-boot-starter-amqp` + `spring-rabbit-test` (test) + driver `postgresql`.
- Gateway: ruta `Path=/reviews/**` → `${REVIEW_SERVICE_URI:http://localhost:8084}`.
- Compose: `review-service` con `depends_on` postgres + mongodb + **rabbitmq** (los tres `service_healthy`), `SPRING_RABBITMQ_HOST: rabbitmq`.

```
review-service/
├── .env                              # APP_JWT_SECRET
├── Dockerfile
├── pom.xml
└── src/main/java/com/booksocial/review/
    ├── config/
    │   ├── SecurityConfig.java       # parse-only JWT
    │   └── RabbitConfig.java         # exchange + cola + binding del consumer
    ├── domain/
    │   ├── Review.java               # JPA entity (Postgres)
    │   ├── ReviewNotFoundException.java
    │   ├── ReviewAlreadyExistsException.java
    │   └── BookNotInCatalogException.java
    ├── readmodel/
    │   ├── ReviewReadModel.java      # Mongo document (_id = "isbn:userId")
    │   ├── ReviewReadModelRepository.java
    │   ├── ReviewStatsReadModel.java # Mongo document (_id = isbn)
    │   ├── ReviewStatsReadModelRepository.java
    │   ├── BookRefReadModel.java     # Mongo document (_id = isbn)
    │   └── BookRefReadModelRepository.java
    ├── repository/
    │   └── ReviewRepository.java     # JPA
    ├── security/
    │   ├── JwtService.java
    │   ├── JwtAuthFilter.java
    │   └── RestAuthenticationEntryPoint.java
    ├── service/
    │   └── ReviewService.java
    ├── events/
    │   ├── BookCreatedEvent.java     # record (copia del publicador)
    │   └── BookCreatedEventConsumer.java  # @RabbitListener
    └── web/
        ├── ReviewController.java
        ├── GlobalExceptionHandler.java
        └── dto/
            ├── CreateReviewRequest.java
            ├── UpdateReviewRequest.java
            ├── ReviewResponse.java
            └── ReviewSummaryResponse.java
```

> **Diferencia clave con book-service**: aquí el servicio **declara su propia cola y binding** en `RabbitConfig` (porque es el consumidor). book-service solo declara el exchange (porque es el productor). Cada consumidor es dueño de su cola.

### 8.2 — Evento cruzado `BookCreatedEvent`

Este es el patrón clave: el catálogo publica un evento y otro servicio lo consume para mantener su propio catálogo local desnormalizado.

#### Configuración RabbitMQ en review-service

```java
@Configuration
public class RabbitConfig {
    public static final String EXCHANGE     = "booksocial.events";
    public static final String REVIEW_QUEUE = "review-service.books.created";
    public static final String REVIEW_KEY   = "book.created";

    @Bean TopicExchange eventsExchange() { ... }          // durable
    @Bean Queue reviewQueue()            { ... }          // durable
    @Bean Binding reviewBinding()        { ... }          // queue → exchange, routing key "book.created"
    @Bean MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("com.booksocial.review.events");
    }
}
```

Topología:

```
[booksocial.events] (Topic Exchange)
    └── routing key "book.created" → [review-service.books.created]
```

#### Evento y consumidor

```java
// Copia del record (mismo paquete que el publicador, pero en review-service)
public record BookCreatedEvent(String bookIsbn, String title, String authorName,
                               String authorId, Instant occurredAt) {}
```

```java
@Component
public class BookCreatedEventConsumer {
    private final BookRefReadModelRepository bookRefRepository;

    @RabbitListener(queues = RabbitConfig.REVIEW_QUEUE)
    public void onBookCreated(BookCreatedEvent event) {
        bookRefRepository.save(new BookRefReadModel(
            event.bookIsbn(), event.title(), event.authorName(), event.authorId()));
        log.info("Processed BookCreatedEvent: {} - {}", event.bookIsbn(), event.title());
    }
}
```

Al recibir `BookCreatedEvent`, hace upsert de `BookRefReadModel` en la colección `book_refs` de Mongo con los 4 campos (`isbn`, `title`, `authorName`, `authorId`). Este documento se usa para verificar que un libro existe antes de permitir una reseña.

> **¿Por qué no declarar la cola del consumer en el publicador?** Si book-service declarara `review-service.books.created`, estaría acoplado al nombre de la cola del otro servicio. Cada consumidor declara su propia cola y binding: así, si mañana se añade otro consumidor (p.ej. `notification-service`), no hay que modificar book-service.

> **Record duplicado**: el `BookCreatedEvent` existe tanto en `com.booksocial.book.events` (publicador) como en `com.booksocial.review.events` (consumidor). Es necesaria la duplicación porque `JacksonJsonMessageConverter` deserializa contra los tipos del paquete confiable del converter (`trustedPackages`). Cada servicio escanea su propio paquete.

#### Read model: `BookRefReadModel`

```java
@Document(collection = "book_refs")
public class BookRefReadModel {
    @Id
    private String isbn;
    private String title;
    private String authorName;
    private String authorId;
}
```

Colección `book_refs` en Mongo. Solo los campos necesarios para validar reseñas y mostrar información básica del libro (sin `description`, `coverUrl`, etc.). `authorName` y `authorId` se desnormalizan del evento para que las respuestas incluyan el nombre del autor sin joins.

### 8.3 — Reseñas CQRS con stats

#### Command side — `domain/Review` (Postgres)

```java
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = "book_isbn, user_id"))
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_isbn", nullable = false, length = 20)
    private String bookIsbn;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int rating;        // 1-5

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;
}
```

Unique constraint sobre `(book_isbn, user_id)`: un usuario solo puede dejar una reseña por libro.

#### Query side — Read models en Mongo

**ReviewReadModel** (colección `reviews`):

```java
@Document(collection = "reviews")
public class ReviewReadModel {
    @Id
    private String id;          // "isbn:userId" (natural composite key)
    private String bookIsbn;
    private Long userId;
    private int rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**ReviewStatsReadModel** (colección `review_stats`):

```java
@Document(collection = "review_stats")
public class ReviewStatsReadModel {
    @Id
    private String bookIsbn;
    private long ratingCount;
    private double averageRating;
}
```

Este es un **modelo agregado**: se recalcula en cada operación de escritura (no se incrementa/decrementa), lo que lo hace idempotente y simple de razonar.

#### Repositorios

```java
// PostgreSQL
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookIsbnAndUserId(String bookIsbn, Long userId);
}

// MongoDB
public interface ReviewReadModelRepository extends MongoRepository<ReviewReadModel, String> {
    List<ReviewReadModel> findByBookIsbnOrderByCreatedAtDesc(String bookIsbn);
    boolean existsByBookIsbnAndUserId(String bookIsbn, Long userId);
}

public interface ReviewStatsReadModelRepository extends MongoRepository<ReviewStatsReadModel, String> {
    Optional<ReviewStatsReadModel> findByBookIsbn(String bookIsbn);
}

public interface BookRefReadModelRepository extends MongoRepository<BookRefReadModel, String> {
    // CRUD estándar + existsById (para verificar catálogo local)
}
```

#### `ReviewService`

```java
@Service @Transactional
public class ReviewService {
    private final BookRefReadModelRepository bookRefRepo;
    private final ReviewRepository reviewRepo;
    private final ReviewReadModelRepository readModelRepo;
    private final ReviewStatsReadModelRepository statsRepo;

    public ReviewResponse create(Long userId, String bookIsbn, CreateReviewRequest req) {
        if (!bookRefRepo.existsById(bookIsbn))
            throw new BookNotInCatalogException(bookIsbn);   // 422
        if (readModelRepo.existsByBookIsbnAndUserId(bookIsbn, userId))
            throw new ReviewAlreadyExistsException(bookIsbn, userId);  // 409

        Review review = reviewRepo.save(new Review(bookIsbn, userId, req.rating(), req.comment()));
        readModelRepo.save(new ReviewReadModel(review));
        syncStats(bookIsbn);
        return toResponse(review);
    }

    public ReviewResponse update(Long userId, String bookIsbn, UpdateReviewRequest req) {
        Review review = reviewRepo.findByBookIsbnAndUserId(bookIsbn, userId)
                .orElseThrow(() -> new ReviewNotFoundException(bookIsbn, userId));
        boolean changed = false;
        if (req.rating() != null && req.rating() != review.getRating()) {
            review.setRating(req.rating());
            changed = true;
        }
        if (req.comment() != null && !req.comment().equals(review.getComment())) {
            review.setComment(req.comment());
            changed = true;
        }
        if (!changed) return toResponse(review);
        review.setUpdatedAt(Instant.now());
        reviewRepo.save(review);
        readModelRepo.save(new ReviewReadModel(review));
        syncStats(bookIsbn);
        return toResponse(review);
    }

    public List<ReviewResponse> listByBook(String bookIsbn) {
        return readModelRepo.findByBookIsbnOrderByCreatedAtDesc(bookIsbn)
                .stream().map(this::toResponse).toList();
    }

    public ReviewSummaryResponse summary(String bookIsbn) {
        ReviewStatsReadModel stats = statsRepo.findByBookIsbn(bookIsbn).orElse(null);
        return new ReviewSummaryResponse(bookIsbn,
            stats != null ? stats.getRatingCount() : 0,
            stats != null ? stats.getAverageRating() : 0.0);
    }

    public List<ReviewResponse> listByUser(Long userId) {
        return readModelRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    private void syncStats(String bookIsbn) {
        List<ReviewReadModel> reviews = readModelRepo.findByBookIsbnOrderByCreatedAtDesc(bookIsbn);
        double avg = reviews.stream().mapToInt(ReviewReadModel::getRating).average().orElse(0.0);
        statsRepo.save(new ReviewStatsReadModel(bookIsbn, reviews.size(), avg));
    }
}
```

- **Control de catálogo local**: `create` verifica `bookRefRepo.existsById(isbn)` → 422 (`BookNotInCatalogException`) si el libro no existe en el catálogo local. El frontend debe llamar `GET /books/{isbn}` antes de permitir una review — esto auto-importa el libro y publica el evento que llena `book_refs` en review-service.
- **`syncStats`**: recalcula media y conteo con `mapToInt(...).average()` sobre las reseñas de Mongo. Idempotente ante re-escrituras.
- `update`: solo actualiza si los valores recibidos difieren de los actuales (patch parcial). Si no hay cambios reales, no toca `updatedAt` ni hace `save`.

#### `ReviewController`

```java
@RestController @RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/{bookIsbn}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(userId, bookIsbn, request);
    }

    @PutMapping("/{bookIsbn}")
    public ReviewResponse update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String bookIsbn,
            @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(userId, bookIsbn, request);
    }

    @GetMapping("/books/{bookIsbn}")
    public List<ReviewResponse> listByBook(@PathVariable String bookIsbn) {
        return reviewService.listByBook(bookIsbn);
    }

    @GetMapping("/books/{bookIsbn}/summary")
    public ReviewSummaryResponse summary(@PathVariable String bookIsbn) {
        return reviewService.summary(bookIsbn);
    }

    @GetMapping("/me")
    public List<ReviewResponse> myReviews(@RequestHeader("X-User-Id") Long userId) {
        return reviewService.listByUser(userId);
    }

    @GetMapping("/users/{userId}")
    public List<ReviewResponse> userReviews(@PathVariable Long userId) {
        return reviewService.listByUser(userId);
    }
}
```

| Método | Ruta                                | Auth  | Descripción                        |
| ------ | ----------------------------------- | ----- | ---------------------------------- |
| `POST` | `/reviews/{bookIsbn}`               | Token | Crear reseña (201 / 409 / 422)     |
| `PUT`  | `/reviews/{bookIsbn}`               | Token | Actualizar reseña (200)            |
| `GET`  | `/reviews/books/{bookIsbn}`         | Token | Lista de reseñas por libro (Mongo) |
| `GET`  | `/reviews/books/{bookIsbn}/summary` | Token | Rating medio + count (Mongo)       |
| `GET`  | `/reviews/me`                       | Token | Reseñas del usuario actual         |
| `GET`  | `/reviews/users/{userId}`           | Token | Reseñas de un usuario específico   |

#### DTOs

```java
public record CreateReviewRequest(
    @Min(1) @Max(5) int rating,
    String comment) {}

public record UpdateReviewRequest(
    @Min(1) @Max(5) Integer rating,     // nullable para patch
    String comment) {}

public record ReviewResponse(Long id, String bookIsbn, Long userId, int rating,
    String comment, Instant createdAt, Instant updatedAt) {}

public record ReviewSummaryResponse(String bookIsbn, long ratingCount, double averageRating) {}
```

#### Excepciones

| Excepción                      | HTTP | Error           |
| ------------------------------ | ---- | --------------- |
| `ReviewNotFoundException`      | 404  | `not_found`     |
| `ReviewAlreadyExistsException` | 409  | `conflict`      |
| `BookNotInCatalogException`    | 422  | `unprocessable` |

### Decisiones de diseño de la Fase 4 (resumen)

- **Primer evento cruzado**: book-service publica `BookCreatedEvent` (con `authorName`+`authorId`); review-service consume y mantiene un catálogo local (`book_refs`). Esto desacopla reseñas de catálogo: review-service nunca llama a book-service por REST.
- **Reseñas en dual-write**: por ahora el comando escribe Postgres + Mongo (como el perfil en 2.2). Los eventos de reseña (para notificaciones, feed de actividad, etc.) se añadirán en una fase futura.
- **Modelo agregado de stats**: `review_stats` se recalcula en cada operación en vez de incrementar/decrementar, lo que lo hace idempotente y simple de razonar.
- **Ordering del seeder**: en un entorno limpio, el seeder publica eventos que review-service consumirá si su cola ya está declarada. Si review-service arranca después, las colas durables en RabbitMQ mantienen los mensajes hasta que se conecte.

---

## Bloque 9 — shelf-service (estantería personal del usuario)

La Fase 4 continúa con el patrón de eventos cruzados: `shelf-service` consume `BookCreatedEvent` de la misma forma que review-service, pero su dominio es distinto — una **estantería personal** donde cada usuario clasifica libros en tres estados: _wants to read_, _reading_ y _read_. Es el primer servicio que usa `X-User-Id` como identificador principal (en vez de `X-User-Email`).

**Ficha del servicio**

|                 |                                                                                                                     |
| --------------- | ------------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8085`                                                                                                              |
| Persistencia    | PostgreSQL (`shelves`, comandos) + MongoDB (lecturas: `shelves`, `book_refs`)                                       |
| Responsabilidad | Estantería personal por usuario: alta/cambio/eliminación de libros con estado (`WANTS_TO_READ`, `READING`, `READ`)  |
| Endpoints clave | `GET /shelves`, `POST /shelves`, `PUT/DELETE /shelves/{isbn}`, `GET /shelves/{isbn}`, `GET /shelves/users/{userId}` |
| Mensajería      | Consume `BookCreatedEvent` (mismo patrón que review-service)                                                        |

### 9.1 — Esqueleto del shelf-service

#### Creación y estructura

Mismo procedimiento que 7.1 y 8.1, con estas variantes:

- Puerto **`8085`**. Dependencias: los mismos 6 starters + `spring-boot-starter-amqp` + driver `postgresql`.
- Gateway: ruta `Path=/shelves/**` → `${SHELF_SERVICE_URI:http://localhost:8085}`.
- Compose: `shelf-service` con `depends_on` postgres + mongodb + **rabbitmq** (los tres `service_healthy`).

```
shelf-service/
├── .env                              # APP_JWT_SECRET
├── Dockerfile
├── pom.xml
└── src/main/java/com/booksocial/shelf/
    ├── ShelfServiceApplication.java
    ├── config/
    │   ├── SecurityConfig.java       # parse-only JWT (Apéndice A)
    │   └── RabbitConfig.java         # exchange + cola + binding del consumer
    ├── domain/
    │   ├── Shelf.java               # JPA entity (Postgres)
    │   ├── ShelfStatus.java         # enum: WANTS_TO_READ, READING, READ
    │   ├── ShelfNotFoundException.java
    │   ├── ShelfAlreadyExistsException.java
    │   └── BookNotInCatalogException.java
    ├── readmodel/
    │   ├── ShelfReadModel.java       # Mongo document (_id = "userId:isbn")
    │   ├── ShelfReadModelRepository.java
    │   ├── BookRefReadModel.java     # Mongo document (_id = isbn)
    │   └── BookRefReadModelRepository.java
    ├── repository/
    │   └── ShelfRepository.java      # JPA
    ├── security/
    │   ├── JwtService.java
    │   ├── JwtAuthFilter.java
    │   └── RestAuthenticationEntryPoint.java
    ├── service/
    │   └── ShelfService.java
    ├── events/
    │   ├── BookCreatedEvent.java     # record (copia del publicador)
    │   └── BookCreatedEventConsumer.java  # @RabbitListener
    └── web/
        ├── ShelfController.java
        ├── GlobalExceptionController.java
        └── dto/
            ├── CreateShelfRequest.java
            ├── UpdateShelfRequest.java
            └── ShelfResponse.java
```

> **Seguridad**: las 4 clases de seguridad se copian tal cual del [Apéndice A](#apéndice-a--plantilla-de-seguridad-reutilizable). No hay diferencias.

### 9.2 — Modelo de dominio: `Shelf`

```java
@Entity
@Table(name = "shelves",
       uniqueConstraints = @UniqueConstraint(columnNames = "book_isbn, user_id"))
public class Shelf {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_isbn", nullable = false, length = 20)
    private String bookIsbn;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ShelfStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;
}
```

```java
public enum ShelfStatus {
    WANTS_TO_READ,
    READING,
    READ
}
```

**Restricción de unicidad**: un usuario solo puede tener **una entrada por ISBN**. Si quiere cambiar el estado, usa `PUT` en vez de `POST`.

### 9.3 — Read models (MongoDB)

#### `ShelfReadModel` — proyección desnormalizada del estante

```java
@Document(collection = "shelves")
public class ShelfReadModel {
    @Id private String id;        // "userId:isbn"
    private Long userId;
    private String bookIsbn;
    private String title;          // ← desnormalizado de BookRefReadModel
    private String authorName;     // ← desnormalizado de BookRefReadModel
    private String authorId;       // ← desnormalizado de BookRefReadModel
    private ShelfStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public ShelfReadModel(Shelf shelf, BookRefReadModel book) {
        this.id = shelf.getUserId() + ":" + shelf.getBookIsbn();
        this.userId = shelf.getUserId();
        this.bookIsbn = shelf.getBookIsbn();
        this.title = book.getTitle();
        this.authorName = book.getAuthorName();
        this.authorId = book.getAuthorId();
        this.status = shelf.getStatus();
        this.createdAt = shelf.getCreatedAt();
        this.updatedAt = shelf.getUpdatedAt();
    }
}
```

**`_id`**: compuesto `userId:isbn` — idempotente y único por usuario+libro.

#### `BookRefReadModel` — catálogo local de libros (copia exacta del de review-service)

```java
@Document(collection = "book_refs")
public class BookRefReadModel {
    @Id private String isbn;
    private String title;
    private String authorName;
    private String authorId;
}
```

> **Patrón duplicado intencionalmente**: `shelf-service` y `review-service` mantienen cada uno su propia copia de `book_refs` en MongoDB. No se comparten bases de datos entre servicios. Esto mantiene el desacoplamiento: si review-service elimina su catálogo, shelf-service no se ve afectado.

#### Repositorio del read model

```java
public interface ShelfReadModelRepository extends MongoRepository<ShelfReadModel, String> {
    List<ShelfReadModel> findAllByUserId(Long userId);
    List<ShelfReadModel> findAllByBookIsbn(String bookIsbn);
}
```

`findAllByBookIsbn` se usa en `GET /shelves/{isbn}` para listar todos los usuarios que tienen un libro en su estantería.

### 9.4 — Consumidor del evento `BookCreatedEvent`

Mismo patrón que en review-service (sección 8.2):

```java
@Component
public class BookCreatedEventConsumer {
    private final BookRefReadModelRepository readModelRepository;

    @RabbitListener(queues = RabbitConfig.SHELF_QUEUE)
    public void onBookCreated(BookCreatedEvent event) {
        readModelRepository.save(
            new BookRefReadModel(event.bookIsbn(), event.title(),
                event.authorName(), event.authorId()));
    }
}
```

```java
@Configuration
public class RabbitConfig {
    public static final String EXCHANGE     = "booksocial.events";
    public static final String SHELF_QUEUE  = "shelf-service.books.created";
    public static final String SHELF_KEY    = "book.created";
    // ... beans: exchange, queue, binding, jsonMessageConverter
}
```

**La cola se llama `shelf-service.books.created`**: cada consumidor declara su propia cola duradera. El routing key es el mismo `book.created`.

### 9.5 — Servicio: operaciones CRUD

```java
@Service
@Transactional
public class ShelfService {

    private final BookRefReadModelRepository bookRefRepository;
    private final ShelfRepository shelfRepository;
    private final ShelfReadModelRepository readModelRepository;

    public ShelfResponse create(CreateShelfRequest req, Long userId) {
        BookRefReadModel bookRef = bookRefRepository.findById(req.bookIsbn())
                .orElseThrow(() -> new BookNotInCatalogException(req.bookIsbn()));

        if (shelfRepository.existsByUserIdAndBookIsbn(userId, req.bookIsbn()))
            throw new ShelfAlreadyExistsException(req.bookIsbn(), userId);

        Shelf shelf = new Shelf(req.bookIsbn(), userId, req.status());
        shelfRepository.save(shelf);

        ShelfReadModel readModel = new ShelfReadModel(shelf, bookRef);
        readModelRepository.save(readModel);

        return toResponse(readModel);
    }

    public ShelfResponse updateStatus(String isbn, Long userId, UpdateShelfRequest req) {
        BookRefReadModel bookRef = bookRefRepository.findById(isbn)
                .orElseThrow(() -> new BookNotInCatalogException(isbn));

        Shelf shelf = shelfRepository.findByUserIdAndBookIsbn(userId, isbn)
                .orElseThrow(() -> new ShelfNotFoundException(isbn, userId));

        if (shelf.getStatus() == req.status()) {
            return toResponse(readModelRepository.save(new ShelfReadModel(shelf, bookRef)));
        }

        shelf.setStatus(req.status());
        shelf.setUpdatedAt(Instant.now());
        shelfRepository.save(shelf);

        ShelfReadModel readModel = readModelRepository.save(new ShelfReadModel(shelf, bookRef));
        return toResponse(readModel);
    }

    public void delete(String isbn, Long userId) {
        Shelf shelf = shelfRepository.findByUserIdAndBookIsbn(userId, isbn)
                .orElseThrow(() -> new ShelfNotFoundException(isbn, userId));

        shelfRepository.delete(shelf);
        readModelRepository.deleteById(userId + ":" + isbn);
    }

    public List<ShelfResponse> listByUser(Long userId) {
        return readModelRepository.findAllByUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    public List<ShelfResponse> listByBookIsbn(String bookIsbn) {
        return readModelRepository.findAllByBookIsbn(bookIsbn)
                .stream().map(this::toResponse).toList();
    }
}
```

**Flujo dual-write** (igual que user-service en 6.2):

1. Validar que el libro existe en `book_refs` (catálogo local vía RabbitMQ).
2. Verificar unicidad (solo en `create`).
3. **`updateStatus`**: solo guarda si el status recibido difiere del actual. Si no cambia, retorna sin tocar `updatedAt`.
4. Escribir en PostgreSQL (`shelves`).
5. Escribir en MongoDB (`shelves` read model).

### 9.6 — Controlador

```java
@RestController
@RequestMapping("/shelves")
public class ShelfController {

    private final ShelfService shelfService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfResponse create(
            @Valid @RequestBody CreateShelfRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return shelfService.create(request, userId);
    }

    @PutMapping("/{isbn}")
    public ShelfResponse updateStatus(
            @PathVariable String isbn,
            @Valid @RequestBody UpdateShelfRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return shelfService.updateStatus(isbn, userId, request);
    }

    @DeleteMapping("/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String isbn,
            @RequestHeader("X-User-Id") Long userId) {
        shelfService.delete(isbn, userId);
    }

    @GetMapping
    public List<ShelfResponse> list(
            @RequestHeader("X-User-Id") Long userId) {
        return shelfService.listByUser(userId);
    }

    @GetMapping("/users/{userId}")
    public List<ShelfResponse> listByUserPublic(@PathVariable Long userId) {
        return shelfService.listByUser(userId);
    }

    @GetMapping("/{isbn}")
    public List<ShelfResponse> listByIsbn(@PathVariable String isbn) {
        return shelfService.listByBookIsbn(isbn);
    }
}
```

#### Endpoints

| Método   | Ruta                      | Auth    | Descripción                           | Headers                      |
| -------- | ------------------------- | ------- | ------------------------------------- | ---------------------------- |
| `POST`   | `/shelves`                | Token   | Añadir libro al estante               | `X-User-Id`, `Authorization` |
| `PUT`    | `/shelves/{isbn}`         | Token   | Cambiar estado (wants/reading/read)   | `X-User-Id`, `Authorization` |
| `DELETE` | `/shelves/{isbn}`         | Token   | Eliminar del estante                  | `X-User-Id`, `Authorization` |
| `GET`    | `/shelves`                | Token   | Listar estante del usuario            | `X-User-Id`, `Authorization` |
| `GET`    | `/shelves/users/{userId}` | Público | Estanterías de un usuario             | —                            |
| `GET`    | `/shelves/{isbn}`         | Público | Usuarios con este libro en estantería | —                            |

> **GETs públicos**: `GET /shelves/users/{userId}` y `GET /shelves/{isbn}` no requieren autenticación. El gateway y el SecurityConfig del shelf-service permiten `GET /shelves/**` sin token. Esto permite al frontend mostrar estanterías de usuarios y popularity de libros sin login.

> **Identidad del usuario**: a diferencia de los demás servicios que usan `X-User-Email`, shelf-service usa **`X-User-Id`** (el ID numérico del usuario). Esto simplifica las queries en PostgreSQL y MongoDB.

### 9.7 — DTOs

```java
public record CreateShelfRequest(
    @NotBlank String bookIsbn,
    ShelfStatus status
) {
    public CreateShelfRequest {
        if (status == null) status = ShelfStatus.WANTS_TO_READ;
    }
}

public record UpdateShelfRequest(@NotNull ShelfStatus status) {}

public record ShelfResponse(
    Long id, String bookIsbn, String title, String authorName, String authorId,
    ShelfStatus status, Instant createdAt, Instant updatedAt
) {}
```

**`CreateShelfRequest`**: si no se especifica `status`, por defecto es `WANTS_TO_READ`.

### Excepciones

| Excepción                     | HTTP | Error           |
| ----------------------------- | ---- | --------------- |
| `ShelfNotFoundException`      | 404  | `not_found`     |
| `ShelfAlreadyExistsException` | 409  | `conflict`      |
| `BookNotInCatalogException`   | 422  | `unprocessable` |

### Decisiones de diseño de shelf-service

- **Doble consumidor del mismo evento**: tanto review-service como shelf-service escuchan `book.created` (con `authorName`+`authorId`). Cada uno mantiene su propia cola (`review-service.books.created` y `shelf-service.books.created`) y su propia copia de `book_refs`. Esto es desacoplamiento por diseño: no hay bases de datos compartidas entre servicios.
- **`X-User-Id` en vez de `X-User-Email`**: el gateway extrae ambos headers del JWT. shelf-service usa el ID numérico porque es más eficiente como clave de búsqueda en MongoDB (`findAllByUserId`) y PostgreSQL (`WHERE user_id = ?`).
- **Unicidad por constraint, no por código**: la restricción `UNIQUE(book_isbn, user_id)` en PostgreSQL garantiza que un usuario no pueda añadir el mismo libro dos veces, aunque el servicio también valida antes de insertar para devolver un error claro (`409`).
- **Dual-write síncrono**: el servicio escribe Postgres y Mongo en la misma transacción. Es el mismo patrón que user-service (perfil) y review-service (reseñas). La consistencia eventual entre servicios se gestiona a nivel de eventos.
- **GETs públicos**: `GET /shelves/users/{userId}` y `GET /shelves/{isbn}` no requieren autenticación. El SecurityConfig permite `GET /shelves/**` sin token, igual que el gateway. Esto permite al frontend mostrar estanterías de usuarios y popularidad de libros sin login.

---

## Bloque 11 — Fase 9: feed social (social-service) + notificaciones (notification-service)

**Objetivo**: dos nuevos servicios de **proyección de lectura pura**: `social-service` (:8086) mantiene el feed de actividad por **fanout-on-write**, y `notification-service` (:8087) crea notificaciones y las empuja al usuario en **tiempo real** vía WebSocket STOMP. Ambos son consumidores de eventos RabbitMQ y usan **MongoDB como única base de datos** (sin JPA/Postgres).

```
social-service/          :8086   solo Mongo · consume 5 colas · GET /feed
notification-service/    :8087   solo Mongo + WebSocket · consume 2 colas · REST + push STOMP
```

**Ficha de `social-service`**

|                 |                                                                                                     |
| --------------- | --------------------------------------------------------------------------------------------------- |
| Puerto          | `8086`                                                                                              |
| Persistencia    | MongoDB (colecciones `activity_items`, `followers`, `feed_entries`)                                 |
| Responsabilidad | Feed personal de actividad: materializa cada evento en el feed de cada usuario (fanout-on-write)    |
| Endpoints clave | `GET /feed?cursor=&limit=` (con `X-User-Id`)                                                        |
| Mensajería      | Consume `follow.followed`, `follow.unfollowed`, `review.created`, `review.updated`, `shelf.changed` |

**Ficha de `notification-service`**

|                 |                                                                                                                 |
| --------------- | --------------------------------------------------------------------------------------------------------------- |
| Puerto          | `8087`                                                                                                          |
| Persistencia    | MongoDB (colección `notifications`)                                                                             |
| Responsabilidad | Notificaciones por usuario, persistidas + push en tiempo real vía WebSocket STOMP                               |
| Endpoints clave | `GET /notifications`, `GET /notifications/unread-count`, `POST /notifications/read`; WS `ws://…:8087/ws?token=` |
| Mensajería      | Consume `follow.followed`; declara cola (sin consumer todavía) para `review.created`                            |

### 11.1 — Esqueleto de un servicio "solo Mongo" (sin JPA)

Los servicios nuevos no tienen Postgres: sus cambios de estado vienen **exclusivamente por eventos RabbitMQ** (excepto el follow, que lo produce user-service publicando en el mismo exchange). Toda la escritura va a MongoDB; no hay command side, ni entidades JPA, ni repos de Spring Data JPA.

`pom.xml` de social-service (notification-service es idéntico más `spring-boot-starter-websocket`):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<!-- + jjwt-api / jjwt-impl / jjwt-jackson 0.12.6 (Apéndice A) -->
```

#### Estructura de paquetes

**social-service**:

```
social-service/src/main/java/com/booksocial/social/
├── SocialServiceApplication.java
├── config/
│   ├── SecurityConfig.java        # parse-only JWT (Apéndice A)
│   └── RabbitConfig.java          # exchange + 5 colas + bindings + converter
├── domain/
│   └── ActivityType.java          # enum: FOLLOW, REVIEW, SHELF
├── readmodel/
│   ├── ActivityItemReadModel.java        # colección activity_items
│   ├── ActivityItemReadModelRepository.java
│   ├── FeedEntryReadModel.java           # colección feed_entries
│   ├── FeedEntryReadModelRepository.java
│   ├── FollowerIndexReadModel.java       # colección followers
│   └── FollowerIndexReadModelRepository.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   └── RestAuthenticationEntryPoint.java
├── events/
│   ├── FollowedEvent.java / UnfollowedEvent.java
│   ├── ReviewEvent.java (sealed) / ReviewCreatedEvent.java / ReviewUpdatedEvent.java
│   ├── ShelfChangedEvent.java
│   ├── FollowEventConsumer.java
│   ├── ReviewEventConsumer.java
│   └── ShelfEventConsumer.java
├── service/
│   └── FeedService.java
└── web/
    ├── FeedController.java
    └── dto/
        ├── FeedItemResponse.java
        └── FeedPageResponse.java
```

**notification-service**:

```
notification-service/src/main/java/com/booksocial/notification/
├── NotificationServiceApplication.java
├── config/
│   ├── SecurityConfig.java        # parse-only JWT (Apéndice A)
│   ├── RabbitConfig.java          # exchange + 2 colas
│   └── WebSocketConfig.java       # @EnableWebSocketMessageBroker (STOMP)
├── readmodel/
│   ├── NotificationReadModel.java        # colección notifications
│   └── NotificationReadModelRepository.java
├── security/
│   ├── JwtService.java
│   ├── JwtAuthFilter.java
│   ├── RestAuthenticationEntryPoint.java
│   └── JwtHandshakeInterceptor.java  # ← propio del WebSocket (no va en el Apéndice A)
├── events/
│   ├── FollowedEvent.java
│   └── FollowEventConsumer.java
├── service/
│   └── NotificationService.java
└── web/
    ├── NotificationController.java
    └── dto/
        └── NotificationResponse.java
```

> La seguridad HTTP son las 4 clases del [Apéndice A](#apéndice-a--plantilla-de-seguridad-reutilizable) sin cambios: `JwtService` parse-only (firma HS256, `requireIssuer("booksocial-identity")`), `JwtAuthFilter` (autoridades `ROLE_*` del claim `roles`), `RestAuthenticationEntryPoint` (401 JSON) y `SecurityConfig` stateless. La única diferencia entre ambos servicios es el `SecurityConfig` (ver abajo).

#### `application.yaml` (ambos servicios)

Son gemelos; cambia `spring.application.name` y `server.port`. Como los demás servicios, importan el `.env`, leen el secreto de `APP_JWT_SECRET` y exponen solo `health`:

```yaml
spring:
  application:
    name: social-service # notification-service en el otro

  config:
    import: "optional:file:.env[.properties]"

  mongodb:
    uri: ${SPRING_MONGODB_URI:mongodb://booksocial:booksocial@localhost:27017/booksocial?authSource=admin}

  rabbitmq:
    host: ${SPRING_RABBITMQ_HOST:localhost}
    port: ${SPRING_RABBITMQ_PORT:5672}
    username: ${SPRING_RABBITMQ_USERNAME:guest}
    password: ${SPRING_RABBITMQ_PASSWORD:guest}

server:
  port: 8086 # 8087 en notification-service

app:
  jwt:
    secret: ${APP_JWT_SECRET}
    issuer: booksocial-identity

management:
  endpoints:
    web:
      exposure:
        include: health
```

#### `SecurityConfig` — qué se abre en cada servicio

```java
// social-service (proxy HTTP normal a través del gateway):
.requestMatchers("/actuator/health").permitAll()
.requestMatchers(HttpMethod.GET, "/feed").authenticated()
.anyRequest().authenticated()

// notification-service: además, el handshake WebSocket NO lleva Authorization header
.requestMatchers("/actuator/health", "/ws/**").permitAll()
.anyRequest().authenticated()
```

El handshake `/ws` está exento del filtro JWT HTTP, pero la **autenticación real del WebSocket** la hace `JwtHandshakeInterceptor` en el propio handshake (ver 11.4). Es la diferencia con el resto de servicios: aquí el token viaja en la URL (`?token=…`), no en un header.

### 11.2 — Nuevos eventos de dominio (Fase 9.2)

Para alimentar el feed hacen falta nuevos eventos. Se añadieron **publicadores** en los servicios de origen (dual-write con Rabbit, misma limitación de outbox documentada en Apéndice B):

#### El mensaje es una "copia privada por servicio" (no una clase compartida)

Cada consumidor define su propio record del evento en **su** paquete de eventos (`com.booksocial.social.events`, `com.booksocial.notification.events`, …), aunque represente el mismo evento con los mismos nombres de campos. No existe un módulo compartido de eventos entre servicios. En el momento de deserializar, `JacksonJsonMessageConverter` se construye con el **trusted package** de ese servicio (sección 8.2): `new JacksonJsonMessageConverter("com.booksocial.social.events")` en social-service y `...("com.booksocial.notification.events")` en notification-service. Así cada servicio solo acepta clases que él mismo define; el "contrato" entre servicios es **implícito por convención** (mismos nombres de campos → misma serialización JSON), no una dependencia de un módulo compartido.

#### Publicadores y consumidores

| Evento                                      | Publicador                              | Routing key                             | Consumidores                                           |
| ------------------------------------------- | --------------------------------------- | --------------------------------------- | ------------------------------------------------------ |
| `FollowedEvent` / `UnfollowedEvent`         | user-service (`FollowEventPublisher`)   | `follow.followed` / `follow.unfollowed` | social (ambos) · notification (solo `follow.followed`) |
| `ReviewCreatedEvent` / `ReviewUpdatedEvent` | review-service (`ReviewEventPublisher`) | `review.created` / `review.updated`     | social                                                 |
| `ShelfChangedEvent`                         | shelf-service (`ShelfEventPublisher`)   | `shelf.changed`                         | social                                                 |

Todos publican al exchange `booksocial.events` con `RabbitTemplate.convertAndSend(EXCHANGE, KEY, event)` y el evento construido con su constructor conveniente (que fija `occurredAt = Instant.now()`):

```java
// user-service → FollowEventPublisher
rabbitTemplate.convertAndSend(EXCHANGE, FOLLOWED_KEY,   new FollowedEvent(followerId, followeeId));
rabbitTemplate.convertAndSend(EXCHANGE, UNFOLLOWED_KEY, new UnfollowedEvent(followerId, followeeId));

// review-service → ReviewEventPublisher
rabbitTemplate.convertAndSend(EXCHANGE, REVIEW_CREATED_KEY, new ReviewCreatedEvent(...));
rabbitTemplate.convertAndSend(EXCHANGE, REVIEW_UPDATED_KEY, new ReviewUpdatedEvent(...));

// shelf-service → ShelfEventPublisher
rabbitTemplate.convertAndSend(EXCHANGE, SHELF_CHANGED_KEY, new ShelfChangedEvent(userId, isbn, title, author, status));
```

#### Contratos de los eventos

```java
// user-service / social-service / notification-service → copias idénticas
public record FollowedEvent(Long followerId, Long followeeId, Instant occurredAt) {
    public FollowedEvent(Long followerId, Long followeeId) {
        this(followerId, followeeId, Instant.now());
    }
}
// UnfollowedEvent: exactamente la misma forma (y mismo constructor secundario)
```

```java
// review-service (público) y social-service (copia privada del mismo shape)
public record ReviewCreatedEvent(Long reviewId, String bookIsbn, String title,
                                 String authorName, Integer rating, String comment,
                                 Long actorUserId, Instant occurredAt) {}
// ReviewUpdatedEvent: la misma forma
```

```java
// social-service: polimorfismo sobre los eventos de review con interface sellada
public sealed interface ReviewEvent permits ReviewCreatedEvent, ReviewUpdatedEvent {
    String bookIsbn();
    String title();
    String authorName();
    Integer rating();
    String comment();
}
```

> Al declarar `ReviewEvent` como `sealed` con `permits` explícitos, el compilador garantiza que cualquier nueva variante se revise en los `switch`. Gracias a la interface, el payload de review se construye **una sola vez** (método `buildReviewPayload(ReviewEvent)`, ver 11.3) y sirve para created y updated.

```java
// shelf-service (público) y social-service (copia)
public record ShelfChangedEvent(Long userId, String bookIsbn, String title,
                                String authorName, String status, Instant occurredAt) {}
```

#### Qué publica y qué no publica cada servicio

- review-service denormaliza `title`/`authorName` **desde sus `book_refs` locales** antes de publicar (en el evento **no** viaja `authorId`): el evento es autocontenido para que el feed no necesite mirar otro servicio.
- shelf-service publica `ShelfChangedEvent` en `create` y en `updateStatus`; el `delete` **no publica** (no hay actividad que mostrar).
- user-service ya publicaba follow/unfollow desde la Fase 2; aquí solo se **añaden consumidores**. El propio user-service también los consume (patrón dual-write del Bloque 6).
- Las colas preexistentes (`review-service.books.created`, `shelf-service.books.created`) se conservan intactas: las nuevas colas son independientes, con prefijo del servicio consumidor.

### 11.3 — Feed por fanout-on-write (social-service)

El feed aplica el patrón "escribe una vez, lee barato": en la escritura se replican copias en cada feed afectado, y la lectura es un simple `find` por `_id`, sin joins.

#### Modelo de datos: 3 colecciones MongoDB

| Colección        | Documento                | `_id`                    | Rol                                        |
| ---------------- | ------------------------ | ------------------------ | ------------------------------------------ |
| `activity_items` | `ActivityItemReadModel`  | determinista (ver abajo) | la actividad pura, única                   |
| `followers`      | `FollowerIndexReadModel` | `String.valueOf(userId)` | índice de seguidores del usuario           |
| `feed_entries`   | `FeedEntryReadModel`     | `feedUserId:activityId`  | copia materializada del feed de un usuario |

##### `ActivityItemReadModel` — la actividad (colección `activity_items`)

```java
@Document(collection = "activity_items")
public class ActivityItemReadModel {
    @Id
    private String id;                        // véase generateActivityId()
    private ActivityType type;                // FOLLOW | REVIEW | SHELF
    private Long actorId;                     // quién la provocó
    private Map<String, Object> payload;      // detalles específicos por tipo
    private Instant occurredAt;               // Instant.now() al crearse

    public ActivityItemReadModel(String id, ActivityType type, Long actorId,
                                 Map<String, Object> payload) {
        this.id = id;
        this.type = type;
        this.actorId = actorId;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }
}
```

```java
public enum ActivityType {   // domain/ActivityType.java
    FOLLOW, REVIEW, SHELF
}
```

El `_id` es **determinista** (`generateActivityId(type, key)` = `type.name() + ":" + key`), no un UUID:

| Tipo     | key del `_id`                | Consecuencia                                                          |
| -------- | ---------------------------- | --------------------------------------------------------------------- |
| `FOLLOW` | `followeeId:followerId`      | seguir dos veces → mismo doc (no duplica)                             |
| `REVIEW` | `reviewId`                   | re-entregas y `review.updated` → mismo doc (update en sitio)          |
| `SHELF`  | `userId:bookIsbn:occurredAt` | cada cambio de estante → actividad **nueva** (historia, no reemplazo) |

Payload por tipo:

| `type`   | `payload`                                          |
| -------- | -------------------------------------------------- |
| `FOLLOW` | `{ "targetUserId": followeeId }`                   |
| `REVIEW` | `{ bookIsbn, title, authorName, rating, comment }` |
| `SHELF`  | `{ bookIsbn, title, authorName, shelfStatus }`     |

##### `FeedEntryReadModel` — el feed materializado (colección `feed_entries`)

```java
@Document(collection = "feed_entries")
public class FeedEntryReadModel {
    @Id
    private String id;             // "feedUserId:activityId" → upsert idempotente
    private Long feedUserId;       // para quién es esta copia
    private String activityId;     // apunta a activity_items
    private Instant occurredAt;    // Instant.now() al hacerse el fanout

    public FeedEntryReadModel(Long feedUserId, String activityId) {
        this.id = feedUserId + ":" + activityId;
        this.feedUserId = feedUserId;
        this.activityId = activityId;
        this.occurredAt = Instant.now();
    }
}
```

Como el `_id` es `feedUserId:activityId`, escribir dos veces el mismo par es un upsert: las **re-entregas de Rabbit no duplican** entradas en el feed.

##### `FollowerIndexReadModel` — índice de seguidores (colección `followers`)

```java
@Document(collection = "followers")
public class FollowerIndexReadModel {
    @Id
    private String id;             // String.valueOf(userId)
    private Long userId;
    private List<Long> followers;  // lista de followerId
}
```

Es el único estado "propio" del servicio (derivado de follow/unfollow) y el que permite saber a qué feeds replicar.

Los repos `ActivityItemReadModelRepository` y `FollowerIndexReadModelRepository` están vacíos (`extends MongoRepository<…, String>`). `FeedEntryReadModelRepository` declara `findByFeedUserIdOrderByOccurredAtDesc(...)`, hoy **sin uso**: `getFeed` usa `MongoTemplate` porque necesita un criterio `$or` con cursor (no un simple orden).

#### `RabbitConfig`: 5 colas durables

```java
public static final String EXCHANGE = "booksocial.events";

public static final String FOLLOWED_QUEUE   = "social-service.follows.followed";
public static final String UNFOLLOWED_QUEUE = "social-service.follows.unfollowed";
public static final String REVIEW_CREATED_QUEUE = "social-service.reviews.created";
public static final String REVIEW_UPDATED_QUEUE = "social-service.reviews.updated";
public static final String SHELF_QUEUE          = "social-service.shelves.changed";
// keys: "follow.followed" · "follow.unfollowed" · "review.created" · "review.updated" · "shelf.changed"
```

Beans: `TopicExchange(EXCHANGE, true, false)` (durable, sin auto-delete) + una `Queue(name, true)` durable y su `Binding` por cada cola. Topología:

```
[booksocial.events] (Topic Exchange, durable)
    ├── "follow.followed"   → [social-service.follows.followed]
    ├── "follow.unfollowed" → [social-service.follows.unfollowed]
    ├── "review.created"    → [social-service.reviews.created]
    ├── "review.updated"    → [social-service.reviews.updated]
    └── "shelf.changed"     → [social-service.shelves.changed]
```

> **Una cola por evento** (mismo principio que 6.5): si dos `@RabbitListener` escucharan la misma cola, Rabbit repartiría los mensajes al azar y la deserialización sería insegura. Converter local: `new JacksonJsonMessageConverter("com.booksocial.social.events")`.

#### Consumers: 3 `@RabbitListener`, un solo paquete

```java
// FollowEventConsumer — las 2 colas de follows
@Component
public class FollowEventConsumer {
    private final FeedService feedService;

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void handleFollowed(FollowedEvent event) {
        feedService.handleFollowed(event.followerId(), event.followeeId());
    }

    @RabbitListener(queues = RabbitConfig.UNFOLLOWED_QUEUE)
    public void handleUnfollowed(UnfollowedEvent event) {
        feedService.handleUnfollowed(event.followerId(), event.followeeId());
    }
}

// ReviewEventConsumer — las 2 colas de reviews
@Component
public class ReviewEventConsumer {
    private final FeedService feedService;

    @RabbitListener(queues = RabbitConfig.REVIEW_CREATED_QUEUE)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        feedService.handleReviewCreated(event);
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_UPDATED_QUEUE)
    public void handleReviewUpdated(ReviewUpdatedEvent event) {
        feedService.handleReviewUpdated(event);
    }
}

// ShelfEventConsumer — 1 cola
@Component
public class ShelfEventConsumer {
    private final FeedService feedService;

    @RabbitListener(queues = RabbitConfig.SHELF_QUEUE)
    public void handleShelfEvent(ShelfChangedEvent event) {
        feedService.handleShelfChanged(event);
    }
}
```

#### `FeedService`: del evento al feed materializado

Los 3 consumers delegan en estos métodos:

```java
public void handleFollowed(Long followerId, Long followeeId) {
    FollowerIndexReadModel idx = followerRepo
            .findById(String.valueOf(followeeId))
            .orElse(new FollowerIndexReadModel(String.valueOf(followeeId), followeeId, new ArrayList<>()));
    if (!idx.getFollowers().contains(followerId))
        idx.getFollowers().add(followerId);
    followerRepo.save(idx);

    String activityId = generateActivityId(ActivityType.FOLLOW,
            String.format("%s:%s", followeeId, followerId));
    activityRepo.save(new ActivityItemReadModel(
            activityId, ActivityType.FOLLOW, followerId,
            Map.of("targetUserId", followeeId)));

    fanout(followerId, activityId);          // feed del actor + seguidores del actor
    fanoutToUser(followeeId, activityId);    // y feed del usuario seguido
}

public void handleReviewCreated(ReviewCreatedEvent event) {
    String activityId = generateActivityId(ActivityType.REVIEW, String.valueOf(event.reviewId()));
    activityRepo.save(new ActivityItemReadModel(
            activityId, ActivityType.REVIEW, event.actorUserId(), buildReviewPayload(event)));
    fanout(event.actorUserId(), activityId);
}

public void handleShelfChanged(ShelfChangedEvent event) {
    String activityId = generateActivityId(ActivityType.SHELF,
            String.format("%s:%s:%s", event.userId(), event.bookIsbn(), event.occurredAt()));
    activityRepo.save(new ActivityItemReadModel(activityId, ActivityType.SHELF, event.userId(),
            Map.of("bookIsbn", event.bookIsbn(), "title", event.title(),
                    "authorName", event.authorName(), "shelfStatus", event.status())));
    fanout(event.userId(), activityId);
}

public void handleUnfollowed(Long followerId, Long followeeId) {
    followerRepo.findById(String.valueOf(followeeId)).ifPresent(idx -> {
        idx.getFollowers().remove(followerId);   // solo el índice; no toca el feed ya escrito
        followerRepo.save(idx);
    });
}

private void fanout(Long actorId, String activityId) {
    feedEntryRepo.save(new FeedEntryReadModel(actorId, activityId));        // su propio feed
    followerRepo.findById(String.valueOf(actorId)).ifPresent(idx ->
            idx.getFollowers().forEach(followerId ->
                    feedEntryRepo.save(new FeedEntryReadModel(followerId, activityId))));
}

private void fanoutToUser(Long userId, String activityId) {
    feedEntryRepo.save(new FeedEntryReadModel(userId, activityId));
}

private String generateActivityId(ActivityType type, String key) {
    return type.name() + ":" + key;
}
```

`handleReviewUpdated` es el caso "update en sitio": si la actividad ya existe, solo se **reemplaza el payload** (mismo `_id`, sin re-fanout y sin cambiar `occurredAt`); si no existe (p. ej. `review.updated` llegó antes que `review.created`), se crea y se hace fanout:

```java
ActivityItemReadModel item = activityRepo.findById(activityId).orElse(null);
if (item == null) {
    item = new ActivityItemReadModel(activityId, ActivityType.REVIEW,
            event.actorUserId(), buildReviewPayload(event));
    activityRepo.save(item);
    fanout(event.actorUserId(), activityId);
} else {
    item.setPayload(buildReviewPayload(event));
    activityRepo.save(item);
}
```

`buildReviewPayload(ReviewEvent event)` construye `Map.of("bookIsbn", ..., "title", ..., "authorName", ..., "rating", ..., "comment", ...)` y sirve para ambos eventos gracias a la interface `ReviewEvent`.

> **Quién ve cada actividad**: `fanout` deja una copia en el feed del actor y en el de **sus** seguidores. El `FOLLOW` además mete una copia en el feed del usuario seguido (`fanoutToUser`). El `unfollow` no borra copias anteriores (sin retroactividad): solo deja de alimentar futuros fanouts.

#### `FeedController` y DTOs

```java
@RestController
@RequestMapping("/feed")
public class FeedController {
    private final FeedService feedService;

    @GetMapping
    public FeedPageResponse getFeed(@RequestHeader("X-User-Id") Long userId,
                                    @RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "10") int limit) {
        return feedService.getFeed(userId, cursor, limit);
    }
}
```

```java
public record FeedPageResponse(List<FeedItemResponse> items, String nextCursor) {}
public record FeedItemResponse(String activityId, String type, Long actorId,
                               Map<String, Object> payload, Instant occurredAt) {}
```

#### Paginación por cursor (sin `skip`)

La implementación real es `FeedPageResponse getFeed(Long userId, String cursor, int limit)`. Recibe el cursor = `_id` del **último `FeedEntryReadModel`** de la página anterior (no un timestamp codificado):

1. Si llega `cursor`, se busca esa entrada para obtener el punto de corte `(occurredAt, id)`:
   ```java
   FeedEntryReadModel lastEntry = feedEntryRepo.findById(cursor).orElse(null);
   ```
2. Query sobre `feed_entries` con `$or` que reproduce el mismo orden de la página anterior:
   ```java
   query.addCriteria(Criteria.where("feedUserId").is(userId));
   if (cutOffOccurredAt != null) {
       query.addCriteria(new Criteria().orOperator(
               Criteria.where("occurredAt").lt(cutOffOccurredAt),
               new Criteria().andOperator(
                       Criteria.where("occurredAt").is(cutOffOccurredAt),
                       Criteria.where("_id").lt(cutOffId))));
   }
   query.with(Sort.by(Sort.Direction.DESC, "occurredAt", "_id"));
   query.limit(limit + 1);
   ```
3. Se piden `limit + 1` entradas: si sobra una, hay más páginas. `nextCursor = pageEntries.getLast().getId()`, o `null` si no hay más.
4. Por cada entrada se resuelve la actividad con `activityRepo.findById(entry.getActivityId())` y se arma el `FeedItemResponse` (entradas huérfanas — actividad inexistente — simplemente se omiten).

- El `$or` (paso 2) es el equivalente de la cláusula `(occurredAt < X) OR (occurredAt = X AND _id < Y)`: la segunda rama actúa de tiebreaker para que **entradas con el mismo instante no se salten ni se repitan**.
- El cliente sigue paginando con `?cursor=nextCursor` (`limit` opcional, default `10`).
- Único endpoint: **`GET /feed?cursor=&limit=`** con `@RequestHeader("X-User-Id")` (lo inyecta el gateway desde el claim `uid`, ver 11.7-error 1).

### 11.4 — Notificaciones + WebSocket STOMP (notification-service)

La notificación es un read model idempotente + un push best-effort: lo crítico (persistir) está garantizado; el push es un extra en tiempo real.

#### Read model y repositorio

```java
@Document(collection = "notifications")
public class NotificationReadModel {
    @Id
    private String id;                // "userId:FOLLOW:followerId" → upsert idempotente
    private Long userId;              // destinatario
    private String type;              // "FOLLOW"
    private Map<String, Object> payload;   // { "followerId": n }
    private boolean read;
    private Instant occurredAt;       // Instant.now() al crearse

    public NotificationReadModel(String id, Long userId, String type,
                                 Map<String, Object> payload, boolean read) {
        this.id = userId + ":" + id;  // el _id compuesto lo fija el constructor
        this.userId = userId;
        this.type = type;
        this.payload = payload;
        this.read = read;
        this.occurredAt = Instant.now();
    }
}
```

El `_id` es determinista (`userId:notificationId`, con `notificationId = "FOLLOW:" + followerId`): `save()` sobre el mismo `_id` es un **upsert**, así que las re-entregas de Rabbit **no duplican** documentos.

```java
public interface NotificationReadModelRepository extends MongoRepository<NotificationReadModel, String> {
    List<NotificationReadModel> findByUserIdOrderByOccurredAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
}
```

#### `RabbitConfig` y consumidor

```java
public static final String EXCHANGE = "booksocial.events";
public static final String FOLLOWED_QUEUE = "notification-service.follows.followed";
public static final String REVIEW_CREATED_QUEUE = "notification-service.reviews.created";
// keys: "follow.followed" · "review.created"
```

Topología:

```
[booksocial.events] (Topic Exchange, durable)
    ├── "follow.followed" → [notification-service.follows.followed]
    └── "review.created"  → [notification-service.reviews.created]   (cola declarada, sin consumer)
```

Converter local de notification-service: `new JacksonJsonMessageConverter("com.booksocial.notification.events")`.

```java
@Component
public class FollowEventConsumer {
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.FOLLOWED_QUEUE)
    public void handleFollowed(FollowedEvent event) {
        notificationService.createFollowNotification(event.followerId(), event.followeeId());
    }
}
```

#### `NotificationService`: persistir y empujar

```java
@Service
public class NotificationService {
    private final NotificationReadModelRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;   // empuja al broker STOMP
    private final MongoTemplate mongoTemplate;               // para el update masivo

    public NotificationResponse createFollowNotification(Long followerId, Long followeeId) {
        String notificationId = "FOLLOW:" + followerId;
        NotificationReadModel n = new NotificationReadModel(
                notificationId, followeeId, "FOLLOW",
                Map.of("followerId", followerId), false);
        notificationRepo.save(n);
        messagingTemplate.convertAndSend("/topic/notifications/" + followeeId, toResponse(n));
        return toResponse(n);
    }

    public List<NotificationResponse> listNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByOccurredAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCount(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    public void markAllAsRead(Long userId) {
        mongoTemplate.updateMulti(Query.query(
                        Criteria.where("userId").is(userId).and("read").is(false)),
                new Update().set("read", true),
                NotificationReadModel.class
        );
    }
}
```

- `createFollowNotification`: **primero** `save()` (upsert por `_id` determinista) y **después** `messagingTemplate.convertAndSend(...)` — el push se emite siempre (no hay "solo si es nuevo"). Si el usuario está desconectado, el push se pierde en el aire, pero la notificación ya está persistida y la recupera con `GET /notifications`.
- `listNotifications` / `unreadCount` usan las queries derivadas del repo.
- `markAllAsRead` es un **bulk** vía `MongoTemplate.updateMulti` (una sola operación, sin cargar documentos): `userId` + `read=false` → `read=true`.
- El destino del push es `/topic/notifications/{id-del-usuario}` (multicast); el destinatario se suscribe a ese topic exacto.

#### `NotificationController` y DTO

```java
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.listNotifications(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    public void markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllAsRead(userId);
    }
}
```

```java
public record NotificationResponse(String id, String type, Map<String, Object> payload,
                                   boolean read, Instant occurredAt) {}
```

Endpoints REST (todos con `@RequestHeader("X-User-Id")`):

| Método | Ruta                          | Descripción                                              |
| ------ | ----------------------------- | -------------------------------------------------------- |
| `GET`  | `/notifications`              | Lista (más recientes primero)                            |
| `GET`  | `/notifications/unread-count` | `{ "count": n }`                                         |
| `POST` | `/notifications/read`         | Marca todo como leído (bulk `MongoTemplate.updateMulti`) |

#### `WebSocketConfig` y `JwtHandshakeInterceptor`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");  // broker simple en memoria (sin Rabbit STOMP)
        registry.setApplicationDestinationPrefixes("/app"); // clientes → servidor (sin uso todavía)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:4200")
                .addInterceptors(jwtHandshakeInterceptor);
    }
}
```

- Agente simple en memoria (`SimpMessagingTemplate`): `/topic/*` multicast (notificaciones), `/queue/*` 1-a-1. No se usa Rabbit STOMP: es pura infraestructura Spring.
- El endpoint `/ws` restringe orígenes a Angular (`http://localhost:4200`) y registra el interceptor.
- En el `SecurityConfig` el path `/ws/**` está `permitAll`, pero eso **no** autentica nada: la autenticación real ocurre en el handshake, con el JWT que el navegador no puede mandar por header HTTP:

```java
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();   // el token llega en la URL: ?token=<jwt>
        String token = null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) { token = param.substring(6); break; }
            }
        }
        if (token == null) return false;              // sin token → se aborta el handshake
        try {
            Claims claims = jwtService.parse(token);
            if (!"access".equals(claims.get("type", String.class))) return false;  // solo access tokens
            Object uid = claims.get("uid");
            if (!(uid instanceof Number)) return false;  // el sub es el email; el id va en "uid"
            attributes.put("userId", ((Number) uid).longValue());
            return true;
        } catch (Exception e) {
            return false;                             // token inválido o issuer distinto
        }
    }
}
```

> Devolver `false` en `beforeHandshake` aborta la conexión antes de abrir el socket; el token se valida con el mismo `JwtService` parse-only y el mismo `APP_JWT_SECRET` (el `uid` del claim es la clave: ver 11.7-error 2).

#### El cliente STOMP se conecta por URL (no por header)

```
ws://localhost:8087/ws?token=<jwt>
```

El cliente se suscribe a `/topic/notifications/{userId}` y recibe el push en tiempo real. La conexión es **directa** a `:8087` (el gateway no proxea WebSockets, ver 11.7-error 4).

### 11.5 — Integración: gateway y docker-compose

Rutas del gateway (mismo patrón strip-and-assert del Bloque 2: el gateway reinyecta `X-User-Id`/`X-User-Roles` desde el JWT, así que los servicios nunca confían en headers del cliente):

```yaml
- id: social-service
  uri: ${SOCIAL_SERVICE_URI:http://localhost:8086}
  predicates:
    - Path=/feed/**

- id: notification-service
  uri: ${NOTIFICATION_SERVICE_URI:http://localhost:8087}
  predicates:
    - Path=/notifications/**
```

En `docker-compose.yml` ambos siguen el **mismo molde** que el resto de servicios (healthchecks con `curl`, `depends_on` a mongodb y rabbitmq ambos `service_healthy`, Dockerfile multi-stage idéntico). El detalle de compose/Dockerfile está en [GUIDE-INFRA.md](./GUIDE-INFRA.md). Solo difieren en puerto y env:

| Servicio               | Contenedor                | Puertos     | `env_file`                     | Overrides en compose                                                  | Healthcheck                                     |
| ---------------------- | ------------------------- | ----------- | ------------------------------ | --------------------------------------------------------------------- | ----------------------------------------------- |
| `social-service`       | `booksocial-social`       | `8086:8086` | `../social-service/.env`       | `SPRING_MONGODB_URI` → `mongodb`, `SPRING_RABBITMQ_HOST` → `rabbitmq` | `curl -f http://localhost:8086/actuator/health` |
| `notification-service` | `booksocial-notification` | `8087:8087` | `../notification-service/.env` | ídem                                                                  | `curl -f http://localhost:8087/actuator/health` |

> **Cómo sobrevive el arranque local (IDE)**: el default `mongodb://...@localhost:27017/booksocial` de `application.yaml` sirve fuera de Docker; el compose lo sobreescribe con la env `SPRING_MONGODB_URI` apuntando al contenedor `mongodb`. Por eso el default no cambia aunque la URI de compose sea distinta.

### 11.6 — Verificación E2E (Docker)

Con dos cuentas reales (`A` sigue, `B` es el seguido). El gateway es quien inyecta `X-User-Id`, así que se prueba con los tokens correctos:

```
# 1) PROVISION: A sigue a B → user-service publica FollowedEvent → social + notification consumen
curl -X POST http://localhost:8080/follows/19 -H "Authorization: Bearer $TOKEN_A"

#    (opcional: B escribe una reseña y cambia su estantería → review.created / shelf.changed)

# 2) FEED de A: contiene la actividad FOLLOW y las actividades de B (review/shelf)
curl http://localhost:8080/feed -H "Authorization: Bearer $TOKEN_A"
curl "http://localhost:8080/feed?limit=1" -H "Authorization: Bearer $TOKEN_A"     # ver nextCursor
curl "http://localhost:8080/feed?cursor=<nextCursor>" -H "Authorization: Bearer $TOKEN_A"   # 2ª página

# 3) NOTIFICACIONES de B: 1 doc FOLLOW persistido + push al topic de B
curl http://localhost:8080/notifications -H "Authorization: Bearer $TOKEN_B"
curl http://localhost:8080/notifications/unread-count -H "Authorization: Bearer $TOKEN_B"  # {"count":1}
curl -X POST http://localhost:8080/notifications/read -H "Authorization: Bearer $TOKEN_B"   # → count 0

# 4) STOMP EN TIEMPO REAL: abrir la conexión y, en otra terminal, ejecutar otro follow
wscat -c "ws://localhost:8087/ws?token=$TOKEN_B"
#   → SUBSCRIBE /topic/notifications/19  (espera el mensaje entrante)
```

Estado verificable en Mongo:

```
db.activity_items.find()                                # actividades FOLLOW/REVIEW/SHELF
db.feed_entries.countDocuments({feedUserId: 10})        # copias materializadas en el feed de A
db.followers.find({_id: "19"})                          # [followerId de A] tras el follow
db.notifications.find({userId: 19})                     # 1 doc FOLLOW (sin duplicados)
```

**Idempotencia ante re-entregas**: republicar a mano el mismo `FollowedEvent` contra el exchange (consola de Rabbit, misma routing key `follow.followed`) **no debe** aumentar `feed_entries` ni `notifications`: sus `_id` compuestos (`feedUserId:activityId`, `userId:FOLLOW:followerId`) convierten el re-`save` en un upsert.

### 11.7 — Errores encontrados en la Fase 9 (con solución directa)

1. **`JwtHandshakeInterceptor` fallaba el handshake**: `Long.valueOf(claims.getSubject())` petaba porque el `sub` del JWT es el **email**. Solución: leer el claim `uid` como `Number` (`((Number) claims.get("uid")).longValue()`).
2. **401 al handshake `/ws`**: exigía auth en `anyRequest()`, pero el handshake no lleva `Authorization: Bearer` (el token va en la URL). Solución: `permitAll("/ws/**")` en el `SecurityConfig` (en notification-service, y también en el gateway mientras estuvo ruteado). La validación real la hace `JwtHandshakeInterceptor` con el mismo `APP_JWT_SECRET`.
3. **Spring Cloud Gateway (WebMVC) NO proxea WebSockets**: error `Can "Upgrade" only to "WebSocket"`. El proxying de WS solo está en la variante Reactiva del gateway. Decisión para Fase 9: el cliente STOMP se conecta **directo** a `ws://localhost:8087/ws?token=`. En producción habría que llegar con un proxy con soporte WS (nginx/traefik) o migrar el gateway a WebFlux.

### Decisiones de diseño de la Fase 9

- **Servicios de lectura pura** ("event-sourced read models"): social y notification solo escriben en Mongo y derivan su estado de los eventos; no hay command side propio. El índice de seguidores es el único "estado" que mantienen (derivado de follow/unfollow).
- **Fanout-on-write** en lugar de leer seguidores en cada petición: la lectura del feed es un `find` simple, a costa de escritura amplificada (`O(seguidores)` por actividad). Correcto para un feed personal con muchos lectores.
- **`_id` deterministas = upsert idempotente** (activities, feed*entries y notifications): las re-entregas de Rabbit no duplican estado. Para `ACTIVITY` de review, el `_id = reviewId` hace que la actualización sea un \_update en sitio* (mismo `_id`, se reemplaza el payload y se conserva `occurredAt`); para shelf se fuerza una actividad nueva por cambio (incluye `occurredAt` en la key).
- **`ReviewEvent` sellada** (`sealed ... permits`): el compilador garantiza que una nueva variante fuerza revisión del switch; el payload se construye una sola vez (`buildReviewPayload(ReviewEvent)`) para created y updated.
- **Follow funciona sin esperar el feed**: el evento `FollowedEvent` lo consume user-service (escrituras propias), social-service (índice + feed) y notification-service (notificación + push). Cada quien, su cola, su copy del evento.
- **Unfollow no retroactivo**: solo se actualiza el índice de seguidores; el feed ya materializado no se depura.
- **WebSocket STOMP vía `SimpMessagingTemplate`**: el push es best-effort; si el usuario no está conectado, no se pierde nada porque la notificación ya está persistida en Mongo y la recupera vía `GET /notifications`.
- **Cabecera `X-User-Id` confiable** gracias al strip-and-assert del gateway (mismo patrón que el resto de servicios).

---

## Apéndice A — Plantilla de seguridad reutilizable

Los servicios downstream (user-service, book-service, review-service, shelf-service) comparten la misma configuración de seguridad: **solo validan JWT, no los generan**. El identity-service es el único que emite tokens. Esta sección consolida el patrón para evitar repetirlo en cada bloque.

### JwtService (parse-only)

```java
@Service
public class JwtService {
    public static final String TYPE_ACCESS = "access";
    private final SecretKey key;     // HMAC desde Base64-encoded secret
    private final String issuer;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = issuer;
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer)
                .build().parseSignedClaims(token).getPayload();
    }
}
```

**Diferencia con identity-service**: este JwtService solo tiene `parse()`. El de identity-service además genera tokens (`generateAccessToken`, `generateRefreshToken`). Comparten el mismo secreto y issuer.

### JwtAuthFilter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                if (JwtService.TYPE_ACCESS.equals(claims.get("type", String.class))) {
                    List<SimpleGrantedAuthority> authorities = ((List<?>) claims.get("roles"))
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                    var auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

### RestAuthenticationEntryPoint

```java
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}");
    }
}
```

### SecurityConfig

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthFilter jwtAuthFilter,
                                    RestAuthenticationEntryPoint entryPoint) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/authors/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/shelves/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

> **Nota**: book-service y shelf-service añaden `requestMatchers(HttpMethod.GET, ...)` para permitir GETs públicos en libros, autores y estanterías. Los demás servicios usan solo `permitAll` para `/actuator/health`.

### application.yaml (sección de seguridad)

```yaml
app:
  jwt:
    secret: ${APP_JWT_SECRET}
    issuer: booksocial-identity
management:
  endpoints:
    web:
      exposure:
        include: health
```

### Dependencias en pom.xml

Cada servicio downstream necesita:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### Cómo copiar la plantilla

Al crear un nuevo servicio downstream:

1. Copiar las 4 clases de seguridad (`JwtService`, `JwtAuthFilter`, `RestAuthenticationEntryPoint`, `SecurityConfig`) desde cualquier servicio existente.
2. Añadir las dependencias de JWT en el `pom.xml`.
3. Añadir `app.jwt.secret` e `app.jwt.issuer` en `application.yaml` y `.env`.
4. Si el servicio tiene endpoints públicos (como `/actuator/health`), añadirlos a `permitAll` en `SecurityConfig`.
5. Si el servicio expone GETs públicos (catálogo, búsqueda, autores), añadir `requestMatchers(HttpMethod.GET, "/ruta/**").permitAll()` tanto en el gateway como en el SecurityConfig del servicio.
6. Si el servicio usa RabbitMQ como consumidor, declarar su propia cola y binding en `RabbitConfig` (cada consumidor es dueño de su cola).

> **No copiar JwtService de identity-service**: ese tiene capacidad de generar tokens. Los servicios downstream solo necesitan `parse()`.

---

## Apéndice B — Decisiones de diseño

Resumen de las decisiones arquitectónicas clave del proyecto:

### Seguridad

- **Secretos por módulo**: `.env` en cada servicio, excluidos de git y de las imágenes; en CI se inyectan como secrets.
- **JWT stateless + secret compartido**: el gateway valida los tokens sin consultar al identity-service.
- **Access corto (15 min) + refresh largo (7 días) rotativo**: el refresh viaja en cookie `httpOnly` + `SameSite=Lax`; el hash SHA-256 se guarda en BD (nunca el token en claro).
- **Patrón strip-then-assert**: el gateway elimina los `X-User-*` del cliente y los reemplaza por los derivados del JWT → los servicios downstream confían en ellos.
- **Roles calculados por edad**: se asigna `MINOR_USER` si la edad (desde `birth_date`) es menor de 18.
- **GETs públicos**: el gateway y los servicios permiten `GET /books/**`, `GET /authors/**` y `GET /shelves/**` sin autenticación. Los POST/PUT/DELETE siguen requiriendo token.

### Persistencia

- **CQRS dual-write**: PostgreSQL para comandos (escrituras), MongoDB para lecturas. Sincronización inicial directa, migrada a eventos RabbitMQ en fases posteriores.
- **Author como entidad independiente**: `Author` en Postgres + Mongo, con `openLibraryId` como clave de cache. Se crea bajo demanda desde Google Books, Open Library o manualmente.
- **Dual APIs externas**: Google Books para libros (búsqueda + auto-import por ISBN), Open Library para autores (búsqueda + datos biográficos + obras). Cache de autores OL en Mongo; Postgres solo para autores locales.
- **`ddl-auto: update` solo en desarrollo**; para producción se usarían migraciones (Flyway/Liquibase).

#### Índices `text` de Mongo (futuro)

La búsqueda actual usa `findByTitleContainingIgnoreCaseOrAuthorNameContainingIgnoreCase`, que genera un regex (`{title: {$regex: "query", $options: "i"}}`). Funciona para la escala actual pero tiene limitaciones:

| Limitación  | Regex actual                                | Índice `text`                           |
| ----------- | ------------------------------------------- | --------------------------------------- |
| Ranking     | Sin orden por relevancia                    | BM25 por defecto                        |
| Tolerancia  | Sin tolerancia a errores ("Graam" ≠ "Gram") | stemming + synonyms configurables       |
| Rendimiento | Full scan de la colección                   | Índice invertido, mucho más rápido      |
| Combinación | OR manual en la query                       | `$text: {$search: "query word1 word2"}` |

Migración futura si se necesita:

```javascript
// Crear índice compuesto
db.books.createIndex(
  { title: "text", authorName: "text" },
  { weights: { title: 3, authorName: 1 } },
);
```

```java
// Query con Spring Data
@Query("{ $text: { $search: ?0 } }")
List<BookReadModel> searchText(String query);
```

Se reconsideraría si: >10k libros en catálogo, se necesita ranking por relevancia, o se requiere búsqueda multilingüe con stemming.

### Infraestructura

- **Parent POM como única fuente de versión** (Spring Boot 4.1.0 + BOM Spring Cloud 2025.1.2).
- **Contenedores con healthchecks y `depends_on: service_healthy`** para arranques ordenados y verificables.
- **Dockerfiles multi-stage** para imágenes mínimas (build en `maven:3.9-eclipse-temurin-21`, runtime en `eclipse-temurin:21-jre`).

### Controllers

- **Return directo** para respuestas 200 (Spring envuelve el objeto en JSON automáticamente).
- **`@ResponseStatus`** para códigos explícitos: `CREATED` (201) en POST, `NO_CONTENT` (204) en DELETE.
- **`ResponseEntity`** solo cuando se necesita manipular la respuesta programáticamente (headers, cookies). Ejemplo justificado: `AuthController` que añade `Set-Cookie` vía `HttpServletResponse`.

### Mensajería

- **Una cola por evento**: RabbitMQ reparte entre listeners de la misma cola; con una cola por tipo, cada listener recibe un tipo concreto.
- **Contadores recalculados, no incrementados**: hace las operaciones idempotentes ante re-entregas del broker.
- **Evento `BookCreatedEvent` con 4 campos**: `bookIsbn`, `title`, `authorName`, `authorId`. Los consumidores desnormalizan `authorName`+`authorId` en sus read models para evitar joins.
- **Sin Transactional Outbox**: se documenta como limitación conocida.

#### ¿Qué es el Transactional Outbox?

En un sistema dual-write (Postgres + RabbitMQ en la misma operación), existe un **hueco de consistencia**: si la app se cae después del `save()` en Postgres pero antes del `convertAndSend()` a RabbitMQ, el evento se pierde y los read models quedan desincronizados.

El patrón Outbox resuelve esto escribiendo el evento en una tabla `outbox` **dentro de la misma transacción** que el negocio. Un mecanismo separado (polling con `@Scheduled` o CDC con Debezium) publica los eventos pendientes en RabbitMQ. Así, la escritura y la publicación nunca se separan.

```
┌─────────────────────────────────────────────────────┐
│  Transacción                                        │
│  ┌──────────────┐  ┌────────────────────────────┐   │
│  │ business_tbl │  │ outbox (evento pendiente)  │   │
│  └──────┬───────┘  └─────────────┬──────────────┘   │
│         └─────────── COMMIT ─────┘                  │
└─────────────────────────────────────────────────────┘
                         │
                 @Scheduled / CDC
                         │
                         ▼
                     RabbitMQ → consumers
```

#### Por qué no se implementa en BookSocial

- **Baja frecuencia**: 3 tipos de eventos (`book.created`, `follow.followed`, `follow.unfollowed`) con tráfico mínimo.
- **Consumidores idempotentes**: todos recalculan contadores o hacen upsert, nunca incrementan. Un evento perdido deja el read model desactualizado pero no corrupto.
- **Reparabilidad manual**: si `book.created` se pierde, review-service y shelf-service no tendrán el libro en `book_refs`. Es fácil de detectar y reparar reenviando el evento.
- **Complejidad añadida**: tabla `outbox`, polling o CDC (Debezium), limpieza de registros publicados. Injustificado para la escala actual.

Se reconsideraría si el proyecto creciera a >100 eventos/minuto, se añadieran eventos críticos (pagos, notificaciones), o los consumidores dejaran de ser idempotentes.

#### Modelo push: fanout-on-write y notificaciones en tiempo real

BookSocial usa el **modelo push** tanto para el feed como para las notificaciones (Fase 9, Bloque 11): el estado se **replica/empuja hacia el consumidor en el momento de la escritura**, en lugar de calcularse bajo demanda en la lectura.

**Feed: fanout-on-write (push) frente a fanout-on-read (pull)**

Para construir un feed personal hay dos estrategias clásicas (popularizadas en el paper _Timelines at Scale_ de Twitter):

|                     | Fanout-on-write (push)                                                        | Fanout-on-read (pull)                                                             |
| ------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| Cuándo se replica   | En la **escritura** de la actividad                                           | En la **lectura** del feed                                                        |
| Escritura           | Amplificada: `O(seguidores)` copias por actividad                             | Barata: una sola actividad por autor                                              |
| Lectura             | Barata: un `find` con índice por `feedUserId`                                 | Cara: consultar la timeline de cada seguido (`O(seguidos)`) y hacer merge + orden |
| Latencia de lectura | Constante (no depende de cuánta gente sigues)                                 | Crece con el nº de seguidos                                                       |
| Consistencia        | Eventual: la copia se crea cuando llega el evento                             | Inmediata: se lee de la fuente                                                    |
| Idempotencia        | Exigida: `_id` compuesto (`feedUserId:activityId`) hace el `save()` un upsert | Natural: no hay copias                                                            |

En nuestra implementación:

- La "escritura" es el momento en que RabbitMQ entrega el evento. El `FeedService` materializa copias en `feed_entries` para el actor, sus seguidores y (en el `FOLLOW`) el usuario seguido. Leer `GET /feed` es una **sola query** indexada con cursor, sin joins ni merges.
- El coste del push es la escritura amplificada: cada actividad genera `O(seguidores)` documentos. Se asume porque encaja en **pocas escrituras / muchos lectores** — la lectura barata es lo que importa en un feed.
- El límite conocido es el **hybrid fanout** de las redes grandes (los autores famosos no replican a millones; los seguidores leen contra su timeline con pull). A esta escala no hace falta.

**Notificaciones: push en tiempo real, pull como respaldo**

notification-service aplica la misma filosofía push en dos planos:

| Plano                         | Mecanismo                                                                    | Tipo                                      |
| ----------------------------- | ---------------------------------------------------------------------------- | ----------------------------------------- |
| Evento → read model           | `FollowEventConsumer` recibe el evento y `save()` la notificación en Mongo   | Push del _estado_ (conducido por eventos) |
| Servidor → cliente            | `SimpMessagingTemplate.convertAndSend("/topic/notifications/{userId}", ...)` | Push del _mensaje_ (WebSocket STOMP)      |
| Cliente → servidor (respaldo) | `GET /notifications` sobre Mongo                                             | Pull (chequeo de la fuente de verdad)     |

El push STOMP es **best-effort**: si el usuario está desconectado el mensaje se pierde. Por eso el `save()` en Mongo es la fuente de verdad y el cliente puede caer siempre en `GET /notifications` (pull). Es decir: **push para la experiencia en tiempo real, pull para no perder nada**.

Frente al **polling REST** (preguntar a `GET /notifications` cada N segundos), el push tiene:

- **latencia mínima**: el evento llega solo cuando hay algo nuevo (sin ventana de espera de N segundos);
- **menos tráfico ocioso**: no hay peticiones "vacías" periódicas;
- **persistencia de conexión**: el socket STOMP queda abierto, el servidor empuja cuando toca.

En el conjunto del proyecto, RabbitMQ es el "brazo push" **entre servicios** (event-driven frente a REST síncrono request/response), y STOMP extiende ese mismo concepto hasta el navegador (ver Apéndice E).

---

## Apéndice C — Operación rápida (backend)

Referencia mínima del día a día con el backend ya construido. Los comandos de **arranque/parada, redespliegue, logs y herramientas de inspección** (Docker Compose) están detallados en [GUIDE-INFRA.md](./GUIDE-INFRA.md) → Bloque 2 "Operación local". Resumen:

| Contenedor                | Servicio             | Puerto          |
| ------------------------- | -------------------- | --------------- |
| `booksocial-postgres`     | PostgreSQL 16        | 5432            |
| `booksocial-mongodb`      | MongoDB 8.0          | 27017           |
| `booksocial-rabbitmq`     | RabbitMQ 4           | 5672 / UI 15672 |
| `booksocial-gateway`      | gateway              | 8080            |
| `booksocial-identity`     | identity-service     | 8081            |
| `booksocial-user`         | user-service         | 8082            |
| `booksocial-book`         | book-service         | 8083            |
| `booksocial-review`       | review-service       | 8084            |
| `booksocial-shelf`        | shelf-service        | 8085            |
| `booksocial-social`       | social-service       | 8086            |
| `booksocial-notification` | notification-service | 8087            |

### Redesplegar un servicio tras cambiar su código

```powershell
docker compose -f infrastructure/docker-compose.yml build book-service   # reconstruye la imagen
docker compose -f infrastructure/docker-compose.yml up -d book-service   # recrea el contenedor
```

Solo ese servicio se reconstruye; bases de datos y RabbitMQ no se tocan. Si el contenedor sirve **código antiguo**, fuerza rebuild: `docker compose build --no-cache <servicio>`.

### Desarrollo local sin reconstruir imágenes

Para iterar rápido en un microservicio puedes ejecutarlo directamente en tu máquina, sin Docker: los puertos de las bases están publicados en `localhost` y las credenciales están en el `.env` del módulo.

```powershell
cd book-service
./mvnw spring-boot:run        # arranca en :8083 conectando a localhost:5432/27017/5672
```

Mientras tanto el resto del stack sigue en sus contenedores. Recuerda que el gateway enruta a los contenedores (`BOOK_SERVICE_URI=http://book-service:8083`), así que para probar tu instancia local llama a `:8083` directamente.

### Logs (diagnóstico)

Todo el stdout de Spring va al log del contenedor (`docker logs <contenedor>`). Qué buscar según el síntoma:

| Síntoma                         | Dónde mirar                                     |
| ------------------------------- | ----------------------------------------------- |
| HTTP 500 en un endpoint         | log del servicio dueño (stack trace completa)   |
| 401/403 inesperado              | log del gateway (filter JWT) y del servicio     |
| Servicio no arranca / unhealthy | `docker logs <contenedor>` al completo          |
| Read model desactualizado       | log del consumidor (review/shelf) + UI RabbitMQ |

### Frontend

Para arrancar/build/i18n del SPA, ver [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md). Resumen: `cd frontend` → `npm start` (ng serve en `:4200` con proxy a `:8080`). Si editas `proxy.conf.json` hay que **reiniciar** `ng serve` (el proxy solo se lee al arrancar).

---

## Apéndice D — RabbitMQ: del publisher al consumer

RabbitMQ (broker AMQP 0-9-1) es el **bus de eventos** del proyecto. A lo largo de la guía se construye por partes: el primer evento en 6.5, el patrón converter/trusted package en 8.2, y la generación de eventos sociales en 11.2-11.4. Este apéndice unifica el mapa completo: **qué publica cada servicio, con qué routing key, qué colas lo reciben y qué efecto produce cada consumidor**.

### Conceptos mínimos

| Concepto        | Qué es                                                                         | En BookSocial                                                                           |
| --------------- | ------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| **Producer**    | Servicio que publica mensajes                                                  | book, user, review y shelf-service (los que escriben hechos)                            |
| **Exchange**    | Enrutador central: recibe mensajes y decide destino                            | `booksocial.events` (topic exchange, durable)                                           |
| **Routing key** | Etiqueta del mensaje; el topic exchange la compara con los bindings            | `book.created`, `follow.followed`, `shelf.changed`, …                                   |
| **Binding**     | Regla que une una cola al exchange mediante un pattern                         | `BindingBuilder.bind(queue).to(exchange).with(key)` en el `RabbitConfig` del consumidor |
| **Queue**       | Cola **durable** donde Rabbit guarda el mensaje hasta que el listener confirma | `social-service.reviews.created`, `user-service.follows.followed`, …                    |
| **Consumer**    | Listener que lee de la cola                                                    | métodos `@RabbitListener` de cada servicio                                              |
| **ACK**         | Confirmación que libera el mensaje de la cola                                  | ACK automático de Spring al volver del listener                                         |

### El flujo, paso a paso

```
                          RabbitMQ
┌──────────────┐        ┌─────────────────────────────────┐
│ user-service │        │ exchange "booksocial.events"    │
│              │        │         (topic exchange)        │
│ FollowEvent  │───────►│                                 │
│  Publisher   │        └───────────────┬─────────────────┘
└──────────────┘                        │     publish
                                        │ "follow.followed"
                                        │
                         ┌──────────────┴──────────────┐
                         │                             │
                       binding                      binding
                   follow.followed               follow.followed
                         │                             │
                         ▼                             ▼
             ┌──────────────────────┐      ┌──────────────────────────┐
             │ queue                │      │ queue                    │
             │ "user-service.       │      │ "notification-service.   │
             │  follows.followed"   │      │  follows.followed"       │
             └──────────┬───────────┘      └────────────┬─────────────┘
                        │                               │
                        ▼                               ▼
             ┌──────────────────────┐      ┌──────────────────────────┐
             │ FollowEventConsumer  │      │ FollowEventConsumer      │
             │ user-service         │      │ notification-service     │
             │                      │      │                          │
             │ MongoDB: follows     │      │ Notificación + STOMP     │
             └──────────────────────┘      └──────────────────────────┘
```

En palabras:

1. El publicador envía `RabbitTemplate.convertAndSend(EXCHANGE, KEY, event)` con la **routing key** aparte del JSON.
2. El exchange `booksocial.events` (topic) compara la key con los patterns de todos sus bindings.
3. Por cada binding que coincide, Rabbit **copia el mensaje a esa cola**. N bindings → N copias (publish/subscribe).
4. Cada consumidor lee de **su** cola. Al terminar, el ACK libera el mensaje y sale de la cola.

### Topología real: 11 colas, 1 exchange

Un solo exchange (`booksocial.events`) y **11 colas**, cada una declarada por el servicio que la consume:

| Dueño                | Colas                                                                                                                                                                        |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| user-service         | `user-service.follows.followed`, `user-service.follows.unfollowed`                                                                                                           |
| review-service       | `review-service.books.created`                                                                                                                                               |
| shelf-service        | `shelf-service.books.created`                                                                                                                                                |
| social-service       | `social-service.follows.followed`, `social-service.follows.unfollowed`, `social-service.reviews.created`, `social-service.reviews.updated`, `social-service.shelves.changed` |
| notification-service | `notification-service.follows.followed`, `notification-service.reviews.created`                                                                                              |

### El flujo completo, publisher → consumer

**user-service — sigue/deja de seguir (parser: Postgres → evento):**

| Evento            | Routing key         | Cola                                    | Consumidor                                    | Efecto                                                                                    |
| ----------------- | ------------------- | --------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `FollowedEvent`   | `follow.followed`   | `user-service.follows.followed`         | user-service `FollowEventConsumer.onFollowed` | crea `FollowReadModel` en Mongo + recalcula `followingCount`/`followersCount`             |
|                   |                     | `social-service.follows.followed`       | social-service `FeedService`                  | materializa la actividad FOLLOW (una copia en el feed del autor y otra en el del seguido) |
|                   |                     | `notification-service.follows.followed` | notification-service `FollowEventConsumer`    | guarda la notificación en Mongo + push STOMP                                              |
| `UnfollowedEvent` | `follow.unfollowed` | `user-service.follows.unfollowed`       | user-service `onUnfollowed`                   | borra `FollowReadModel` + recalcula contadores                                            |
|                   |                     | `social-service.follows.unfollowed`     | social-service                                | elimina las entradas FOLLOW del feed del autor y del seguido                              |

**book-service — alta de un libro:**

| Evento             | Routing key    | Cola                           | Consumidor                                | Efecto                           |
| ------------------ | -------------- | ------------------------------ | ----------------------------------------- | -------------------------------- |
| `BookCreatedEvent` | `book.created` | `review-service.books.created` | review-service `BookCreatedEventConsumer` | crea el `BookRef` de `book_refs` |
|                    |                | `shelf-service.books.created`  | shelf-service `BookCreatedEventConsumer`  | crea el `BookRef` de `book_refs` |

**review-service — reseñas:**

| Evento               | Routing key      | Cola                                   | Consumidor                   | Efecto                                                             |
| -------------------- | ---------------- | -------------------------------------- | ---------------------------- | ------------------------------------------------------------------ |
| `ReviewCreatedEvent` | `review.created` | `social-service.reviews.created`       | social-service `FeedService` | materializa la actividad REVIEW (autor + seguidores)               |
|                      |                  | `notification-service.reviews.created` | **— (sin listener)**         | cola declarada pero nadie la consume aún: los mensajes se acumulan |
| `ReviewUpdatedEvent` | `review.updated` | `social-service.reviews.updated`       | social-service               | upsert del contenido en las entradas REVIEW existentes             |

**shelf-service — estantería:**

| Evento              | Routing key     | Cola                             | Consumidor                   | Efecto                                                                            |
| ------------------- | --------------- | -------------------------------- | ---------------------------- | --------------------------------------------------------------------------------- |
| `ShelfChangedEvent` | `shelf.changed` | `social-service.shelves.changed` | social-service `FeedService` | materializa la actividad SHELF en el feed del autor (leída/nueva según la acción) |

### Reglas que gobiernan el flujo

- **Un exchange, una cola por evento-consumidor.** Cada servicio crea su cola y su binding; Rabbit copia a las colas cuyo pattern coincide (publish/subscribe). Dentro de una cola, solo hay 1 listener por servicio en este proyecto.
- **Keys jerárquicas `dominio.accion`**: `book.created`, `follow.followed`/`follow.unfollowed`, `review.created`/`review.updated`, `shelf.changed`. Un topic exchange permitiría suscripciones con comodines (`follow.*`), aunque aquí cada cola se ciñe a una key exacta.
- **Todo durable**: `TopicExchange(EXCHANGE, true, false)` y `new Queue(name, true)`; el mensaje sobrevive reinicios del broker si el consumidor no lo había confirmado.
- **Declaración idempotente**: no hay app de gestión; cada servicio declara exchange, colas y bindings al arrancar en su `RabbitConfig`. Rabbit devuelve sin error si ya existen (declare-and-check).
- **Contrato por convención**: el body es JSON; cada servicio declara su trusted package en el converter (`JacksonJsonMessageConverter("com.booksocial.<servicio>.events")`) y define una **copia privada del record** (sección 11.2). La serialización no depende de un módulo compartido.
- **ACK automático + redelivery**: si el listener lanza una excepción, el mensaje se devuelve a la cola y se reintenta. Por eso todos los consumidores son **idempotentes** (upsert/recalculo, nunca incrementos) — ver Apéndice B.

### Cómo inspeccionarlo en caliente

- **UI de gestión**: `http://localhost:15672` (guest/guest). En _Exchanges → booksocial.events_ ves los bindings; en _Queues_ ves las 11 colas con mensajes `Ready`/`Unacked`.
- **Estado sano = colas en 0.** Si una cola acumula mensajes `Ready`:
  - servicio consumidor caído, o
  - cola declarada sin `@RabbitListener` — el caso real de `notification-service.reviews.created` (se acumula por diseño mientras no exista el consumidor).
- Para inspección por logs del contenedor y comandos de diagnóstico, remite a Apéndice C (Herramientas de inspección).

---

## Apéndice E — WebSocket (STOMP): del servidor al navegador

WebSocket + STOMP viven en notification-service (:8087) y son el **último tramo del push**: RabbitMQ lleva el evento hasta notification-service (Apéndice D) y STOMP lo empuja de ahí **hasta el navegador**, en tiempo real. Este apéndice mapea ese tramo: cómo se autentica la conexión, cómo se suscribe el cliente y cómo viaja la notificación de Mongo a la pantalla.

### Conceptos mínimos

| Concepto                    | Qué es                                                                                                    | En BookSocial                             |
| --------------------------- | --------------------------------------------------------------------------------------------------------- | ----------------------------------------- |
| **WebSocket**               | Conexión TCP bidireccional y persistente; se abre mediante `HTTP Upgrade`                                 | endpoint `/ws` de notification-service    |
| **STOMP**                   | Protocolo de mensajería de alto nivel sobre WebSocket (frames `CONNECT`, `SUBSCRIBE`, `SEND`, `MESSAGE`…) | todo el tráfico cliente↔broker            |
| **Handshake**               | El `HTTP Upgrade` inicial; aquí se valida el JWT                                                          | `JwtHandshakeInterceptor.beforeHandshake` |
| **Broker simple**           | Registro en memoria que reparte a destinos `*` y `*`                                                      | `enableSimpleBroker("/topic", "/queue")`  |
| **Topic**                   | Destino multicast: todos los suscritos reciben cada mensaje                                               | `/topic/notifications/{userId}`           |
| **Suscripción**             | El cliente se registra en un destino para recibirlo                                                       | `SUBSCRIBE /topic/notifications/19`       |
| **SimpMessagingTemplate**   | API del lado servidor para publicar en el broker                                                          | `convertAndSend(destino, payload)`        |
| **Application destination** | Envíos del cliente al servidor (`/app/*`)                                                                 | configurado, todavía sin uso              |

### El flujo completo (3 tramos)

```
 Tramo 1 (Rabbit)          Tramo 2 (servicio)                 Tramo 3 (WebSocket STOMP)
┌──────────────┐     ┌──────────────────────────────────┐     ┌──────────────────────┐
│ user-service │────►│ notification-service             │────►│ navegador            │
│ FollowEvent  │     │  FollowEventConsumer             │     │ SUBSCRIBE            │
│  Publisher   │     │    │  NotificationService        │     │ /topic/notifications │
│ follow.      │     │    │  ├─ save()        → Mongo   │     │ /{userId}            │
│ followed     │     │    │  └─ convertAndSend(topic)───┼────►│ MESSAGE frame        │
└──────────────┘     └──────────────────────────────────┘     └──────────────────────┘
   evento                 persistir + empujar                          recibir
```

1. **Evento (Rabbit)**: user-service publica `follow.followed`; `FollowEventConsumer` de notification-service lo recibe (colas, Apéndice D).
2. **Persistir**: `NotificationService.createFollowNotification(followerId, followeeId)` hace `save()` del `NotificationReadModel` en Mongo → **fuente de verdad**.
3. **Empujar**: `messagingTemplate.convertAndSend("/topic/notifications/" + followeeId, toResponse(n))`. Spring serializa el DTO a JSON y el broker simple lo entrega como frame `MESSAGE` a los suscriptores de ese topic.
4. **Recibir**: el cliente suscrito recibe el frame; si nadie está suscrito, el frame se descarta sin pérdida porque la notificación ya quedó persistida (pull: `GET /notifications`).

### El handshake: autenticar sin header

Un navegador no puede poner cabeceras `Authorization` en un `WebSocket` (la API solo permite configurar subprotocols), de ahí que la **autenticación ocurra en el handshake, con el token en la URL**:

```
ws://localhost:8087/ws?token=<jwt>
```

`JwtHandshakeInterceptor.beforeHandshake` (mismo `JwtService` parse-only y `APP_JWT_SECRET` que el resto de servicios):

| Comprobación                       | Resultado `false` → se aborta la conexión antes de abrir el socket           |
| ---------------------------------- | ---------------------------------------------------------------------------- |
| Hay parámetro `token` en el query  | sin token → abort                                                            |
| `jwtService.parse(token)` no lanza | token inválido/expirado/otro issuer → abort                                  |
| `claims.get("type") == "access"`   | un refresh token → abort                                                     |
| `claims.get("uid")` es `Number`    | sin `uid` numérico → abort                                                   |
| Si todo ok                         | `attributes.put("userId", uid)` queda disponible para futuros usos del canal |

> En `SecurityConfig` el path `/ws/**` está `permitAll` en el filtro HTTP, pero **eso no autentica nada**: la seguridad real vive en el handshake. Es el mismo patrón que con los GETs públicos del proyecto: `permitAll` solo evita el 401 del filtro, la validación de identidad ocurre donde corresponde (aquí, en el interceptor del handshake).

### El broker y los destinos

- **Broker simple en memoria** de Spring (`/topic`, `/queue`). No se usa el plugin STOMP de RabbitMQ: es pura infraestructura Spring, un broker intra-proceso dentro de notification-service.
- **`/topic/*`**: multicast (uno → todos los suscritos). Cada notificación se publica en `/topic/notifications/{userId}` — el topic lleva el id del destinatario, así solo su suscripción lo recibe (privacidad por construcción).
- **`/queue/*`**: 1-a-1, registrado pero sin uso todavía.
- **`/app`**: prefijo de mensajes cliente→servidor (`SEND`), configurado sin handlers todavía (no hay comandos del cliente hacia el servidor).
- Envía a través del `WebSocketMessageBrokerConfigurer` (`SimpMessagingTemplate`) sin `@MessageMapping`: no hay endpoints de entrada, solo salida push.

### Dónde vive cada pieza

| Pieza                     | Archivo                                 | Rol                                                                                             |
| ------------------------- | --------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `WebSocketConfig`         | `config/WebSocketConfig.java`           | registro STOMP: broker `/topic`,`/queue`, prefijo `/app`, endpoint `/ws`, orígenes, interceptor |
| `JwtHandshakeInterceptor` | `security/JwtHandshakeInterceptor.java` | autenticación en el handshake (token en query, parse-only, guarda `userId`)                     |
| `NotificationService`     | `service/NotificationService.java`      | dispara `convertAndSend` después del `save()`                                                   |
| `FollowEventConsumer`     | `events/FollowEventConsumer.java`       | bridge Rabbit → `NotificationService`                                                           |
| `NotificationController`  | `web/NotificationController.java`       | REST de respaldo (pull): list, unread-count, mark-all-read                                      |
| `NotificationReadModel`   | `readmodel/`                            | fuente de verdad (Mongo)                                                                        |
| `SecurityConfig`          | `config/SecurityConfig.java`            | `/ws/**` `permitAll` (la seguridad es el handshake)                                             |

### Push vs pull: el respaldo REST

| Operación          | Push (STOMP)                                       | Pull (REST)                                              |
| ------------------ | -------------------------------------------------- | -------------------------------------------------------- |
| Nueva notificación | frame `MESSAGE` en `/topic/notifications/{userId}` | `GET /notifications` (lista, `occurredAt` desc)          |
| No leídas          | —                                                  | `GET /notifications/unread-count`                        |
| Marcar leídas      | —                                                  | `POST /notifications/read` (`updateMulti` → `read=true`) |

### Inspección y problemas típicos

```powershell
# Cliente de prueba: abre la conexión y espera el push
wscat -c "ws://localhost:8087/ws?token=$TOKEN_B"
#   → SUBSCRIBE /topic/notifications/19   (entonces provocar un follow en otra terminal)
```

| Síntoma                                             | Causa probable                                                                                                                                |
| --------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| Handshake rechazado (501/log de abort)              | token ausente, expirado, tipo distinto de `access`, issuer distinto u **origen no permitido** (la App solo acepta `http://localhost:4200`)    |
| La notificación está en Mongo pero no llega el push | conexión no abierta, o suscrito a un topic con `userId` distinto                                                                              |
| El mensaje llega a `wscat` pero no a Angular        | el cliente STOMP está conectado a otro origen/puerto; recordar que el gateway **no** proxea WS (conexión directa a `:8087`, ver 11.7-error 4) |
| Siempre puedes recuperar                            | `GET /notifications` desde el gateway (pull)                                                                                                  |

### Qué no se usa (decisiones conscientes)

- **Sin RabbitMQ STOMP**: el broker es el simple en memoria de Spring; Rabbit solo entrega el evento hasta el servicio (Apéndice D).
- **Sin SockJS fallback**: `addEndpoint("/ws")` sin `.withSockJS()` → conexión directa `ws://`. Si se quisiera soporte de proxies/proxies sin WS, sería el siguiente paso.
- **Sin destinos globales**: todos los topics llevan el `userId` del destinatario; no hay broadcast sin identidad.
- **El `userId` del handshake queda en `attributes`** pero de momento no se usa contra la suscripción: la segmentación por topic ya impide cruzarse notificaciones.

---

_Para la interfaz de usuario (Angular): ver [GUIDE-FRONTEND.md](./GUIDE-FRONTEND.md)._

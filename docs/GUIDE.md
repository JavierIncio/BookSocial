## Bloque 0 — Cimientos (monorepo + infraestructura + CI)

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

La estructura inicial del proyecto será:

```
  frontend/
  gateway/
  identity-service/
  user-service/
  book-service/
  review-service/
  shelf-service/
  social-service/
  club-service/
  messaging-service/
  news-service/
  notification-service/
  infrastructure/
  docs/
```

- Cada `microservicio` tendrá **aislamiento total**: su propio código, _pom.xml_, _.env_ y _Dockerfile_. Esto permite que cada servicio gestione de forma independiente su configuración y ciclo de ejecución.

- `infrastructure/` contiene todo aquello que pertenece a la infraestructura y no al código de negocio, como _Docker Compose_, _Terraform_ y configuraciones relacionadas.

- `docs/` contiene la documentación del proyecto, incluyendo el `ROADMAP` y el estado de cada sesión de desarrollo.

El `.gitignore` raíz tendrá cuatro bloques principales:

```gitignore
  # Java/Maven
    target/
    *.class
    *.jar
    !.mvn/wrapper/maven-wrapper.jar
    .idea/
    *.iml
    .vscode/

  # Node/Angular
    node_modules/
    dist/
    .angular/cache/
    *.log

  # Entorno y secretos
    .env
    .env.*
    application-local.yml
    application-local.yaml
    secrets/
    *.pem
    *.key

  # Infraestructura
    *.tfstate
    *.tfstate.backup
    *.tfvars
    .terraform/
```

El bloque de **entorno y secretos** es especialmente importante. La política del proyecto es que ningún dato sensible viva en Git.

Los archivos `.env` serán **específicos de cada módulo o microservicio**, en lugar de mantener un único `.env` global. Estos archivos contendrán la configuración sensible necesaria para cada servicio y se cargarán en runtime, como se explicará en el _Bloque 1_.

De esta forma, el repositorio contiene el código y la configuración no sensible necesaria para entender el proyecto, pero nunca credenciales, tokens, claves privadas, contraseñas u otros secretos.

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

> Si no se definen, el usuario y contraseña por defecto de RabbitMQ son: `guest / guest`

1. **Volúmenes**
   Cada servicio utiliza un volumen:

```yml
volumes:
  - postgres-data:/var/lib/postgresql/data
```

Los volúmenes permiten que los datos sobrevivan al ciclo de vida del contenedor.

El contenedor de PostgreSQL se elimina y se crea de nuevo, pero los datos permanecen almacenados en postgres-data. Sin un volumen, al eliminar el contenedor también se perderían los datos almacenados dentro de él.

Esto es especialmente importante durante el desarrollo, porque permite reiniciar la infraestructura sin tener que reconstruir las bases de datos continuamente.

2. **Healthchecks**
   Cada servicio tiene un healthcheck específico:

| Servicio   | Comprobación                |
| ---------- | --------------------------- |
| PostgreSQL | `pg_isready`                |
| MongoDB    | `mongosh` + `ping`          |
| RabbitMQ   | `rabbitmq-diagnostics ping` |

El `healthcheck` no significa simplemente que el contenedor se haya iniciado. Comprueba que el servicio que contiene está realmente disponible.

Por ejemplo, un **contenedor de _PostgreSQL_** puede estar arrancado mientras PostgreSQL todavía está inicializándose. El healthcheck permite distinguir ambos estados.

El estado puede consultarse con `docker compose ps` y veremos estados como: `healthy | unhealthy | starting`.

Este mismo mecanismo será importante posteriormente cuando los microservicios se incorporen al `docker-compose.yml` y se utilice `depends_on` con condiciones basadas en healthcheck. Por ejemplo:

```yml
depends_on:
  postgres:
    condition: service_healthy
```

El microservicio puede esperar a que PostgreSQL esté realmente disponible antes de iniciar

3. **Puertos publicados**
   Los puertos se publican del contenedor hacia el host:

   ```yml
   ports:
     - "5432:5432"
   ```

Mientras los microservicios se ejecuten directamente en el ordenador, podrán conectarse utilizando localhost:

```
  PostgreSQL → localhost:5432
  MongoDB    → localhost:27017
  RabbitMQ   → localhost:5672
```

**RabbitMQ** publica además el puerto `15672`:

```yml
ports:
  - "5672:5672"
  - "15672:15672"
```

El puerto `5672` es el utilizado por las aplicaciones para comunicarse con **RabbitMQ**, mientras que `15672` proporciona la interfaz web de administración. La consola estará disponible en: [http://localhost:15672](http://localhost:15672)

### 0.5 — Workflow base de CI (ci.yml)

El proyecto tendrá un workflow básico de Integración Continua (CI) en `.github/workflows/ci.yml`. Una configuración inicial es:

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
      - name: Checkout
        uses: actions/checkout@v5

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

1. ¿Cuándo se ejecuta?

```yml
on:
  push:
    branches: [main]
  pull_request:
```

- **Push a `main`:** Cada vez que se hace push directamente a main, GitHub Actions comprueba que el proyecto sigue compilando correctamente.

- **Pull Request:** También se ejecuta cuando se crea o actualiza un Pull Request. Esto permite detectar problemas antes de integrar los cambios en main. El objetivo es evitar que código que no compila o cuyos tests fallan llegue a la rama principal.

2. `checkout` y configuración de Java

```yml
- name: Checkout
  uses: actions/checkout@v5
```

Descarga el contenido del repositorio en el runner de GitHub Actions para que los siguientes pasos puedan trabajar con el código.

```yml
- name: Set up Java
  uses: actions/setup-java@v5
  with:
    distribution: temurin
    java-version: "21"
    cache: maven
```

Configura JDK 21 Temurin y habilita la caché de Maven. La caché evita descargar nuevamente todas las dependencias Maven en cada ejecución. Después del primer build, las ejecuciones posteriores pueden ser considerablemente más rápidas.

3. El problema del `mvnw` en Linux
   El proyecto utiliza el Maven Wrapper.

En Windows se puede ejecutar:

```PowerShell
.\mvnw.cmd clean verify
```

Mientras que en Linux:

```Bash
./mvnw clean verify
```

> El primer workflow produjo un error: `exit code 126`. El problema era que `mvnw` no tenía el bit de ejecución necesario para ejecutarse como script en Linux. Esto puede ocurrir especialmente cuando el repositorio se trabaja desde Windows y posteriormente el código se ejecuta en un runner Linux. `chmod +x mvnw` → los ejecutables no guardan el bit de ejecución al commitear desde Windows.

4. `./mvnw -B clean verify`
   Hay tres elementos importantes:

- `./mvnw`: Utiliza el Maven Wrapper del propio proyecto en lugar de depender de una instalación global de Maven en el runner.

- `-B`: Significa _batch mode_. Maven ejecuta el proceso sin interacción de consola innecesaria, algo apropiado para CI.

- `clean verify`:
  - `clean` elimina los artefactos generados por builds anteriores.
  - `verify` ejecuta el ciclo de vida de Maven hasta la fase verify, incluyendo las fases anteriores:
  ```
    validate
    ↓
    compile
    ↓
    test
    ↓
    package
    ↓
    verify
  ```
  Por tanto, los **tests** también se ejecutan durante el CI. Si la compilación falla o un test falla, GitHub Actions marca el workflow como fallido y el Pull Request queda señalado.

## Bloque 1 — Identity Service

El **Identity Service** es el microservicio responsable de todo lo relacionado con la identidad y autenticación de los usuarios de la aplicación. Su responsabilidad principal es:

- Registrar usuarios.
- Autenticar usuarios mediante email y contraseña.
- Generar y validar JWT.
- Gestionar _access tokens_ y _refresh tokens_.
- Gestionar roles.
- Permitir autenticación mediante **_Google OAuth2_**.
- Exponer los datos del usuario autenticado.
- Gestionar logout y revocación de refresh tokens.
- Proporcionar un punto centralizado para las reglas de seguridad.

En la arquitectura del proyecto, este servicio escucha en el puerto `8081`, mientras que el Gateway utiliza el `8080`.

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

| Endpoint               | Descripción                                                         |
| ---------------------- | ------------------------------------------------------------------- |
| `POST /auth/register`  | Registra un usuario, devuelve `201` + tokens + cookie refresh       |
| `POST /auth/login`     | Autentica, devuelve `200` + tokens + cookie refresh                 |
| `POST /auth/refresh`   | Rota el refresh token (desde cookie **o** body) y emite un par nuevo|
| `POST /auth/logout`    | Revoca el refresh token, limpia la cookie, devuelve `204`           |

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

| Excepción                         | HTTP | Respuesta                                              |
| --------------------------------- | ---- | ------------------------------------------------------ |
| `EmailAlreadyExistsException`     | 409  | `{"error":"email_already_exists"}`                     |
| `InvalidRefreshTokenException`    | 401  | `{"error":"invalid_refresh_token"}`                    |
| `AuthenticationException`         | 401  | `{"error":"bad_credentials"}`                          |
| `MethodArgumentNotValidException` | 400  | `{"error":"validation_failed","fields":{...}}`         |

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

## Bloque 2 — API Gateway

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

### 2.2 — `application.yaml` del gateway

```yaml
spring:
  application:
    name: gateway

  config:
    import: "optional:file:.env[.properties]"   # mismo patrón de secretos que identity-service

  threads:
    virtual:
      enabled: true                              # hilos virtuales (Java 21)

  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: identity                        # nombre de la ruta
              uri: ${IDENTITY_SERVICE_URI:http://localhost:8081}
              predicates:
                - Path=/auth/**,/users/**

server:
  port: 8080

app:
  jwt:
    secret: ${APP_JWT_SECRET}                    # MISMO secret que identity-service
    issuer: booksocial-identity                  # MISMO issuer

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

### 2.4 — `JwtAuthFilter` del gateway y el patrón *strip-then-assert*

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

> **Patrón *strip-then-assert*** (quitar-y-afirmar): el gateway **elimina** cualquier header `X-User-*` que el cliente envíe (un cliente podría falsificarlos) y los **reemplaza** por los valores derivados del JWT ya verificado. Así, el cliente **no puede suplantar** una identidad, porque cualquier `X-User-Id` que ponga será descartado.

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
                .anyRequest().authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

- **`STATELESS`**: el gateway no mantiene sesión HTTP (a diferencia de identity-service, que la necesita para OAuth2). Cada petición es independiente.
- **`permitAll`** para `/auth/**` (login, register, refresh, logout — no pueden exigir token) y `/actuator/health` (healthcheck de Docker).
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

## Bloque 3 — Frontend Angular

### 3.1 — Creación del proyecto y estructura

El frontend se genera con la CLI de Angular 21:

```powershell
ng new frontend --style scss
```

Estructura de `frontend/src/app`:

```
core/
  guards/          # auth.guard, guest.guard
  interceptors/    # auth.interceptor
  models/          # auth.models, user.models, api-error.models
  services/        # auth.service, user.service
features/
  auth/            # login, register, oauth2-callback
  home/            # home (página principal)
shared/
  pipes/           # capitalize, initials
environments/      # configuraciones por entorno
```

#### `proxy.conf.json`: evitar CORS en desarrollo

Durante `ng serve`, el frontend está en `:4200` y el backend en `:8080`. Para no tener problemas de CORS, el proxy de desarrollo reenvía las llamadas al backend:

```json
{
  "/auth":  { "target": "http://localhost:8080", "changeOrigin": true, "secure": false },
  "/users": { "target": "http://localhost:8080", "changeOrigin": true, "secure": false }
}
```

El código Angular llama a `/auth/login` (ruta relativa) y el proxy lo redirige a `http://localhost:8080/auth/login`. En producción, el frontend se serviría bajo el mismo dominio del gateway y no haría falta proxy.

#### `googleAuthUrl`: la excepción del gateway

```ts
export const environment = {
  production: false,
  googleAuthUrl: 'http://localhost:8081/oauth2/authorization/google',
};
```

El login de Google **no pasa por el gateway**: apunta directamente a `:8081`. Es el comportamiento esperado documentado en el Bloque 1.6 (el gateway devuelve `401` en esa ruta).

### 3.2 — `AuthService`: estado de sesión en el cliente

El estado de la sesión se mantiene en memoria con `BehaviorSubject` (para que los componentes se suscriban a cambios):

```ts
private readonly accessTokenStore = new BehaviorSubject<string | null>(null);
private readonly authenticated = new BehaviorSubject<boolean>(false);
```

Métodos:

- `login(credentials)` / `register(payload)`: `POST` a `/auth/login` o `/auth/register` y aplican el token de la respuesta.
- `refresh()`: `POST /auth/refresh` con `withCredentials: true` (para enviar la cookie httpOnly).
- `logout()`: `POST /auth/logout` con cookie y limpia el estado.
- `applyOAuthToken(accessToken)`: usado por el callback de Google, que recibe el token en el fragmento de la URL.
- `restoreSession()`: intenta `refresh()` al arrancar la app; si falla, limpia la sesión.

> El access token vive en **memoria** (nunca en `localStorage`), lo que reduce el riesgo de robo por XSS. La sesión "larga" se restaura con la cookie httpOnly del refresh.

### 3.3 — Interceptor JWT y guardas de ruta

#### `AuthInterceptor` (`HttpInterceptorFn`)

Se registra con `provideHttpClient(withInterceptors([authInterceptor]))`. Hace tres cosas:

1. **No toca los endpoints de auth**: `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout` no llevan token.
2. **Adjunta el `Bearer` token** a cualquier otra petición.
3. **Manejo del 401 (refresh automático)**: si una petición protegida devuelve `401` y la sesión existe, llama a `auth.refresh()` y **reintenta** la petición original con el token nuevo:

```ts
return auth.refresh().pipe(
  catchError(() => { auth.clearSession(); return throwError(() => error); }),
  switchMap(() => next(req.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken}` } }))),
);
```

Si el refresh también falla, limpia la sesión y propaga el error (el usuario tendrá que volver a loguearse).

#### Guardas

- **`authGuard`**: si `!isAuthenticated` redirige a `/login` → protege rutas privadas (p.ej. `home`).
- **`guestGuard`**: si `isAuthenticated` redirige a `/home` → evita que un usuario logueado vea login/registro.

#### `app.config.ts`: restauración de sesión al arrancar

```ts
provideAppInitializer(() => inject(AuthService).restoreSession())
```

Antes de renderizar la app, Angular intenta renovar la sesión con el refresh token de la cookie. Así, al hacer F5 la sesión sobrevive.

### 3.4 — Páginas y flujo OAuth2

- **`login`**: formulario reactivo (`ReactiveFormsModule`) con validación de email/contraseña; mapea `error.status === 401` a "Invalid email or password."; botón "Continuar con Google" que hace `window.location.href = googleAuthUrl`.
- **`register`**: igual con los campos adicionales (`firstName`, `lastName`, `birthDate`).
- **`oauth2-callback`**: lee el **fragmento** de la URL (`#access_token=...` o `#error=access_denied`), limpia la URL con `history.replaceState`, aplica el token o muestra el error:

```ts
const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
const token = params.get('access_token');
if (token) { this.auth.applyOAuthToken(token); this.router.navigate(['/home']); }
```

- **`home`**: llama a `userService.me()` (`GET /users/me` vía gateway) para mostrar el perfil y ofrece logout.

```
Sin sesión:  /login → (Google) → :8081 → :4200/oauth2/callback#access_token=... → /home
Con sesión:  /home → interceptor añade Bearer → gateway valida → me() responde
```

## Bloque 4 — Contenerización y CI ampliado

### 4.1 — Dockerfiles multi-stage

Cada servicio tiene su propio `Dockerfile` con **dos etapas**:

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

¿Por qué multi-stage?

- **Stage de build**: una imagen de Maven compila el proyecto. `-pl identity-service -am` compila solo ese módulo (y sus dependencias), `-DskipTests` acelera el build (los tests ya corren en CI).
- **Stage de runtime**: se parte de una imagen Java mínima (`21-jre`, sin Maven, sin código fuente) y solo se copia el JAR. Resultado: imagen **pequeña** y sin herramientas de compilación.
- Se instala **`curl`** en la etapa runtime porque el healthcheck de Docker Compose lo usa.

> El gateway es idéntico, cambiando el módulo (`-pl gateway`) y el JAR copiado (`gateway-0.1.0-SNAPSHOT.jar`).

### 4.2 — `.dockerignore` raíz

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

Protege dos cosas: **tamaño** del contexto de build (no se copian `target/`, `node_modules/`, `.git/`) y, sobre todo, **secretos** — los `.env` de los módulos **nunca entran en la imagen**. Los secretos se inyectan en runtime con `env_file`.

### 4.3 — `docker-compose.yml` ampliado

A los tres servicios de infraestructura se añaden las aplicaciones:

```yaml
  identity-service:
    build:
      context: ..
      dockerfile: identity-service/Dockerfile
    container_name: booksocial-identity
    ports:
      - "8081:8081"
    env_file:
      - ../identity-service/.env          # secretos desde el host, fuera de la imagen
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/booksocial
    depends_on:
      postgres:
        condition: service_healthy        # espera a que Postgres esté realmente listo
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

- **`build.context: ..`**: el contexto es la raíz del monorepo (necesita el `pom.xml` padre y el wrapper), y el `dockerfile` se indica por ruta relativa.
- **`env_file` + `environment`**: los secretos (`APP_JWT_SECRET`, `GOOGLE_CLIENT_*`) llegan del `.env` del módulo; la **configuración no sensible** (URLs de otros contenedores) se sobreescribe con `environment`. Así el mismo jar funciona dentro de la red Docker usando los nombres de contenedor (`postgres`, `identity-service`) como host.
- **`depends_on ... service_healthy`**: el healthcheck de la Fase 0 (sección 0.4) se reutiliza: gateway no arranca hasta que identity-service responde a `/actuator/health`, y este no arranca hasta que Postgres está sano.

Arranque completo con un solo comando:

```powershell
docker compose -f infrastructure/docker-compose.yml up -d --build
```

### 4.4 — CI ampliado

El workflow `ci.yml` pasa de un solo job a dos:

**Job `build` (backend)** — el original, ampliado con:
- un **servicio PostgreSQL** (contenedor de soporte de GitHub Actions) que cubre la dependencia de identity-service en los tests;
- los **secrets** (`APP_JWT_SECRET`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_CLIENT_ID`) inyectados como variables de entorno.

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
      - run: npm ci          # instalación reproducible desde el lockfile
      - run: npm run build   # ng build
```

> Los dos jobs se ejecutan **en paralelo**; para que el pipeline esté verde deben compilar backend y frontend a la vez.

### 4.5 — Verificación E2E en Docker

Estado final esperado: **5 contenedores healthy** (postgres, mongodb, rabbitmq, identity-service, gateway).

Suite de verificación vía gateway (con `Invoke-RestMethod`, ver Bloque 5):

```
register → 201 TokenResponse (cookie refresh_token)
GET /users/me → 200 (identidad del usuario)
login → 200 (cookie con jti)
refresh → 200 (rotación, nueva cookie)
logout → 204 (cookie limpiada)
```

Y en navegador: `ng serve` en `:4200` → register → home → F5 (sesión restaurada por cookie) → logout → login → Google (ventana de incógnito).

## Bloque 5 — Cierre: errores resueltos y decisiones de diseño

### Errores encontrados (con solución directa)

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
   - Causa: `./mvnw` sin bit de ejecución en el runner Linux (se commitenó desde Windows).
   - Solución: `chmod +x mvnw` antes de ejecutar el wrapper en el workflow.

6. **`GET /oauth2/authorization/google` devuelve `401` a través del gateway**
   - No es un bug: el frontend llama a esa ruta **directamente contra `:8081`**, no vía gateway.

### Decisiones de diseño (resumen)

- **Secretos por módulo**: `.env` en cada servicio, excluidos de git y de las imágenes; en CI se inyectan como secrets.
- **JWT stateless + secret compartido**: el gateway valida los tokens sin consultar al identity-service.
- **Access corto (15 min) + refresh largo (7 días) rotativo**: el refresh viaja en cookie `httpOnly` + `SameSite=Lax`; el hash SHA-256 se guarda en BD (nunca el token en claro).
- **Roles calculados por edad**: `MINOR_USER` si la edad desde `birth_date` es menor de 18.
- **Patrón strip-then-assert**: el gateway elimina los `X-User-*` del cliente y los reemplaza por los derivados del JWT → los servicios downstream confían en ellos.
- **`ddl-auto: update` solo en desarrollo**; para producción se usarían migraciones (Flyway/Liquibase).
- **Parent POM como única fuente de versión** (Spring Boot 4.1.0 + BOM Spring Cloud 2025.1.2).
- **Contenedores con healthchecks y `depends_on: service_healthy`** para arranques ordenados y verificables.

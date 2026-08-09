## Bloque 0 — Cimientos (monorepo + infraestructura + CI)

### 0.1 — Instalación de JDK 21 + Maven + wrapper mvnw

Para este proyecto se ha optado por usar **_Java 21 (LTS)_** como _runtime_ y **_Maven_** como _build tool_. **Spring Boot 4.1.0** exige Java 21+, por eso se ha establecido que el equipo tenga la variable de entorno `JAVA_HOME` apuntando al JDK 21 (la terminal apunmta a esa versión de java).

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

  # Node/Angular
    node_modules/
    dist/

  # Entorno y secretos
    .env
    .env.*
    application-local.yml
    secrets/
    *.pem
    *.key

  # Infraestructura
    *.tfstate
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

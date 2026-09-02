# BookSocial

Plataforma web de gestión de lectura y comunidad literaria inspirada en aplicaciones como Goodreads.

Proyecto personal orientado principalmente al **aprendizaje práctico de arquitecturas de microservicios y tecnologías backend**. El objetivo es experimentar con diferentes tecnologías y patrones para entender cómo funcionan realmente en un proyecto completo, desde el desarrollo hasta el despliegue.

El proyecto se desarrolla principalmente con **Java y Spring Boot**, aunque también incorpora un frontend en Angular e infraestructura cloud y DevOps para tener una visión global del ciclo de vida de una aplicación distribuida.

Durante el desarrollo se utiliza **OpenCode como herramienta de apoyo**, principalmente para orientación, revisión de código y contraste de decisiones técnicas. La finalidad es utilizarlo como asistente durante el proceso de aprendizaje, manteniendo el foco en comprender e implementar las tecnologías por cuenta propia.

### Tech Stack

- **Backend:** Java 21 + Spring Boot
- **Frontend:** Angular 21
- **Arquitectura:** Microservices
- **Patrones:** CQRS + Event-Driven Architecture
- **Mensajería:** RabbitMQ
- **Bases de datos:** PostgreSQL + MongoDB
- **Cache & Rate Limiting:** Redis
- **APIs externas:** Google Books API + Open Library API
- **CI/CD:** GitHub Actions
- **Contenedores:** Docker
- **Infrastructure as Code:** Terraform
- **Cloud:** Google Cloud Platform (GCP)

### Integraciones

La aplicación utiliza **Google Books API** como fuente principal de información sobre libros y **Open Library** como fuente complementaria para los datos de autores, realizando un _cross-reference_ entre ambas fuentes para enriquecer la información.

La comunicación entre microservicios utiliza **RabbitMQ** mediante eventos, mientras que **CQRS** permite separar las operaciones de lectura y escritura. **Redis** se utiliza, entre otros casos, para implementar mecanismos de _rate limiting_.

### Objetivo del proyecto

El objetivo principal de BookSocial no es únicamente construir una aplicación funcional, sino **trastear con diferentes tecnologías y conceptos para comprobar cómo se comportan y cómo se integran en un proyecto real**.

El proyecto sirve como entorno de experimentación con:

- Arquitecturas de microservicios.
- Comunicación síncrona y asíncrona.
- CQRS y modelos de lectura/escritura separados.
- Arquitecturas orientadas a eventos.
- Diferentes tipos de bases de datos.
- Cache y _rate limiting_.
- Autenticación y autorización.
- Integración con APIs externas.
- Docker y despliegue de servicios.
- CI/CD con GitHub Actions.
- Infrastructure as Code con Terraform.
- Despliegue en Google Cloud.

El foco está especialmente puesto en el **backend y la arquitectura**, utilizando el frontend principalmente como medio para probar y consumir las funcionalidades desarrolladas.

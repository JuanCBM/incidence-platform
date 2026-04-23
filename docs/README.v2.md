# Incidence Platform — v2 (Kafka y Angular)

> Repositorio: [JuanCBM/incidence-platform](https://github.com/JuanCBM/incidence-platform)

---

## Contexto

En la versión anterior construisteis la base de la plataforma: un microservicio capaz de gestionar **usuarios** e **incidencias** a través de una API REST, con su base de datos correspondiente. Para levantar la infraestructura usabais **Podman** directamente, arrancando los contenedores de forma manual.

En esta segunda iteración vais a dar un paso más hacia una arquitectura real de microservicios. El objetivo es introducir **comunicación asíncrona** entre servicios mediante una cola de mensajería, añadir un nuevo microservicio independiente y construir una interfaz web que consuma todo lo que habéis desarrollado.

Además, en esta versión se espera que toda la infraestructura esté **orquestada**: nada de arrancar contenedores a mano. Todo el sistema debe poder levantarse de forma coordinada.

---

## Qué se pide

### 1. Orquestación con Docker Compose (o Podman Compose)

Hasta ahora arrancasteis los contenedores de forma individual con Podman. En esta versión necesitáis que **toda la infraestructura** (bases de datos, Kafka, y los propios microservicios) se pueda levantar de forma coordinada con un único comando.

Tendréis que decidir:

- Qué servicios forman parte de la infraestructura y cuáles son vuestras aplicaciones.
- En qué orden deben arrancar y si hay dependencias entre ellos.
- Cómo gestionáis la configuración de cada servicio (puertos, variables de entorno, etc.).

> Si seguís usando Podman, `podman-compose` es compatible con la sintaxis de Docker Compose. La decisión de cuál usar es vuestra, pero el fichero de orquestación debe estar en la raíz del repositorio.

---

### 2. Cola de mensajería con Apache Kafka

Cuando ocurra un evento relevante en el microservicio principal (por ejemplo, la creación o el cambio de estado de una incidencia), ese microservicio deberá **publicar un mensaje** en un topic de Kafka.

Tendréis que decidir:

- Qué información tiene sentido incluir en ese mensaje y por qué.
- En qué momento(s) del ciclo de vida de una incidencia tiene sentido publicar.
- Cómo estructuráis el mensaje: qué campos, qué formato, qué nombre le dais al topic.

No hay una única respuesta correcta. Lo importante es que podáis **justificar vuestras decisiones**.

---

### 3. Nuevo microservicio: Notificaciones

Cread un nuevo proyecto Spring Boot independiente dentro del repositorio. Este servicio actuará como **consumidor** de los mensajes que publica el microservicio principal.

Su responsabilidad es procesar esos mensajes y generar una notificación. Qué significa "notificar" en este contexto es algo que vosotros tenéis que definir: puede ser un log estructurado, un registro en base de datos, un email simulado, etc. Lo que no puede ser es que el mensaje se reciba y no ocurra nada observable.

Tened en cuenta que este servicio debe:

- Ser un proyecto Maven/Gradle separado, con su propio `pom.xml` o `build.gradle`.
- Arrancar de forma independiente al microservicio principal.
- No compartir código directamente con el otro servicio. Si necesitáis compartir algo, pensad cómo hacerlo de forma correcta.

---

### 4. Aplicación frontend con Angular

Desarrollad una aplicación Angular que sirva como interfaz de usuario para la plataforma. La aplicación debe cubrir los siguientes apartados:

#### 4.1 Gestión de usuarios
- Listado de usuarios existentes.
- Formulario para dar de alta un nuevo usuario.
- Vista de detalle de un usuario.

#### 4.2 Gestión de incidencias
- Listado de incidencias, con capacidad de filtrar o buscar.
- Formulario para crear una nueva incidencia asociada a un usuario.
- Vista de detalle de una incidencia con su estado actual.
- Posibilidad de actualizar el estado de una incidencia.

#### 4.3 Panel de actividad reciente
- Una sección que muestre la actividad reciente del sistema: incidencias creadas, cambios de estado, etc.
- Cómo obtenéis esta información y de qué servicio la pedís es una decisión vuestra.

> La aplicación debe comunicarse con los microservicios a través de sus APIs REST. Prestad atención al manejo de errores y a la experiencia de usuario básica.

---

## Estructura esperada del repositorio

No hay una estructura impuesta, pero como referencia razonable:

```
incidence-platform/
├── incidence-service/       # Microservicio principal (v1 ampliado)
├── notification-service/    # Nuevo microservicio consumidor de Kafka
├── incidence-frontend/      # Aplicación Angular
└── docker-compose.yml       # Orquestación de todos los servicios e infraestructura
```

---

## Criterios de evaluación

| Aspecto | Qué se valora |
|---|---|
| Orquestación | Que todo el sistema arranque con un único comando y que las dependencias entre servicios estén bien resueltas |
| Kafka | Que el productor y el consumidor funcionen correctamente y que el diseño del mensaje tenga sentido |
| Microservicio de notificaciones | Que sea independiente, que consuma bien los mensajes y que haga algo observable con ellos |
| Angular | Que la aplicación funcione, que consuma la API real y que la experiencia de uso sea coherente |
| Decisiones técnicas | Que seáis capaces de explicar por qué habéis tomado cada decisión |

---

## Entrega

- Todo el código debe estar en el repositorio **JuanCBM/incidence-platform**.
- El fichero de orquestación debe estar en la raíz y levantar toda la infraestructura necesaria.
- Incluid en cada microservicio un `README` breve explicando cómo arrancarlo en local y qué hace.
- Preparad una pequeña explicación (puede ser oral) de las decisiones que habéis tomado respecto al diseño del mensaje Kafka y al microservicio de notificaciones.

---

## Recursos de referencia

A continuación se listan algunos puntos de partida. La intención **no** es que los sigáis paso a paso, sino que os ayuden a orientaros cuando no sepáis por dónde empezar:

- [Spring for Apache Kafka – documentación oficial](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Angular – Tour of Heroes (guía oficial)](https://angular.io/tutorial)
- [Docker Compose – referencia](https://docs.docker.com/compose/)
- [Podman Compose – repositorio oficial](https://github.com/containers/podman-compose)

> **Nota:** Está bien consultar documentación y buscar ejemplos, pero el código que entreguéis tiene que ser vuestro y tiene que ser código que entendáis. Si no podéis explicar por qué habéis escrito algo, no lo entreguéis.

---
---

# 🔒 Preguntas de revisión de PR — USO INTERNO

> Estas preguntas son para usar durante la revisión de la Pull Request o en la defensa oral. No forman parte del enunciado del alumno.

---

### Sobre Kafka y el diseño del mensaje

- ¿Por qué habéis elegido publicar el mensaje en ese momento concreto del ciclo de vida de la incidencia y no en otro?
- ¿Qué campos tiene el mensaje que publicáis? ¿Hay algún campo que hayáis descartado deliberadamente? ¿Por qué?
- ¿Qué pasa si Kafka no está disponible cuando el microservicio principal intenta publicar un mensaje?
- ¿Cómo habéis nombrado el topic? ¿Hay algún criterio detrás de ese nombre o fue arbitrario?
- Si mañana necesitarais publicar un segundo tipo de evento (por ejemplo, cuando se elimina un usuario), ¿usaríais el mismo topic o crearíais uno nuevo? ¿Por qué?

---

### Sobre el microservicio de notificaciones

- ¿Qué ocurre exactamente cuando el servicio recibe un mensaje? Explicadlo paso a paso.
- ¿Qué pasa si el mensaje llega con un formato inesperado o le falta algún campo?
- ¿El servicio de notificaciones tiene base de datos propia? ¿Por qué sí o por qué no?
- ¿Qué ocurre si el servicio de notificaciones está caído en el momento en que se publica un mensaje en Kafka? ¿Se pierde ese mensaje?
- ¿Cómo sabéis que el mensaje se ha consumido correctamente? ¿Hay algún mecanismo de confirmación?

---

### Sobre la orquestación

- ¿En qué orden arrancan los servicios en vuestro fichero de orquestación? ¿Por qué ese orden?
- ¿Habéis usado `depends_on`? ¿Es suficiente para garantizar que Kafka está completamente listo antes de que arranque el productor?
- ¿Cómo gestionáis la configuración sensible (contraseñas, credenciales)? ¿Están hardcodeadas?
- Si alguien clona el repositorio desde cero, ¿qué pasos exactos necesita seguir para tener el sistema funcionando?

---

### Sobre Angular

- ¿Cómo gestionáis las llamadas HTTP al backend? ¿Hay algún servicio centralizado o cada componente llama directamente?
- ¿Qué ocurre en la interfaz si el backend devuelve un error? Mostradme un ejemplo.
- ¿Cómo habéis resuelto el problema de CORS entre Angular y los microservicios?
- El panel de actividad reciente, ¿de qué servicio obtiene los datos y por qué de ese?
- ¿Cómo se actualiza el panel de actividad: manualmente, con polling, o de otra forma? ¿Por qué elegisteis ese enfoque?

---

### Preguntas de diseño general

- ¿Qué ventaja tiene usar Kafka frente a hacer una llamada REST directa desde el microservicio principal al de notificaciones?
- ¿Qué ocurriría si el microservicio principal necesitase saber si la notificación se procesó correctamente? ¿Cómo lo resolveríais con lo que habéis montado?
- Si tuvierais que añadir un tercer microservicio que también consumiese los mismos eventos de Kafka, ¿qué cambiaríais en lo que ya tenéis?
- ¿Qué parte del sistema os parece más frágil o menos robusta? ¿Qué haríais para mejorarla con más tiempo?

# notification-service

**Microservicio consumidor de eventos de la plataforma de incidencias.**  
Escucha el topic de Apache Kafka `incidencias-eventos`, persiste cada evento en su propia base de datos PostgreSQL, envía una notificación por email al usuario afectado y expone una API REST para consultar el historial de actividad.

---

## Descripción general

```
com.empresa.notification
├── consumer        Listener de Kafka (@KafkaListener)
├── service         Interfaz + lógica de procesamiento y email
│   └── impl        Implementaciones concretas
├── controller      API REST de consulta de notificaciones
├── repository      Spring Data JPA
├── domain          Entidad Notificacion
├── dto             IncidenciaEventoDTO (desacoplado del microservicio principal)
└── config          CORS
```

Este servicio es **completamente independiente** de `incidence-service`. No comparte código ni base de datos con él. La única dependencia en tiempo de ejecución es el broker de Kafka y su propio PostgreSQL.

---

## Requisitos previos

| Herramienta   | Versión mínima | Notas                                |
|---------------|----------------|--------------------------------------|
| Java          | 17             | `JAVA_HOME` configurado              |
| Maven Wrapper | incluido       | Usar `./mvnw` en lugar de `mvn`      |
| PostgreSQL    | 14+            | Base de datos `notificaciones_db`    |
| Apache Kafka  | 3.x            | Broker en `localhost:9092`           |
| Cuenta SMTP   | —              | Mailtrap en desarrollo (ver config)  |

---

## Arranque en local (sin Compose)

**1. Crear la base de datos**

```sql
CREATE DATABASE notificaciones_db;
```

> El esquema de la tabla `notificaciones` se crea automáticamente al arrancar gracias a `spring.jpa.hibernate.ddl-auto=update`.

**2. Iniciar Kafka**

```bash
# Con Podman Compose desde la raíz del repositorio
python -m podman_compose up -d zookeeper kafka
```

**3. Compilar y arrancar**

```bash
cd notification-service
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8081`.

**4. Verificar**

```bash
# Actividad reciente del sistema (últimos 50 eventos)
curl http://localhost:8081/notificaciones

# Historial de una incidencia concreta
curl http://localhost:8081/notificaciones/1
```

---

## Arranque con Compose (recomendado)

```bash
# Desde la raíz del repositorio
python -m podman_compose up --build
```

El servicio espera a que Kafka esté disponible según la configuración de `depends_on` en el `docker-compose.yml`.

---

## Configuración (`application.properties`)

| Propiedad                                | Valor por defecto                    | Descripción                          |
|------------------------------------------|--------------------------------------|--------------------------------------|
| `server.port`                            | `8081`                               | Puerto HTTP del servicio             |
| `spring.datasource.url`                  | `jdbc:postgresql://localhost:5432/notificaciones_db` | URL de PostgreSQL |
| `spring.kafka.bootstrap-servers`         | `localhost:9092`                     | Broker de Kafka                      |
| `spring.kafka.consumer.group-id`         | `notification-group`                 | Consumer group de Kafka              |
| `spring.kafka.consumer.auto-offset-reset`| `earliest`                           | Consume desde el inicio si no hay offset |
| `spring.mail.host`                       | `sandbox.smtp.mailtrap.io`           | Servidor SMTP (Mailtrap en dev)      |
| `spring.mail.port`                       | `2525`                               | Puerto SMTP                          |
| `notificacion.mail.from`                 | `noreply@incidence-platform.com`     | Remitente de los emails              |

---

## Flujo de procesamiento

Cada mensaje que llega al topic `incidencias-eventos` pasa por las siguientes etapas:

```
Kafka topic (incidencias-eventos)
        │
        ▼
IncidenciaEventoConsumer.consumir(String mensaje)
  │  Deserializa JSON → IncidenciaEventoDTO
  │
  ▼
NotificacionService.procesarEvento(dto, mensajeOriginal)
  │  1. Persiste Notificacion en PostgreSQL
  │  2. Llama a EmailService.enviarNotificacion(dto)
  │
  ▼
EmailService
     Compone asunto y cuerpo según tipo de evento
     Envía email vía JavaMailSender al emailUsuario del DTO
```

Si el mensaje llega con formato incorrecto o le falta un campo, el error se registra en el log y el mensaje se descarta sin interrumpir el consumidor.

---

## Modelo de dominio

### `Notificacion`

| Campo            | Tipo            | Descripción                                       |
|------------------|-----------------|---------------------------------------------------|
| `id`             | `Long`          | PK autogenerada                                   |
| `incidenciaId`   | `Long`          | ID de la incidencia en `incidence-service`        |
| `evento`         | `String`        | Tipo de evento: `INCIDENCIA_CREADA`, `ESTADO_CAMBIADO` |
| `mensaje`        | `String` (TEXT) | Payload JSON original recibido de Kafka           |
| `fechaRecepcion` | `LocalDateTime` | Momento en que se procesó el evento               |

### `IncidenciaEventoDTO`

| Campo          | Tipo     | Descripción                                |
|----------------|----------|--------------------------------------------|
| `incidenciaId` | `Long`   | ID de la incidencia que originó el evento  |
| `evento`       | `Enum`   | `INCIDENCIA_CREADA` o `ESTADO_CAMBIADO`    |
| `titulo`       | `String` | Título de la incidencia                    |
| `descripcion`  | `String` | Descripción de la incidencia               |
| `estado`       | `Enum`   | Estado en el momento del evento            |
| `prioridad`    | `Enum`   | Prioridad de la incidencia                 |
| `emailUsuario` | `String` | Email al que se envía la notificación      |

---

## API REST

### Base URL

```
http://localhost:8081/notificaciones
```

### Endpoints

| Método | Ruta                           | Descripción                                                  | Códigos de respuesta |
|--------|--------------------------------|--------------------------------------------------------------|----------------------|
| `GET`  | `/notificaciones`              | Devuelve los 50 eventos más recientes de todo el sistema, ordenados de más nuevo a más antiguo. Usado por el panel de actividad reciente de Angular. | `200`, `204` |
| `GET`  | `/notificaciones/{incidenciaId}` | Devuelve el historial completo de eventos de una incidencia concreta. | `200`, `204` |
| `GET`  | `/notificaciones/{incidenciaId}/log` | Genera y descarga un fichero `.txt` con el historial de la incidencia. | `200`, `204` |

**Ejemplo de respuesta — `GET /notificaciones`**:

```json
[
  {
    "id": 7,
    "incidenciaId": 3,
    "evento": "ESTADO_CAMBIADO",
    "mensaje": "{\"incidenciaId\":3,\"evento\":\"ESTADO_CAMBIADO\",...}",
    "fechaRecepcion": "2025-04-21T10:45:00"
  },
  {
    "id": 6,
    "incidenciaId": 3,
    "evento": "INCIDENCIA_CREADA",
    "mensaje": "{\"incidenciaId\":3,\"evento\":\"INCIDENCIA_CREADA\",...}",
    "fechaRecepcion": "2025-04-21T09:30:00"
  }
]
```

---

## Ejecución de tests

```bash
./mvnw test
```

> El servicio no dispone actualmente de tests de integración con Kafka. Las pruebas manuales del flujo completo se realizan levantando toda la infraestructura con Compose y creando o actualizando incidencias desde el frontend o mediante `curl`.

---

## Decisiones técnicas

### Por qué este servicio tiene su propia base de datos

Cada microservicio debe ser autónomo. Si `notification-service` compartiese la base de datos con `incidence-service`, cualquier cambio de esquema en uno rompería al otro. La independencia de datos es uno de los principios fundamentales de la arquitectura de microservicios.

### Por qué el payload Kafka almacena el JSON completo como `mensaje`

El campo `mensaje` guarda el JSON original tal como llegó de Kafka. Esto permite reconstruir exactamente qué información estaba disponible en el momento del evento, independientemente de los cambios futuros en el modelo de dominio. Es un registro inmutable del hecho.

### Por qué se usa `auto-offset-reset=earliest`

Si el servicio de notificaciones se reinicia (por ejemplo, durante un redespliegue), consumirá desde el último offset confirmado, no desde el principio. `earliest` solo aplica cuando no existe ningún offset previo, garantizando que no se pierden eventos de arranque en el primer despliegue.

### Por qué el email del usuario llega en el evento Kafka y no se consulta desde este servicio

`notification-service` no tiene acceso a la base de datos de usuarios (principio de autonomía). Si necesitase el email, tendría que hacer una llamada REST a `incidence-service`, creando acoplamiento síncrono entre servicios. Incluir el email en el evento Kafka es más robusto y no introduce dependencias en tiempo de ejecución.

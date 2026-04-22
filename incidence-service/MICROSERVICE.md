# incidence-service

**Módulo principal de la plataforma de gestión de incidencias.**  
Expone la API REST que gestiona usuarios e incidencias, publica eventos en Apache Kafka y persiste todos los datos en PostgreSQL mediante Flyway.

---

## Descripción general

```
com.empresa.incidencias
├── controller          Implementación de los contratos OpenAPI
├── service             Interfaces + lógica de negocio (transaccional)
│   └── impl            Implementaciones concretas
├── repository          Spring Data JPA (acceso a datos)
├── domain
│   ├── entity          Entidades JPA (Usuario, Incidencia, SugerenciaIA)
│   └── dto             DTOs de filtrado y Kafka
└── infrastructure      Kafka producer, Copilot CLI client, CORS config
```

La arquitectura sigue el patrón de capas estricto: **controller → service → repository → entidad**. Ninguna capa accede directamente a la capa que no le corresponde.

---

## Requisitos previos

| Herramienta   | Versión mínima | Notas                              |
|---------------|----------------|------------------------------------|
| Java          | 17             | `JAVA_HOME` configurado            |
| Maven Wrapper | incluido       | Usar `./mvnw` en lugar de `mvn`    |
| PostgreSQL    | 14+            | Base de datos `incidencias`        |
| Apache Kafka  | 3.x            | Broker en `localhost:9092`         |
| Podman/Docker | cualquiera     | Necesario para levantar con Compose|

---

## Arranque en local (sin Compose)

**1. Crear la base de datos**

```sql
CREATE DATABASE incidencias;
CREATE USER incidencias WITH PASSWORD 'incidencias';
GRANT ALL PRIVILEGES ON DATABASE incidencias TO incidencias;
```

**2. Iniciar Kafka**

```bash
# Con Podman Compose desde la raíz del repositorio
python -m podman_compose up -d zookeeper kafka
```

**3. Compilar y arrancar**

```bash
cd incidence-service
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080`.  
Flyway ejecuta automáticamente las migraciones al arrancar.

**4. Verificar**

```bash
curl http://localhost:8080/usuarios
curl http://localhost:8080/incidencias
```

---

## Arranque con Compose (recomendado)

```bash
# Desde la raíz del repositorio
python -m podman_compose up --build
```

Todos los servicios (PostgreSQL, Kafka, Zookeeper, `incidence-service`, `notification-service`) arrancan coordinados.

---

## Configuración (`application.properties`)

| Propiedad                              | Valor por defecto              | Descripción                        |
|----------------------------------------|--------------------------------|------------------------------------|
| `spring.datasource.url`                | `jdbc:postgresql://localhost:5432/incidencias` | URL de conexión a PostgreSQL |
| `spring.datasource.username`           | `incidencias`                  | Usuario de la base de datos        |
| `spring.datasource.password`           | `incidencias`                  | Contraseña de la base de datos     |
| `spring.kafka.bootstrap-servers`       | `localhost:9092`               | Broker de Kafka                    |
| `spring.flyway.locations`              | `schema,data`                  | Rutas de migraciones Flyway        |

---

## API REST

El contrato completo está definido en formato **OpenAPI 3.0** y disponible en Swagger UI una vez arrancado el servicio:

```
http://localhost:8080/swagger-ui.html
```

### Recursos principales

#### Usuarios — `/usuarios`

| Método | Ruta              | Descripción                        | Códigos de respuesta |
|--------|-------------------|------------------------------------|----------------------|
| `GET`  | `/usuarios`       | Lista todos los usuarios           | `200`                |
| `GET`  | `/usuarios/{id}`  | Obtiene un usuario por ID          | `200`, `404`         |
| `POST` | `/usuarios`       | Crea un nuevo usuario              | `201`, `400`         |
| `PUT`  | `/usuarios/{id}`  | Actualiza un usuario existente     | `200`, `404`         |
| `DELETE` | `/usuarios/{id}` | Elimina un usuario               | `204`, `404`         |

#### Incidencias — `/incidencias`

| Método | Ruta                              | Descripción                                    | Códigos de respuesta |
|--------|-----------------------------------|------------------------------------------------|----------------------|
| `GET`  | `/incidencias`                    | Lista todas las incidencias                    | `200`                |
| `GET`  | `/incidencias/{id}`               | Obtiene una incidencia por ID                  | `200`, `404`         |
| `POST` | `/incidencias`                    | Crea una nueva incidencia                      | `201`, `404`         |
| `PUT`  | `/incidencias/{id}`               | Actualiza una incidencia (incluye cambio de estado) | `200`, `404`    |
| `DELETE` | `/incidencias/{id}`             | Elimina una incidencia                         | `204`, `404`         |
| `GET`  | `/incidencias/buscar`             | Búsqueda con filtros opcionales y paginación   | `200`                |
| `POST` | `/incidencias/{id}/sugerencia`    | Genera una sugerencia IA mediante Copilot CLI  | `201`, `404`, `503`  |

**Parámetros de búsqueda** (`/incidencias/buscar`):

| Parámetro    | Tipo              | Descripción                          |
|--------------|-------------------|--------------------------------------|
| `usuarioId`  | `Long` (opcional) | Filtra por usuario asignado          |
| `estado`     | `String` (opcional) | `ABIERTA`, `EN_PROGRESO`, `CERRADA` |
| `prioridad`  | `String` (opcional) | `BAJA`, `MEDIA`, `ALTA`, `CRITICA`  |
| `page`       | `int` (opcional)  | Número de página (por defecto `0`)   |
| `size`       | `int` (opcional)  | Tamaño de página (por defecto `20`)  |
| `sort`       | `String` (opcional) | Campo de ordenación                 |

---

## Modelo de dominio

### `Usuario`

| Campo       | Tipo        | Restricciones              |
|-------------|-------------|----------------------------|
| `id`        | `Long`      | PK, autogenerado           |
| `nombre`    | `String`    | Obligatorio                |
| `email`     | `String`    | Obligatorio, único         |
| `rol`       | `Rol` (enum)| `ADMIN`, `SOPORTE`, `USUARIO` |
| `fechaAlta` | `LocalDate` | Asignada automáticamente   |

### `Incidencia`

| Campo               | Tipo                    | Restricciones                              |
|---------------------|-------------------------|--------------------------------------------|
| `id`                | `Long`                  | PK, autogenerado                           |
| `titulo`            | `String`                | Obligatorio                                |
| `descripcion`       | `String`                | Obligatorio                                |
| `estado`            | `EstadoIncidencia` (enum) | `ABIERTA`, `EN_PROGRESO`, `CERRADA`      |
| `prioridad`         | `Prioridad` (enum)      | `BAJA`, `MEDIA`, `ALTA`, `CRITICA`         |
| `fechaCreacion`     | `LocalDateTime`         | Asignada automáticamente                   |
| `fechaActualizacion`| `LocalDateTime`         | Actualizada en cada PUT                    |
| `usuarioAsignado`   | `Usuario`               | FK obligatoria a `usuarios`                |

---

## Eventos Kafka

Cuando se crea o actualiza una incidencia, el servicio publica un mensaje en el topic **`incidencias-eventos`**.

**Estructura del mensaje (JSON)**:

```json
{
  "incidenciaId": 12,
  "evento": "INCIDENCIA_CREADA",
  "titulo": "Error en login",
  "descripcion": "El usuario no puede iniciar sesión",
  "estado": "ABIERTA",
  "prioridad": "ALTA",
  "emailUsuario": "usuario@empresa.com"
}
```

| Campo          | Valores posibles                           |
|----------------|--------------------------------------------|
| `evento`       | `INCIDENCIA_CREADA`, `ESTADO_CAMBIADO`     |
| `estado`       | `ABIERTA`, `EN_PROGRESO`, `CERRADA`        |
| `prioridad`    | `BAJA`, `MEDIA`, `ALTA`, `CRITICA`         |

---

## Migraciones Flyway

Las migraciones se organizan en dos carpetas separadas:

```
src/main/resources/db/migration/
├── schema/     Estructura de tablas, índices y restricciones
└── data/       Datos base e iniciales del sistema
```

| Versión | Carpeta  | Descripción                          |
|---------|----------|--------------------------------------|
| V1      | schema   | Creación de tablas `usuarios` e `incidencias` |
| V2      | schema   | Tabla `sugerencia_ia`                |
| V3–V5   | data     | Usuarios y datos de prueba iniciales |
| V6      | data     | Usuario `miguel.mencar22@gmail.com`  |

---

## Ejecución de tests

```bash
./mvnw test
```

Los tests incluyen:

- **Tests unitarios de servicio** — con Mockito, sin acceso a base de datos
- **Tests de repositorio** — con `@DataJpaTest` y base de datos en memoria
- **Tests parametrizados** — validación de múltiples escenarios por caso de uso

---

## Decisiones técnicas

### Por qué se usa interfaz en la capa de servicio si solo hay una implementación

El enunciado lo exige, y además respeta el principio de **inversión de dependencias (DIP)**. Si en el futuro se necesita una implementación alternativa (por ejemplo, en tests de integración), el controlador no debe cambiar. El coste es mínimo y el beneficio en mantenibilidad es real.

### Por qué los DTOs no exponen la entidad JPA directamente

Las entidades JPA tienen anotaciones de ciclo de vida (`@PrePersist`, relaciones lazy) que pueden provocar comportamientos inesperados durante la serialización JSON. El DTO es un contrato estable e independiente del modelo de persistencia.

### Por qué se publican dos tipos de evento en Kafka y no uno genérico

`INCIDENCIA_CREADA` y `ESTADO_CAMBIADO` tienen semánticas distintas y el consumidor puede necesitar reaccionar de forma diferente ante cada uno. Un único evento `INCIDENCIA_ACTUALIZADA` obligaría al consumidor a inspeccionar el payload para deducir qué ocurrió, lo que rompe el principio de responsabilidad única.

### Por qué `CopilotClient` está en la capa de infraestructura

El servicio de negocio no debe saber que la respuesta viene de un proceso del sistema. Encapsular `ProcessBuilder` en `CopilotClient` permite reemplazarlo o mockearlo en tests sin tocar la lógica de `SugerenciaIAServiceImpl`.

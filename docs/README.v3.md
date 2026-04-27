# Incidence Platform — v3

> Repositorio: [JuanCBM/incidence-platform](https://github.com/JuanCBM/incidence-platform)

---

## Contexto

En la versión anterior añadisteis comunicación asíncrona mediante Kafka, un microservicio de notificaciones independiente y una interfaz Angular para interactuar con la plataforma.

En esta tercera iteración vais a incorporar una nueva pieza habitual en sistemas empresariales: el **procesamiento batch**. La idea es introducir un proceso que, cuando se lance, sea capaz de operar sobre un volumen significativo de datos de forma controlada y trazable.

---

## Qué se pide

### 1. Nuevo microservicio: Batch de cierre de incidencias

Cread un nuevo proyecto Spring Boot independiente que utilice **Spring Batch** para ejecutar el siguiente proceso:

> Cerrar automáticamente todas las incidencias que lleven más de un tiempo determinado sin resolverse.

Qué significa "sin resolverse" y cuánto tiempo debe haber pasado para considerar una incidencia candidata al cierre son decisiones que tendréis que tomar y justificar. El umbral de tiempo debe ser configurable, no hardcodeado.

El proceso debe:

- Leer las incidencias candidatas desde la base de datos.
- Procesarlas en **chunks**: no todas de golpe. Tendréis que decidir cuántas incidencias procesáis por chunk y razonar ese número.
- Actualizar el estado de cada incidencia candidata a cerrada.
- Dejar trazabilidad de lo que ha ocurrido: qué incidencias se han cerrado, cuántas, y si ha habido algún error durante el proceso.

> **Importante:** el batch arranca **bajo demanda**, no de forma programada. Tiene que haber algún mecanismo para lanzarlo manualmente cuando se considere oportuno. Cómo lo exponéis (un endpoint, un comando, etc.) es una decisión vuestra.

---

### 2. Integración con el resto del sistema

El batch no vive aislado. Pensad cómo encaja con lo que ya tenéis:

- Cuando el batch cierra una incidencia, ¿debería publicarse algún evento en Kafka? ¿Por qué sí o por qué no?
- ¿El microservicio de notificaciones debería enterarse de los cierres masivos? ¿De la misma forma que con los cierres individuales?
- ¿Hay algo en la interfaz Angular que tenga sentido actualizar o añadir para dar visibilidad a este proceso?

No es obligatorio implementar todo esto, pero sí es obligatorio que hayáis pensado en ello y podáis explicar las decisiones que habéis tomado.

---

### 3. Datos de prueba

Para poder probar el batch necesitáis incidencias en la base de datos. Cread un mecanismo para generar al menos **100 incidencias** de prueba con fechas variadas, de forma que el batch tenga datos reales sobre los que operar cuando se lance.

Cómo lo hacéis (un script SQL, un endpoint de carga, un fichero de datos iniciales…) es una decisión vuestra.

---

## Estructura esperada del repositorio

```
incidence-platform/
├── incidence-service/       # Microservicio principal
├── notification-service/    # Microservicio consumidor de Kafka
├── batch-service/           # Nuevo microservicio Spring Batch
├── incidence-frontend/      # Aplicación Angular
└── docker-compose.yml       # Orquestación de todos los servicios e infraestructura
```

---

## Criterios de evaluación

| Aspecto | Qué se valora |
|---|---|
| Spring Batch | Que el job esté bien estructurado (reader, processor, writer) y que el chunking funcione correctamente |
| Lanzamiento bajo demanda | Que haya un mecanismo claro para lanzar el batch y que funcione |
| Trazabilidad | Que quede registro de lo que ha hecho el batch y de los posibles errores |
| Integración | Que hayáis pensado cómo encaja el batch con Kafka y las notificaciones |
| Datos de prueba | Que el sistema tenga datos suficientes para poder probar el batch de forma significativa |
| Decisiones técnicas | Que podáis justificar el tamaño del chunk, el criterio de antigüedad y el mecanismo de lanzamiento |

---

## Entrega

- Todo el código debe estar en el repositorio **JuanCBM/incidence-platform**.
- El `docker-compose.yml` debe incluir el nuevo servicio batch.
- El `batch-service` debe tener su propio `README` explicando cómo lanzarlo y qué hace exactamente.
- Preparad una explicación de las decisiones de diseño: tamaño de chunk, criterio de antigüedad, mecanismo de lanzamiento, e integración con el resto del sistema.

---

## Recursos de referencia

- [Spring Batch – documentación oficial](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [Spring Batch – guía de introducción](https://spring.io/guides/gs/batch-processing/)

> **Nota:** Spring Batch tiene bastante configuración y conceptos propios (Job, Step, Reader, Processor, Writer, JobLauncher…). Antes de escribir código, dedicad tiempo a entender qué hace cada pieza. Un batch mal estructurado que funciona es peor que uno bien estructurado que aún no compila.

---
---

# 🔒 Preguntas de revisión de PR — USO INTERNO

> Para usar durante la revisión de la Pull Request o en la defensa oral.

---

### Sobre Spring Batch y la estructura del job

- Explicadme la estructura de vuestro job: ¿cuántos steps tiene y qué hace cada uno?
- ¿Qué hace exactamente el Reader, el Processor y el Writer en vuestro caso?
- ¿Por qué habéis elegido ese tamaño de chunk? ¿Qué pasaría si lo pusierais a 1? ¿Y a 10.000?
- ¿Qué ocurre si el batch falla a mitad del proceso, por ejemplo en el chunk 7 de 10? ¿Las incidencias de ese chunk quedan cerradas, abiertas, o en un estado inconsistente?
- ¿Spring Batch guarda algo en base de datos por defecto? ¿Sabéis para qué sirve eso?

---

### Sobre el criterio de antigüedad y el lanzamiento

- ¿Qué criterio exacto habéis usado para decidir qué incidencias son candidatas al cierre? ¿Es la fecha de creación, de última modificación, o alguna otra?
- ¿Dónde está configurado el umbral de antigüedad? ¿Cómo lo cambiaría alguien sin tocar el código?
- ¿Cómo se lanza el batch? Si yo tengo el sistema corriendo ahora mismo, ¿qué hago exactamente para ejecutarlo?
- ¿Se puede lanzar el batch dos veces seguidas? ¿Qué pasaría con las incidencias que ya cerró la primera vez?

---

### Sobre la integración con Kafka y notificaciones

- Cuando el batch cierra una incidencia, ¿se publica algún evento en Kafka? ¿Por qué habéis tomado esa decisión?
- Si el batch cierra 80 incidencias, ¿se envían 80 notificaciones? ¿Tiene sentido eso? ¿Cómo lo habéis resuelto?
- ¿Hay alguna diferencia entre un cierre manual desde la API y un cierre masivo por el batch? ¿Debería haberla?

---

### Sobre los datos de prueba

- ¿Cómo habéis generado las 100 incidencias de prueba? ¿Ese mecanismo funcionaría también en producción o es solo para desarrollo?
- ¿Las fechas de las incidencias están distribuidas de forma que el batch tenga algo interesante que hacer? ¿Hay incidencias que cumplan el criterio y otras que no?

---

### Preguntas de diseño general

- ¿Qué ventaja tiene Spring Batch frente a hacer un simple bucle en un endpoint REST que recorra las incidencias y las cierre una a una?
- ¿Qué pasaría si hubiera 500.000 incidencias candidatas en vez de 100? ¿Vuestro diseño aguantaría?
- Si en el futuro este batch tuviera que ejecutarse todas las noches de forma automática, ¿qué cambiaríais de lo que habéis hecho?
- ¿Hay alguna parte del batch que os haya resultado especialmente difícil de entender o implementar?
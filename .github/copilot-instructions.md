# Copilot Instructions — Tarea 5: Arquitectura MCP

## Contexto del proyecto

Este proyecto implementa una arquitectura basada en **Model Context Protocol (MCP)** para exponer información técnica de dos microservicios backend (`incidence-service` y `notification-service`) y hacerla consultable desde **GitHub Copilot** o **Claude Desktop** mediante lenguaje natural.

### Microservicios existentes

| Microservicio | Descripción | Estado |
|---|---|---|
| `incidence-service` | API REST de gestión de incidencias y usuarios. Documentada con `openapi.yaml`. | ✅ Desarrollado |
| `notification-service` | Consumidor Kafka que procesa eventos de incidencias. Sin API REST actualmente. | ✅ Desarrollado — API REST pendiente |

---

## Qué es MCP y cómo funciona aquí

MCP es un protocolo abierto y estándar de la industria (adoptado por Anthropic, OpenAI, Google, Microsoft) que permite a una IA conectarse con herramientas y datos externos de forma estandarizada. En este proyecto se usa como capa que convierte el código e infraestructura del proyecto en algo **consultable mediante lenguaje natural**.

La inteligencia la pone el modelo de IA (Copilot/Claude). Los MCPs son programas simples que solo ejecutan acciones concretas: leer un archivo, llamar a una API. Quien decide qué acción usar es el modelo, leyendo las descripciones de las `tools` declaradas en cada MCP.

---

## Arquitectura objetivo

```
GitHub Copilot / Claude Desktop
        │
        ▼
MCP Orquestador          ← punto de entrada único, nunca se modifica al añadir MCPs
        │
        ├──► MCP Incidencias    → lee openapi.yaml del incidence-service
        ├──► MCP Notificaciones → llama a GET /notificaciones del notification-service
        └──► MCP Build          → lee pom.xml de ambos microservicios
```

---

## Componentes a implementar

### 1. MCP Incidencias
- **Fuente de datos:** `openapi.yaml` del `incidence-service`
- **Tools que expone:**
  - `listar_endpoints` — devuelve todos los endpoints disponibles con métodos HTTP
  - `obtener_schema_endpoint` — devuelve el schema de request/response de un endpoint concreto
  - `listar_codigos_error` — devuelve los códigos de error definidos
- **Descripción de tools:** en lenguaje natural y claro. Copilot las lee para decidir cuándo usarlas.

### 2. MCP Notificaciones
- **Fuente de datos:** `GET /notificaciones` del `notification-service` (endpoint a crear)
- **Tools que expone:**
  - `obtener_notificaciones` — devuelve la lista de notificaciones procesadas
  - `obtener_estado_servicio` — devuelve estado del servicio y cantidad procesada
- **Dependencia:** requiere implementar primero el endpoint REST en `notification-service`

### 3. MCP Build
- **Fuente de datos:** `pom.xml` de ambos microservicios
- **Tools que expone:**
  - `obtener_version_java` — versión de Java configurada
  - `obtener_version_spring` — versión de Spring Boot
  - `listar_dependencias` — dependencias principales (JPA, Kafka, Flyway, etc.)
  - `listar_plugins_maven` — plugins configurados

### 4. MCP Orquestador
- Punto de entrada único para Copilot/Claude Desktop
- Carga los MCPs especializados **una sola vez al arrancar** (no en cada consulta)
- Delega a cada MCP según la descripción de sus tools
- **No contiene lógica de negocio**, solo enrutamiento
- Registrado en `claude_desktop_config.json` o configuración de Copilot

---

## Principios de diseño clave

### Escalabilidad por convención de carpeta
Los MCPs especializados se colocan en una carpeta común. El Orquestador la escanea al arrancar y carga lo que encuentre. Añadir un nuevo MCP no requiere modificar el Orquestador.

```
mcps/
  ├── mcp-incidencias.js
  ├── mcp-notificaciones.js
  └── mcp-build.js         ← añadir aquí = disponible automáticamente al reiniciar

orquestador.js             ← nunca se toca
```

### Carga en arranque, no en cada llamada
El Orquestador carga todos los MCPs en memoria una sola vez al iniciar. Las consultas posteriores van directas a memoria. Actualizar o añadir un MCP requiere reiniciar el Orquestador (patrón estándar, igual que cualquier microservicio).

### Descripciones de tools como contrato
Las descripciones de cada tool son lo que el modelo de IA lee para decidir cuándo usarlas. Deben ser claras y en lenguaje natural. Esto es esencialmente prompt engineering aplicado a herramientas.

```typescript
server.tool(
  "obtener_version_spring",
  "Devuelve la versión de Spring Boot configurada en los microservicios del proyecto",
  {},
  async () => { /* lógica */ }
);
```

### Transporte stdio
Todos los MCPs se implementan como procesos stdio (compatibles con Claude Desktop y GitHub Copilot). El host lanza el proceso y se comunica por stdin/stdout.

---

## Estructura de carpetas sugerida

```
/
├── mcps/
│   ├── mcp-incidencias.js
│   ├── mcp-notificaciones.js
│   └── mcp-build.js
├── orquestador.js
├── incidence-service/
│   └── openapi.yaml
├── notification-service/
│   └── src/...              ← añadir endpoint GET /notificaciones aquí
└── claude_desktop_config.json
```

---

## Configuración en Claude Desktop / Copilot

Solo se registra el Orquestador. Los MCPs especializados son internos.

```json
{
  "mcpServers": {
    "proyecto-backend": {
      "command": "node",
      "args": ["./orquestador.js"]
    }
  }
}
```

---

## Prompts de prueba y MCP esperado

| Prompt | MCP que debe responder |
|---|---|
| ¿Qué endpoints expone el incidence-service? | MCP Incidencias |
| ¿Cuántas notificaciones se han procesado? | MCP Notificaciones |
| ¿Qué versión de Spring Boot usan los microservicios? | MCP Build |
| ¿Qué schema tiene el body para crear una incidencia? | MCP Incidencias |
| ¿Qué dependencias de Kafka están configuradas? | MCP Build |

---

## Extensión pendiente: API REST en notification-service

Añadir al menos un endpoint observable:

```
GET /notificaciones
```

Debe devolver la lista de notificaciones procesadas (registros en BD, logs estructurados, etc.). Este endpoint será la fuente de datos del MCP Notificaciones.

---

## Compatibilidad con otras IAs

MCP es un estándar abierto gestionado por la Linux Foundation (Agentic AI Foundation). Los MCPs implementados en este proyecto son compatibles sin cambios con:

- GitHub Copilot (soporte nativo en VS Code, JetBrains, CLI)
- Claude Desktop
- ChatGPT Desktop
- Gemini / Google AI Studio
- Cursor, Windsurf y otros clientes MCP

---

## Criterios de éxito

- El Orquestador selecciona el MCP correcto en todos los prompts de prueba
- El `notification-service` expone al menos un endpoint REST funcional
- La configuración es reproducible: otro desarrollador puede levantar los MCPs siguiendo la documentación
- Añadir un nuevo MCP no requiere modificar ningún componente existente

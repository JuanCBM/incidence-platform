# MCP Orchestrator — Incidence Platform

## ¿Qué es MCP?

**Model Context Protocol (MCP)** es un protocolo abierto que permite a ChatGPT, Claude, GitHub Copilot y otros LLMs conectarse con herramientas y datos externos de forma estándar.

En este proyecto, los MCPs permiten consultar información técnica de los microservicios mediante **lenguaje natural** desde GitHub Copilot o Claude Desktop.

## Arquitectura

```
GitHub Copilot / Claude Desktop
            │
            ▼
┌──────────────────────────────┐
│   Orquestador (stdio)        │ ← Punto de entrada único
│   orquestador.js             │    Nunca se modifica
└──────────────────────────────┘
            │
    ┌───────┼───────┐
    ▼       ▼       ▼
  Incidencias  Notificaciones  Build
  (parseaᵣ  (consulta API    (parsea
   openapi)   REST)          pom.xml)
```

### Componentes

| Componente | Archivo | Responsabilidad | Fuente de datos |
|---|---|---|---|
| **MCP Incidencias** | `mcp-incidencias.js` | Expone endpoints y schemas | `openapi.yaml` |
| **MCP Notificaciones** | `mcp-notificaciones.js` | Consulta notificaciones procesadas | `GET /notificaciones` |
| **MCP Build** | `mcp-build.js` | Información de versiones y dependencias | `pom.xml` (ambos servicios) |
| **Orquestador** | `orquestador.js` | Carga todos los MCPs y enruta requests | N/A |

---

## Setup

### 1. Instalar dependencias

```bash
cd incidence-platform
npm install
```

Esto instala:
- `@modelcontextprotocol/sdk` — SDK oficial de MCP
- `yaml` — Parser de YAML (para openapi.yaml)
- `xml2js` — Parser de XML (para pom.xml)

### 2. Verificar estructura

Los MCPs deben estar en la carpeta raíz **o en `/mcps`**:

```
incidence-platform/
  ├── orquestador.js          ← Punto de entrada
  ├── mcp-incidencias.js      ← MCP especializado
  ├── mcp-notificaciones.js   ← MCP especializado
  ├── mcp-build.js            ← MCP especializado
  └── mcps/                   ← (opcional) Carpeta de descubrimiento automático
      ├── mcp-incidencias.js
      ├── mcp-notificaciones.js
      └── mcp-build.js
```

**Nota:** Actualmente los MCPs están en la raíz. Puedes copiarlos a `/mcps` y el Orquestador los descubrirá automáticamente.

### 3. Configurar Claude Desktop

**En macOS/Linux:**

```bash
vim ~/.config/Claude/claude_desktop_config.json
```

**En Windows:**

```bash
notepad "%APPDATA%\Claude\claude_desktop_config.json
```

**Contenido:**

```json
{
  "mcpServers": {
    "incidence-platform": {
      "command": "node",
      "args": ["C:\\ruta\\a\\incidence-platform\\orquestador.js"]
    }
  }
}
```

Reemplaza `C:\\ruta\\a\\incidence-platform` con la ruta real en tu sistema.

### 4. Reiniciar Claude Desktop

Cierra y abre Claude Desktop para que cargue la nueva configuración.

---

## Prompts de Prueba

Abre Claude Desktop y prueba estos prompts. El Orquestador debe seleccionar el MCP correcto automáticamente.

### Prompt 1: Endpoints del incidence-service
```
¿Qué endpoints expone el incidence-service?
```
**MCP esperado:** Incidencias  
**Respuesta:** Lista de todos los endpoints (GET, POST, PUT, DELETE)

### Prompt 2: Schema de un endpoint específico
```
¿Qué schema tiene el body para crear una incidencia?
Ayuda: busca el endpoint POST /incidencias
```
**MCP esperado:** Incidencias  
**Respuesta:** Schema JSON del request body

### Prompt 3: Códigos de error
```
¿Qué códigos de error HTTP devuelven los endpoints?
```
**MCP esperado:** Incidencias  
**Respuesta:** Lista de códigos 4xx y 5xx

### Prompt 4: Versión de Java
```
¿Qué versión de Java está configurada en los microservicios?
```
**MCP esperado:** Build  
**Respuesta:** Java version (ej: 17)

### Prompt 5: Versión de Spring Boot
```
¿Qué versión de Spring Boot usan los microservicios?
```
**MCP esperado:** Build  
**Respuesta:** Spring Boot version (ej: 3.5.13)

### Prompt 6: Dependencias
```
¿Qué dependencias de Kafka están configuradas?
```
**MCP esperado:** Build  
**Respuesta:** Dependencias relacionadas con Kafka

### Prompt 7: Estado del notification-service
```
¿Cuál es el estado del notification-service?
¿Cuántas notificaciones se han procesado?
```
**MCP esperado:** Notificaciones  
**Respuesta:** Estado (OK/ERROR), total procesadas, última actualización

### Prompt 8: Últimas notificaciones
```
¿Cuáles son las últimas notificaciones procesadas?
```
**MCP esperado:** Notificaciones  
**Respuesta:** Últimas 50 notificaciones con eventos y mensajes

---

## Cómo Funciona

### 1. Inicio del Orquestador

```bash
node orquestador.js
```

El Orquestador:
1. ✓ Descubre todos los MCPs (escaneando `/mcps` o usando archivos en raíz)
2. ✓ Inicia cada MCP como proceso hijo
3. ✓ Comunica con cada MCP por stdio (stdin/stdout)
4. ✓ Obtiene la lista de tools que expone cada MCP
5. ✓ Escucha conexiones de Copilot/Claude Desktop

### 2. Consulta desde Copilot/Claude Desktop

Cuando escribes un prompt:

1. **Copilot/Claude lee la descripción de cada tool**
   - Son textos en lenguaje natural que explican qué hace cada tool
   - La IA decide cuál tool usar basándose en el prompt

2. **Orquestador recibe la llamada a la tool**
   - Identifica qué MCP la expone
   - Delega la ejecución al MCP especializado

3. **MCP especializado ejecuta la lógica**
   - MCP Incidencias: parsea openapi.yaml
   - MCP Build: parsea pom.xml
   - MCP Notificaciones: consulta API REST

4. **Resultado vuelve a Copilot/Claude**
   - Texto legible (nunca stack traces)
   - JSON formateado si es estructura compleja

---

## Añadir Nuevos MCPs

La arquitectura es escalable. Añadir un nuevo MCP es muy simple:

### Paso 1: Crear el MCP

Crea `mcp-nuevo.js` en la raíz o en `/mcps`:

```javascript
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { ListToolsRequestSchema, CallToolRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const server = new Server({
  name: "mcp-nuevo",
  version: "1.0.0",
});

// Registrar tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "mi_tool",
      description: "Descripción en lenguaje natural de qué hace",
      inputSchema: { type: "object", properties: {}, required: [] },
    },
  ],
}));

// Manejar llamadas
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name } = request.params;
  if (name === "mi_tool") {
    return {
      content: [{ type: "text", text: "Resultado aquí" }],
    };
  }
});

// Iniciar
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("[MCP Nuevo] Iniciado");
}
main();
```

### Paso 2: Copiar a `/mcps` (opcional)

```bash
cp mcp-nuevo.js mcps/
```

### Paso 3: Reiniciar Orquestador

```bash
# Termina el proceso actual (Ctrl+C)
# Luego reinicia:
node orquestador.js
```

**Automáticamente:**
- ✓ El Orquestador descubre el nuevo MCP
- ✓ Carga sus tools
- ✓ Claude/Copilot puede usarlas
- ✓ Sin modificar `orquestador.js`

---

## Estructura de Archivos Clave

### `orquestador.js`

- **Responsabilidad:** Punto de entrada único, descubre y carga MCPs
- **No contiene:** Lógica de negocio, parsers, o llamadas HTTP
- **¿Cuándo modificar?** Casi nunca. Solo para cambios arquitectónicos.

### `mcp-incidencias.js`

- **Responsabilidad:** Parsea `openapi.yaml` y expone 3 tools
- **Carga:** Al iniciar el Orquestador (UNA SOLA VEZ)
- **Actualizar:** Si cambia el formato de openapi.yaml

### `mcp-build.js`

- **Responsabilidad:** Parsea `pom.xml` de ambos microservicios
- **Carga:** Al iniciar el Orquestador (UNA SOLA VEZ)
- **Actualizar:** Si cambias versiones de Java o Spring Boot

### `mcp-notificaciones.js`

- **Responsabilidad:** Consulta `GET /notificaciones` del notification-service
- **HTTP:** Usa fetch con timeout de 10 segundos
- **Errores:** Devuelve texto legible (nunca stack traces)
- **Configuración:** URL del servicio por variable de entorno `NOTIFICATION_SERVICE_URL`

### `notification-service/NotificacionController.java`

- **Cambio:** Endpoint `GET /notificaciones` devuelve DTO extendido con estadísticas
- **DTO:** `NotificacionesResponseDTO` incluye lista + estado del servicio
- **BD:** Usa PostgreSQL (ya configurado)

---

## Reglas de Implementación

### Arranque

- ✓ Parsea archivos UNA SOLA VEZ al iniciar
- ✓ Si un archivo falla, `process.exit(1)` inmediatamente
- ✓ El MCP no puede arrancar en estado roto

### Errores en Tools

- ✓ Nunca dejes excepciones sin capturar
- ✓ Devuelve texto legible: `"El endpoint '/foo' no existe en openapi.yaml"`
- ✓ El modelo de IA interpreta ese texto, no un stack trace

### Errores HTTP

- ✓ Envuelve `fetch` en try/catch
- ✓ Si el servicio no está disponible: `"notification-service no disponible: {mensaje}"`
- ✓ Sin fallbacks silenciosos

### Descripciones de Tools

- ✓ Lenguaje natural claro
- ✓ El modelo las lee para decidir cuándo usar cada tool
- ✓ Una descripción vaga = tool equivocada

---

## Troubleshooting

### El Orquestador no inicia

```bash
node orquestador.js
```

Verifica:
- ✓ Node.js v18+ instalado
- ✓ Dependencias instaladas: `npm install`
- ✓ Permisos de lectura en archivos (openapi.yaml, pom.xml)

### Las tools no aparecen en Claude Desktop

1. Verifica que `claude_desktop_config.json` existe y tiene la ruta correcta
2. Reinicia Claude Desktop
3. Abre la consola de Claude (Cmd+Shift+P → "Developer") para ver logs

### Un MCP no responde

```bash
node mcp-incidencias.js
# Debe mostrar: "[MCP Incidencias] Servidor iniciado. Escuchando en stdio..."
```

Si sale error, revisa la salida de error (stderr).

---

## Criterios de Éxito

- [ ] El Orquestador selecciona el MCP correcto en todos los prompts
- [ ] `notification-service` expone `GET /notificaciones` con estadísticas
- [ ] Cada MCP carga su fuente de datos al iniciar (sin reintentos)
- [ ] Las descripciones de tools son claras en lenguaje natural
- [ ] Añadir un nuevo MCP no requiere modificar componentes existentes
- [ ] Los prompts de prueba funcionan correctamente

---

## Referencias

- [Model Context Protocol](https://modelcontextprotocol.io)
- [MCP SDK para JavaScript](https://github.com/modelcontextprotocol/typescript-sdk)
- [Claude Desktop Docs](https://claude.ai/docs)

---

## Autores

- **Orquestador:** Implementación de escalabilidad por convención (auto-discovery de MCPs)
- **MCPs especializados:** Cada uno enfocado en un dominio específico
- **Fecha:** 2026-04-22

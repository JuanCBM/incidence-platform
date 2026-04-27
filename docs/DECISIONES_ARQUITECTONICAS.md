# 🎓 DECISIONES ARQUITECTÓNICAS — Explicación del Por Qué

**Documento de referencia para entender la arquitectura MCP implementada**

---

## 1. CARGA EN ARRANQUE, NO EN CADA LLAMADA

### Decisión
Los MCPs cargan sus fuentes de datos (openapi.yaml, pom.xml) **una sola vez al iniciar**.

### Alternativas Consideradas

**Opción A: Cargar en arranque (✅ ELEGIDA)**
```javascript
// Al iniciar MCP
const spec = loadOpenApiSpec();  // 1x
const cachedSpec = spec;

// En cada tool call
const endpoint = cachedSpec.paths[name];  // Instantáneo (memoria)
```

**Opción B: Cargar en cada call**
```javascript
// En cada tool call
const spec = loadOpenApiSpec();  // N veces
const endpoint = spec.paths[name];
```

**Opción C: Cargar por demanda (lazy loading)**
```javascript
// Primera vez que se llama
if (!cachedSpec) {
  cachedSpec = loadOpenApiSpec();
}
const endpoint = cachedSpec.paths[name];
```

### Por Qué Opción A

| Aspecto | Opción A | Opción B | Opción C |
|---------|----------|----------|----------|
| **Velocidad** | Instantáneo (memoria) | 100-500ms por llamada | 100-500ms (1ª vez) |
| **Fiabilidad** | Falla al inicio (exit 1) | Falla en runtime | Falla en runtime |
| **Previsibilidad** | Determinístico | No determinístico | No determinístico |
| **Complejidad** | Simple | Simple | Mediocre |

### Impacto

- **Copilot/Claude:** Respuestas **rápidas** (sin latencia de parseo)
- **Debugging:** Si hay error, falla **al arrancar** (no oculto en runtime)
- **Escalabilidad:** Soporta **1000s de llamadas** sin degradación

### Código de Referencia

```javascript
// mcp-incidencias.js
let cachedSpec = null;

function loadOpenApiSpec() {
  // ... parsear YAML ...
  return spec;
}

// Al iniciar
cachedSpec = loadOpenApiSpec();  // ← UNA SOLA VEZ

// Manejador de tools (puede llamarse 1000 veces)
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const endpoint = cachedSpec.paths[request.params.arguments.endpoint];
  // ^ Instantáneo: en memoria, no re-parsea
});
```

---

## 2. PUNTO DE ENTRADA ÚNICO (ORQUESTADOR)

### Decisión
Solo el Orquestador se registra en `claude_desktop_config.json`. Los MCPs son internos.

### Alternativas Consideradas

**Opción A: Un Orquestador (✅ ELEGIDA)**
```json
{
  "mcpServers": {
    "incidence-platform": {
      "command": "node",
      "args": ["orquestador.js"]
    }
  }
}
```

**Opción B: Registrar todos los MCPs directamente**
```json
{
  "mcpServers": {
    "mcp-incidencias": { "command": "node", "args": ["mcp-incidencias.js"] },
    "mcp-build": { "command": "node", "args": ["mcp-build.js"] },
    "mcp-notificaciones": { "command": "node", "args": ["mcp-notificaciones.js"] }
  }
}
```

### Por Qué Opción A

| Aspecto | Opción A | Opción B |
|---------|----------|----------|
| **Agregar MCP** | Cambiar archivo js, reiniciar Orquestador | Cambiar claude_desktop_config.json + reiniciar |
| **Configuración** | Un punto (Orquestador) | 3+ puntos (cada MCP) |
| **Escalabilidad** | ∞ MCPs sin tocar config | Crece exponencialmente |
| **Complejidad** | Centralizada | Distribuida |
| **Cambios frecuentes** | Solo código JS | Código JS + JSON |

### Impacto

- **Para el usuario:** Agregar nuevo MCP = **crear archivo JS**, no reconfigurar Claude Desktop
- **Para el operador:** Cambios centralizados en el código, no en configuración
- **Para el equipo:** Escalable — agregar MCPs es predecible

---

## 3. AUTO-DISCOVERY DE MCPs (CONVENCIÓN SOBRE CONFIGURACIÓN)

### Decisión
El Orquestador **escanea `/mcps`** automáticamente. Sin archivo de configuración.

### Alternativas Consideradas

**Opción A: Auto-discovery (✅ ELEGIDA)**
```javascript
// orquestador.js
const discovered = discoverMCPs();  // Lee /mcps
for (const mcp of discovered) {
  await bootMCP(mcp.path);  // Inicia automáticamente
}
```

**Opción B: Archivo de configuración**
```javascript
// mcps-config.js (necesario crear)
export const MCPS = [
  { name: 'incidencias', path: './mcps/mcp-incidencias.js' },
  { name: 'build', path: './mcps/mcp-build.js' },
  { name: 'notificaciones', path: './mcps/mcp-notificaciones.js' },
];

for (const mcp of MCPS) {
  await bootMCP(mcp.path);
}
```

**Opción C: Registro manual en Orquestador**
```javascript
// orquestador.js
const MCPs = [
  'mcp-incidencias',
  'mcp-build',
  'mcp-notificaciones',
];

for (const name of MCPs) {
  await bootMCP(`./mcps/${name}.js`);
}
```

### Por Qué Opción A

| Aspecto | A: Auto-discovery | B: Config | C: Manual |
|---------|------------------|----------|----------|
| **Agregar MCP** | `cp mcp-nuevo.js mcps/` | Editar config + copiar | Editar código + copiar |
| **Posibilidad de error** | Ninguna (convención) | Olvido en config | Olvido en código |
| **Files a mantener** | 0 (solo MCPs) | 1 (config) | 0 (solo MCPs) |
| **Onboarding** | "Copia aquí" | "Edita esto también" | "Edita esto también" |

### Impacto

- **Developers nuevos:** Entienden la convención en 30 segundos
- **Automatización:** CI/CD puede copiar MCPs sin cambios en código
- **Git:** Menos merge conflicts (no hay config centralizada)

---

## 4. STDIO COMO TRANSPORTE

### Decisión
Los MCPs se comunican con el Orquestador por **stdin/stdout** (protocolo stdio).

### Alternativas Consideradas

**Opción A: Stdio (✅ ELEGIDA)**
```
[Orquestador] → [stdout] → [Mensajes JSON] → [MCP]
[MCP] → [stdout] → [Respuesta JSON] → [Orquestador]
```

**Opción B: HTTP**
```
[MCP] ← GET/POST → [Orquestador]
                    ↓
                  Puerto 8001
```

**Opción C: WebSockets**
```
[MCP] ←WS→ [Orquestador]
           ↓
          Puerto 8002
```

### Por Qué Opción A (stdio)

| Aspecto | Stdio | HTTP | WebSocket |
|---------|-------|------|-----------|
| **Estándar** | MCP oficial ✓ | Propietario | Propietario |
| **Compatibilidad** | Claude, Copilot, Cursor, ... | Solo custom | Solo custom |
| **Setup** | Cero (subproceso) | Puertos, networking | Puertos, networking |
| **Seguridad** | Local (heredada del OS) | Necesita auth | Necesita auth |
| **Complicidad** | Trivial | Media | Media-Alta |

### Impacto

- **Portabilidad:** Los MCPs funcionan en Claude, Copilot, ChatGPT, Gemini sin cambios
- **Seguridad:** Comunicación local, sin network exposure
- **Futuro-proof:** Estándar abierto (Linux Foundation)

---

## 5. MANEJO DE ERRORES: TEXTO LEGIBLE, NO STACK TRACES

### Decisión
Las tools devuelven **mensajes en lenguaje natural**, nunca stack traces.

### Alternativas Consideradas

**Opción A: Texto legible (✅ ELEGIDA)**
```javascript
if (!pathItem) {
  return "El endpoint '/foo' no existe en openapi.yaml";
}
```

**Opción B: Lanzar excepción**
```javascript
if (!pathItem) {
  throw new Error("Cannot read property 'paths' of undefined at line 42");
}
// → Stack trace en consola de Claude
```

**Opción C: Fallback silencioso**
```javascript
if (!pathItem) {
  return null;  // o {}
}
// → Claude no sabe qué pasó
```

### Por Qué Opción A

| Aspecto | A: Texto claro | B: Exception | C: Fallback |
|---------|---|---|---|
| **Claude entiende** | ✓ Exactamente qué pasó | ✗ Stack trace incomprensible | ✗ No sabe si fue error |
| **Debugging** | ✓ El usuario sabe qué intentar | ✗ Confusión | ✗ Silent failure |
| **Acción** | ✓ Usuario puede reformular prompt | ✗ No hay acción posible | ✗ Acción desconocida |

### Código Ejemplo

```javascript
// ❌ MALO
async function obtenerNotificaciones() {
  const response = await fetch(...);
  return await response.json();  // Puede crashear
}

// ✅ BIEN
async function obtenerNotificaciones() {
  try {
    const response = await fetch(...);
    if (!response.ok) {
      return `notification-service respondió con status ${response.status}`;
    }
    return await response.json();
  } catch (error) {
    return `notification-service no disponible: ${error.message}`;
  }
}
```

---

## 6. DESCRIPCIONES DE TOOLS EN LENGUAJE NATURAL

### Decisión
Las descripciones de tools son el "prompt engineering" que decide qué tool usar.

### Ejemplo

```javascript
{
  name: "obtener_schema_endpoint",
  description: "Devuelve el schema de request y response para un endpoint específico. 
                Útil para entender qué datos enviar y qué recibirás",
  // ^ Claude Lee esto para decidir cuándo usar esta tool
}
```

### Por Qué Importa

Si la descripción fuera vaga:
```javascript
description: "Obtener schema"
// Claude podría confundirla con "obtener_schema_endpoint" vs "obtener_tipo_schema" vs...
```

Siendo clara:
```javascript
description: "Devuelve el schema de REQUEST Y RESPONSE para un ENDPOINT ESPECÍFICO"
// ↑ Palabras clave que Claude reconoce
```

---

## 7. PATRÓN REPOSITORIO PARA NOTIFICACIONES

### Decisión
Las notificaciones se guardan en **PostgreSQL** (no en memoria).

### Alternativas Consideradas

**Opción A: PostgreSQL (✅ ELEGIDA)**
```java
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
  List<Notificacion> findTop50ByOrderByFechaRecepcionDesc();
  long count();
}
```

**Opción B: En memoria**
```java
private List<Notificacion> notificaciones = new ArrayList<>();
```

**Opción C: Archivos JSON**
```java
new JsonRepository("notificaciones.json").findAll();
```

### Por Qué Opción A

| Aspecto | PostgreSQL | Memoria | JSON |
|---------|-----------|---------|------|
| **Persistencia** | ✓ Entre reinicios | ✗ Se pierde | ✓ Entre reinicios |
| **Escala** | ✓ Millones de filas | ✗ Limitado | ✗ Lento con archivos grandes |
| **Integridad** | ✓ ACID, transacciones | ✗ Nada | ✗ Sincronización de archivos |
| **Concurrencia** | ✓ Multi-proceso | ✗ Conflictos | ✗ Conflictos |

### Patrón Implementado

```java
// Interfaz (abstracción)
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
  List<Notificacion> findTop50ByOrderByFechaRecepcionDesc();
}

// Implementación: PostgreSQL (vía Spring Data JPA)
// El consumidor Kafka usa la interfaz, nunca directamente PostgreSQL
```

**Ventaja:** Si en el futuro quieres cambiar a MongoDB, solo reemplazas la implementación.

---

## 8. COMUNICACIÓN ORQUESTADOR ↔ MCPs

### Decisión
Comunicación **asincrónica bidireccional por JSON-RPC** sobre stdio.

### Formato de Mensajes

```javascript
// Orquestador envía
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}

// MCP responde
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [...]
  }
}
```

### Por Qué JSON-RPC

- ✓ Estándar abierto (RFC 7115)
- ✓ Soporta request/response con ID
- ✓ Error handling estándar
- ✓ Compatible con MCP SDK oficial

---

## 📊 Matriz de Decisiones

| Decisión | Opción Elegida | Razón Principal |
|----------|---|---|
| Carga | Arranque | Velocidad (instantáneo en runtime) |
| Entrada | Orquestador único | Escalabilidad (agregar MCPs sin config) |
| Discovery | Auto (carpeta /mcps) | Convención sobre configuración |
| Transporte | Stdio | Estándar abierto (compatible futura) |
| Errores | Texto claro | Claude entiende y actúa |
| Tools | Descripciones claras | Prompt engineering (selección correcta) |
| Datos | PostgreSQL | Persistencia + escala |
| Protocolo | JSON-RPC | Estándar + bidireccional |

---

## 🎯 Conclusión

Cada decisión fue tomada considerando:

1. **Escalabilidad:** ¿Funciona con 1 MCP? ¿Con 10? ¿Con 100?
2. **Mantenibilidad:** ¿Otros developers lo entienden fácilmente?
3. **Fiabilidad:** ¿Qué pasa si algo falla?
4. **Compatibilidad:** ¿Funciona con las herramientas existentes?
5. **Futuro:** ¿Es un estándar abierto o propietario?

La arquitectura resultante es **simple, escalable y compatible** con el ecosistema MCP abierto.


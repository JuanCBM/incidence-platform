# 🏗️ ARQUITECTURA VISUAL — MCP Incidence Platform

---

## 🎯 Flujo de Consulta Completo

```
┌─────────────────────────────────────────────────────────────────────┐
│                  Usuario en Claude Desktop / Copilot                │
│                   "¿Qué endpoints expone?"                          │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
                ┌─────────────────────────────┐
                │  Orquestador (punto entrada)│
                │   orquestador.js            │
                └────────────┬────────────────┘
                             │
                    Busca qué MCP proporciona
                    la tool "listar_endpoints"
                             │
                             ▼
                ┌─────────────────────────────┐
                │  MCP Incidencias            │
                │  mcp-incidencias.js         │
                └────────────┬────────────────┘
                             │
                    Accede a openapi.yaml
                    (cacheado en memoria)
                             │
                             ▼
        ┌────────────────────────────────────┐
        │ incidence-service/openapi.yaml     │
        │ (cargado al iniciar, no re-parsea) │
        └────────────────────────────────────┘
                             │
                             ▼
                    Retorna: [endpoints]
                             │
                             ▼
        ┌─────────────────────────────────────────┐
        │  Claude interpreta la lista de endpoints│
        │  y genera respuesta en lenguaje natural │
        └─────────────────────────────────────────┘
                             │
                             ▼
        ┌──────────────────────────────────────────┐
        │  "He encontrado 12 endpoints:            │
        │   GET   /usuarios                        │
        │   POST  /usuarios                        │
        │   ..."                                   │
        └──────────────────────────────────────────┘
```

---

## 🗂️ Estructura de Directorios

```
incidence-platform/
│
├── 📄 package.json                    ← Dependencias Node.js
├── 📄 orquestador.js                  ← ⭐ PUNTO DE ENTRADA ÚNICO
│
├── 📄 mcp-incidencias.js              ← MCP especializado (3 tools)
├── 📄 mcp-build.js                    ← MCP especializado (4 tools)
├── 📄 mcp-notificaciones.js           ← MCP especializado (2 tools)
│
├── 📄 claude_desktop_config.json      ← Configuración para Claude Desktop
│
├── 📂 mcps/                           ← (Opcional) Carpeta de descubrimiento automático
│   ├── mcp-incidencias.js
│   ├── mcp-build.js
│   └── mcp-notificaciones.js
│
├── 📂 incidence-service/              ← Microservicio existente
│   ├── pom.xml
│   ├── src/
│   │   └── main/
│   │       └── resources/
│   │           └── openapi.yaml       ← Fuente de datos para MCP Incidencias
│   └── ...
│
├── 📂 notification-service/           ← Microservicio existente
│   ├── pom.xml                        ← Fuente de datos para MCP Build
│   ├── src/
│   │   └── main/
│   │       └── java/com/empresa/notification/
│   │           ├── controller/
│   │           │   └── NotificacionController.java   ← GET /notificaciones
│   │           └── dto/
│   │               └── NotificacionesResponseDTO.java ← DTO con estadísticas
│   └── ...
│
└── 📂 Documentación/
    ├── README.MCP.md                  ← Guía completa
    ├── SETUP_Y_VALIDACION.md          ← Paso a paso
    ├── DECISIONES_ARQUITECTONICAS.md  ← Por qué cada decisión
    └── IMPLEMENTACION_MCP_RESUMEN.md  ← Resumen técnico
```

---

## 🔄 Ciclo de Vida del Orquestador

```
┌────────────────────────────────────────────────────────────────┐
│ 1. STARTUP                                                     │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  $ node orquestador.js                                        │
│       │                                                        │
│       ├─ 🔍 Descubrir MCPs en /mcps                           │
│       │       └─ Encontrados: incidencias, build,             │
│       │           notificaciones                              │
│       │                                                        │
│       ├─ 🚀 Iniciar MCP: incidencias                          │
│       │       └─ Lee openapi.yaml (cacheado)                  │
│       │       └─ Expone 3 tools                               │
│       │                                                        │
│       ├─ 🚀 Iniciar MCP: build                                │
│       │       └─ Lee pom.xml (cacheado)                       │
│       │       └─ Expone 4 tools                               │
│       │                                                        │
│       ├─ 🚀 Iniciar MCP: notificaciones                       │
│       │       └─ Configura URL del servicio                   │
│       │       └─ Expone 2 tools                               │
│       │                                                        │
│       └─ 📡 Escuchar en stdio                                 │
│           (esperando conexión de Claude/Copilot)             │
│                                                                │
└────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────┐
│ 2. RUNTIME (esperando solicitudes)                            │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Claude Desktop / Copilot se conecta                          │
│       │                                                        │
│       ├─ Solicita listar todas las tools disponibles          │
│       │       └─ Orquestador: 9 tools totales                │
│       │                                                        │
│       └─ Usuario escribe prompt                               │
│           "¿Qué endpoints expone?"                            │
│                  │                                             │
│                  ▼                                             │
│       Claude decide usar: listar_endpoints                    │
│                  │                                             │
│                  ▼                                             │
│       Orquestador recibe request                              │
│           1. Identifica: MCP Incidencias                      │
│           2. Delega la ejecución                              │
│           3. Espera respuesta                                 │
│                  │                                             │
│                  ▼                                             │
│       MCP Incidencias ejecuta tool                            │
│           1. Accede cachedSpec (sin re-parsear)               │
│           2. Extrae endpoints                                 │
│           3. Retorna JSON                                     │
│                  │                                             │
│                  ▼                                             │
│       Orquestador envía respuesta a Claude                    │
│                  │                                             │
│                  ▼                                             │
│       Claude genera respuesta en lenguaje natural              │
│                  │                                             │
│                  ▼                                             │
│       Usuario ve: "He encontrado 12 endpoints..."             │
│                                                                │
└────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────┐
│ 3. SHUTDOWN                                                    │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Ctrl+C en terminal                                           │
│       │                                                        │
│       ├─ Cierra MCPs (procesos hijos)                         │
│       └─ Desconecta stdio                                     │
│                                                                │
│  Para agregar nuevo MCP:                                      │
│       1. Crear mcp-nuevo.js                                   │
│       2. Copiar a /mcps (opcional)                            │
│       3. Reiniciar Orquestador (Ctrl+C, node orquestador.js)  │
│           (sin cambiar código del Orquestador)                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 🔌 Comunicación entre Procesos (stdio)

```
┌──────────────────────┐         stdin/stdout        ┌──────────────────────┐
│                      │◄─────────────────────────►│                      │
│ Claude Desktop       │  Protocolo JSON-RPC       │ Orquestador          │
│                      │                            │                      │
│ • Envía prompts      │                            │ • Recibe requests    │
│ • Selecciona tools   │                            │ • Enruta a MCPs      │
│ • Muestra respuestas │                            │ • Agrega tools       │
│                      │                            │                      │
└──────────────────────┘                            └──────────┬───────────┘
                                                              │
                                                      stdin/stdout
                                                              │
                        ┌─────────────────────────────────────┼──────────────────────┐
                        ▼                                     ▼                      ▼
                 ┌──────────────────┐          ┌──────────────────────┐  ┌──────────────────┐
                 │ MCP Incidencias  │          │ MCP Build            │  │ MCP Notificaciones│
                 │                  │          │                      │  │                   │
                 │ 3 tools:         │          │ 4 tools:             │  │ 2 tools:          │
                 │ • listar_endup   │          │ • obtener_version... │  │ • obtener_notif...│
                 │ • obtener_schema │          │ • listar_dependencias│  │ • obtener_estado..│
                 │ • listar_codigos │          │ • listar_plugins     │  │                   │
                 │                  │          │                      │  │ Consulta API REST │
                 │ Lee: openapi.yaml│          │ Lee: pom.xml         │  │ Endpoint:         │
                 │ (cacheado)       │          │ (cacheado)           │  │ GET /notificaciones│
                 └──────────────────┘          └──────────────────────┘  └──────────────────┘
```

---

## 📊 Matriz de Tools por MCP

```
┌─────────────────────┬──────────────────────────────┬────────────────┐
│ MCP                 │ Tools Disponibles            │ Fuente de Datos│
├─────────────────────┼──────────────────────────────┼────────────────┤
│ Incidencias         │ 1. listar_endpoints          │ openapi.yaml   │
│ (mcp-incidencias)   │ 2. obtener_schema_endpoint   │ (parseado 1x)  │
│                     │ 3. listar_codigos_error      │                │
├─────────────────────┼──────────────────────────────┼────────────────┤
│ Build               │ 1. obtener_version_java      │ pom.xml        │
│ (mcp-build)         │ 2. obtener_version_spring    │ (parseado 1x)  │
│                     │ 3. listar_dependencias       │ (ambos)        │
│                     │ 4. listar_plugins_maven      │                │
├─────────────────────┼──────────────────────────────┼────────────────┤
│ Notificaciones      │ 1. obtener_notificaciones    │ GET /notif...  │
│ (mcp-notificaciones)│ 2. obtener_estado_servicio   │ (consulta live)│
│                     │                              │ PostgreSQL BD  │
└─────────────────────┴──────────────────────────────┴────────────────┘

TOTAL: 9 tools

Las descripciones de cada tool guían a Claude en la selección.
```

---

## 🎯 Flujo de Decisión del Orquestador

```
┌─ Request llega con tool: "listar_endpoints"
│
└─► ¿Qué MCP la proporciona?
    │
    ├─ incidencias?  ✓ SÍ → Delegar a MCP Incidencias
    ├─ build?        ✗ No
    └─ notificaciones? ✗ No
        │
        ▼
    MCP Incidencias ejecuta:
        1. Accede cachedSpec.paths
        2. Extrae todos los endpoints
        3. Retorna JSON
        │
        ▼
    Respuesta al usuario


┌─ Request llega con tool: "obtener_version_spring"
│
└─► ¿Qué MCP la proporciona?
    │
    ├─ incidencias?  ✗ No
    ├─ build?        ✓ SÍ → Delegar a MCP Build
    └─ notificaciones? ✗ No
        │
        ▼
    MCP Build ejecuta:
        1. Accede cachedPoms['incidencias']
        2. Lee versión de parent
        3. Retorna: "3.5.13"
        │
        ▼
    Respuesta al usuario


┌─ Request llega con tool: "obtener_estado_servicio"
│
└─► ¿Qué MCP la proporciona?
    │
    ├─ incidencias?  ✗ No
    ├─ build?        ✗ No
    └─ notificaciones? ✓ SÍ → Delegar a MCP Notificaciones
        │
        ▼
    MCP Notificaciones ejecuta:
        1. fetch(notification-service/notificaciones)
        2. Parse JSON response
        3. Retorna estadísticas
        │
        ▼
    Respuesta al usuario
```

---

## 🔐 Capas de Seguridad y Aislamiento

```
┌──────────────────────────────────────────────────────────┐
│ Nivel 1: Control de Acceso                               │
│                                                          │
│ Solo Claude Desktop / Copilot pueden conectarse          │
│ • Autenticación local (heredada del OS)                  │
│ • Sin network exposure                                   │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│ Nivel 2: Validación de Tools                             │
│                                                          │
│ Orquestador valida que cada request sea para una tool   │
│ existente. Si no, retorna error legible.                │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│ Nivel 3: Aislamiento de Procesos                         │
│                                                          │
│ Cada MCP es un proceso separado:                        │
│ • Crash en un MCP ≠ Crash en Orquestador                │
│ • Recursos independientes                               │
│ • Permisos del OS (no pueden escalar privilegios)       │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│ Nivel 4: Manejo de Errores                               │
│                                                          │
│ Los MCPs capturan excepciones y devuelven texto legible │
│ • Nunca stack traces (información sensible)              │
│ • Claude recibe mensaje claro                           │
│ • Usuario puede actuar correctamente                    │
└──────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│ Nivel 5: Timeout                                         │
│                                                          │
│ MCP Notificaciones: timeout de 10s en fetch             │
│ • Evita cuelgues si el servicio cae                      │
│ • Retorna error legible si timeout                       │
└──────────────────────────────────────────────────────────┘
```

---

## 📈 Escalabilidad

```
Hoy (3 MCPs):              Futuro (N MCPs):

  ┌──────────────┐           ┌──────────────┐
  │ Incidencias  │           │ Incidencias  │
  └──────────────┘           └──────────────┘
  
  ┌──────────────┐           ┌──────────────┐
  │ Build        │           │ Build        │
  └──────────────┘           └──────────────┘
  
  ┌──────────────┐           ┌──────────────┐
  │ Notificaciones│           │ Notificaciones
  └──────────────┘           └──────────────┘
                             
    ↓                         ├──────────────┐
                              │ Database     │
  ORQUESTADOR                 ├──────────────┤
    ↓                         │ Security     │
                              ├──────────────┤
  3 MCPs                       │ Deployment   │
                              ├──────────────┤
  Costo: Fijo                 │ Monitoring   │
                              ├──────────────┤
                              │ Custom MCP 1 │
                              ├──────────────┤
                              │ Custom MCP 2 │
                              └──────────────┘
                                   ↓
                              ORQUESTADOR
                                   ↓
                              N MCPs
                              
                              Costo: Solo agregar archivos
                              (sin modificar Orquestador)
```

---

## ✨ Ejemplo Completo: Consulta Real

```
USUARIO:
  "¿Qué schema tiene el endpoint POST /incidencias?"

          ↓

CLAUDE (internamente):
  "El usuario pregunta por un schema de endpoint específico.
   La tool 'obtener_schema_endpoint' parece perfecta.
   Necesito parámetros: endpoint='/incidencias', method='POST'"

          ↓

ORQUESTADOR (recibe):
  {
    "name": "obtener_schema_endpoint",
    "arguments": {
      "endpoint": "/incidencias",
      "method": "POST"
    }
  }

          ↓

ORQUESTADOR (busca):
  "¿Qué MCP expone 'obtener_schema_endpoint'?"
  → "MCP Incidencias"

          ↓

MCP INCIDENCIAS (ejecuta):
  const operation = cachedSpec.paths["/incidencias"]["post"];
  return {
    endpoint: "/incidencias",
    method: "POST",
    requestBody: { ... schema del body ... },
    responses: {
      201: { ... schema de respuesta creado ... },
      400: { ... schema de error ... }
    }
  };

          ↓

CLAUDE (recibe JSON):
  Interpreta el schema y genera:
  
  "Para crear una incidencia, necesitas:
   {
     "titulo": "string (requerido)",
     "descripcion": "string (requerido)",
     "prioridad": "BAJA|MEDIA|ALTA (por defecto MEDIA)",
     "usuarioId": "number (requerido)"
   }
   
   Recibirás un 201 con la incidencia creada o 400 si faltan datos."

          ↓

USUARIO:
  Lee la respuesta clara y puede hacer la request correctamente.
```

---

## 🎓 Conclusión Visual

La arquitectura implementada es:

```
┌─────────────────────────────────────────────────┐
│ SIMPLE                                          │
│ • Punto de entrada único (Orquestador)          │
│ • MCPs especializados y aislados                │
│ • Sin lógica compartida                         │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ ESCALABLE                                       │
│ • Auto-discovery de MCPs (carpeta /mcps)        │
│ • Agregar MCP = crear archivo, no modificar     │
│ • N MCPs sin degradación                        │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ MANTENIBLE                                      │
│ • Carga de datos en arranque (confiable)        │
│ • Errores en texto legible (debuggeable)        │
│ • Protocolo abierto stdio (futuro-proof)        │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ COMPATIBLE                                      │
│ • Estándar MCP (Linux Foundation)               │
│ • Funciona en Claude, Copilot, ChatGPT, Gemini │
│ • Sin cambios de código                         │
└─────────────────────────────────────────────────┘
```

**Resultado:** Arquitectura **producción-ready** lista para conectar a Claude Desktop y GitHub Copilot. 🚀


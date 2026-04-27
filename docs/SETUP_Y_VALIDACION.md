# ✅ GUÍA DE SETUP Y VALIDACIÓN

**Paso a paso para levantar toda la plataforma y validar la arquitectura MCP**

---

## 📋 Prerrequisitos

- [ ] Node.js 18+ instalado
- [ ] Java 17+ instalado
- [ ] Podman Desktop instalado y corriendo
- [ ] Python + podman-compose instalado (`pip install podman-compose`)
- [ ] Git configurado
- [ ] Editor de código (VS Code, JetBrains, etc.)

---

## 🐳 Paso 1: Levantar la infraestructura (Podman)

### Primera vez o tras cambios en el código Java

```powershell
# Compilar los JARs
cd incidence-service
.\mvnw clean package -DskipTests

cd ..\notification-service
.\mvnw clean package -DskipTests

# Volver a raíz y levantar reconstruyendo imágenes
cd ..
python -m podman_compose up --build -d
```

### Arranques normales (sin cambios en el código)

```powershell
# Si Podman Desktop acaba de arrancar
podman machine start

# Levantar contenedores en segundo plano
cd incidence-platform
python -m podman_compose up -d
```

### Verificar que todo está corriendo

```powershell
podman ps
```

**Esperado:** 6 contenedores en estado `Up`:

| Contenedor | Puerto |
|---|---|
| `incidencias-db` | 5432 |
| `notificaciones-db` | 5433 |
| `zookeeper` | 2181 |
| `kafka` | 9092 |
| `incidence-service` | 8080 |
| `notification-service` | 8081 |

### Parar los contenedores

```powershell
python -m podman_compose down
```

---

## 🌐 Paso 2: Levantar el Frontend Angular

```powershell
cd incidence-frontend
npm install        # solo la primera vez
npm start          # arranca con proxy configurado automáticamente
```

**Esperado:**
```
✔ Compiled successfully.
Application bundle generation complete.
```

Abrir en el navegador: **http://localhost:4200**

> El proxy está configurado en `angular.json` — redirige automáticamente
> `/incidencias`, `/usuarios` y `/notificaciones` al backend.

---

## 📦 Paso 3: Instalar dependencias Node (MCPs)

```powershell
cd incidence-platform
npm install
```

**Verificar:**
```powershell
npm list @modelcontextprotocol/sdk yaml xml2js
```

---

## 🧪 Paso 4: Probar MCPs individualmente

### MCP Incidencias

```powershell
node mcp/mcp-incidencias.js
```

**Esperado:**
```
[MCP Incidencias] Parseando openapi.yaml...
[MCP Incidencias] ✓ openapi.yaml parseado correctamente
[MCP Incidencias] Servidor iniciado. Escuchando en stdio...
```

### MCP Build

```powershell
node mcp/mcp-build.js
```

**Esperado:**
```
[MCP Build] ✓ incidencias parseado
[MCP Build] ✓ notificaciones parseado
[MCP Build] ✓ Todos los pom.xml parseados correctamente
[MCP Build] Servidor iniciado. Escuchando en stdio...
```

### MCP Notificaciones *(requiere contenedor)*

```powershell
node mcp/mcp-notificaciones.js
```

**Esperado:**
```
[MCP Notificaciones] Servidor iniciado. Escuchando en stdio...
[MCP Notificaciones] Consultará: http://127.0.0.1:8081
```

**Presiona Ctrl+C para salir de cada uno.**

---

## 🎬 Paso 5: Verificar el Orquestador

```powershell
node mcp/orquestador.js
```

**Esperado:**
```
[Orquestador] ✓ Encontrados 3 MCPs: mcp-build.js, mcp-incidencias.js, mcp-notificaciones.js
[Orquestador] ✓ MCP cargado: build (4 tools)
[Orquestador] ✓ MCP cargado: incidencias (3 tools)
[Orquestador] ✓ MCP cargado: notificaciones (2 tools)
[Orquestador] ✓ 3/3 MCPs cargados correctamente
[Orquestador] ✓ Conectado. Servidor activo.
```

---

## 🔌 Paso 6: Configurar Claude Desktop

El archivo `claude_desktop_config.json` de la raíz del proyecto es una copia de referencia.
El real está en:

```
%APPDATA%\Claude\claude_desktop_config.json
```

**Contenido correcto:**

```json
{
  "mcpServers": {
    "incidence-platform": {
      "command": "node",
      "args": ["C:\\ruta\\completa\\incidence-platform\\mcp\\orquestador.js"]
    }
  }
}
```

Después de editar: **cerrar y reabrir Claude Desktop**.

---

## 🔌 Paso 7: Configurar GitHub Copilot (VS Code)

El archivo `.vscode/mcp.json` ya está configurado en el proyecto.
Al abrir VS Code en la carpeta del proyecto, Copilot detectará el servidor automáticamente.

Si no lo detecta: `Ctrl+Shift+P` → `MCP: List Servers`

---

## 🎯 Paso 8: Probar prompts

| Prompt | MCP | ¿Necesita contenedor? |
|---|---|---|
| ¿Qué endpoints expone el incidence-service? | Incidencias | ❌ |
| ¿Qué schema tiene POST /incidencias? | Incidencias | ❌ |
| ¿Qué versión de Spring Boot usan los microservicios? | Build | ❌ |
| ¿Qué versión de Java usan? | Build | ❌ |
| ¿Qué dependencias de Kafka están configuradas? | Build | ❌ |
| ¿Cuántas notificaciones se han procesado? | Notificaciones | ✅ |
| ¿Está disponible el notification-service? | Notificaciones | ✅ |

---

## 🐛 Troubleshooting

### Las tools no aparecen en Claude Desktop
1. Verifica que el config apunta a `mcp/orquestador.js` (no a `orquestador.js`)
2. Reinicia Claude Desktop completamente
3. Comprueba que Node.js está en el PATH del sistema

### Error `Cannot find module '@modelcontextprotocol/sdk'`
```powershell
cd incidence-platform
npm install
```

### Error `openapi.yaml no encontrado`
```powershell
ls incidence-service/src/main/resources/openapi.yaml
```

### Angular muestra error al cargar datos
1. Verifica que los contenedores están `Up`: `podman ps`
2. Verifica que el backend responde: `Invoke-WebRequest http://localhost:8080/usuarios`
3. Asegúrate de arrancar con `npm start` (tiene el proxy configurado)

### MCP Notificaciones devuelve TIMEOUT
- El contenedor `notification-service` no está corriendo
- Arranca con `python -m podman_compose up -d`

---

## 📊 Checklist de Éxito

- [ ] `podman ps` muestra 6 contenedores `Up`
- [ ] Angular carga en `http://localhost:4200` con datos
- [ ] `npm install` completó sin errores (carpeta raíz)
- [ ] Orquestador detecta 3/3 MCPs
- [ ] `claude_desktop_config.json` apunta a `mcp/orquestador.js`
- [ ] Claude Desktop / Copilot responden a al menos 3 prompts de prueba

**Si todo está checkado:** ✅ **PLATAFORMA COMPLETAMENTE OPERATIVA**

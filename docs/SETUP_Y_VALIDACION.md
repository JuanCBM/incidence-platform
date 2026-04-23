# ✅ GUÍA DE SETUP Y VALIDACIÓN

**Paso a paso para levantar y validar la arquitectura MCP**

---

## 📋 Checklist Pre-Setup

- [ ] Node.js 18+ instalado
- [ ] Git configurado
- [ ] Acceso a la carpeta `incidence-platform`
- [ ] Editor de código (VS Code, JetBrains, etc.)

---

## 🚀 Paso 1: Instalar Dependencias

```bash
cd incidence-platform
npm install
```

**Esperado:**
```
added 45 packages in 3.5s
```

**Verificar:**
```bash
npm list @modelcontextprotocol/sdk yaml xml2js
```

---

## 🔍 Paso 2: Verificar Estructura de Archivos

### Archivos Requeridos

```bash
# Raíz del proyecto
ls -la | grep -E "(package.json|orquestador|mcp-|claude_desktop)"
```

**Esperado:**

```
package.json
orquestador.js
mcp-incidencias.js
mcp-notificaciones.js
mcp-build.js
claude_desktop_config.json
```

### Archivos Java (notification-service)

```bash
find notification-service -name "NotificacionController.java" -o -name "NotificacionesResponseDTO.java"
```

**Esperado:**

```
notification-service/src/main/java/com/empresa/notification/controller/NotificacionController.java
notification-service/src/main/java/com/empresa/notification/dto/NotificacionesResponseDTO.java
```

---

## 🧪 Paso 3: Probar MCPs Individuales

### Test 1: MCP Incidencias

```bash
node mcp-incidencias.js
```

**Esperado (en stderr):**
```
[MCP Incidencias] Parseando openapi.yaml...
[MCP Incidencias] ✓ openapi.yaml parseado correctamente
[MCP Incidencias] Servidor iniciado. Escuchando en stdio...
```

**Presiona Ctrl+C para salir**

### Test 2: MCP Build

```bash
node mcp-build.js
```

**Esperado:**
```
[MCP Build] Parseando pom.xml de microservicios...
[MCP Build] ✓ incidencias parseado
[MCP Build] ✓ notificaciones parseado
[MCP Build] ✓ Todos los pom.xml parseados correctamente
[MCP Build] Servidor iniciado. Escuchando en stdio...
```

**Presiona Ctrl+C para salir**

### Test 3: MCP Notificaciones

```bash
node mcp-notificaciones.js
```

**Esperado:**
```
[MCP Notificaciones] Servidor iniciado. Escuchando en stdio...
[MCP Notificaciones] Consultará: http://localhost:8081
```

**Presiona Ctrl+C para salir**

---

## 🎬 Paso 4: Iniciar el Orquestador

```bash
node orquestador.js
```

**Esperado:**
```
╔═══════════════════════════════════════════════════════════════════╗
║                  ORQUESTADOR MCP v1.0                            ║
║              Incidence Platform — GitHub Copilot                 ║
║                                                                   ║
║  Punto de entrada único para Copilot/Claude Desktop              ║
║  Carga MCPs especializados una sola vez en memoria               ║
║                                                                   ║
║  https://modelcontextprotocol.io                                 ║
╚═══════════════════════════════════════════════════════════════════╝

[Orquestador] 🔍 Descubriendo MCPs...
[Orquestador] ✓ Encontrados 3 MCPs
[Orquestador] 🚀 Iniciando: incidencias
[Orquestador] ✓ MCP cargado: incidencias (3 tools)
[Orquestador] 🚀 Iniciando: build
[Orquestador] ✓ MCP cargado: build (4 tools)
[Orquestador] 🚀 Iniciando: notificaciones
[Orquestador] ✓ MCP cargado: notificaciones (2 tools)
[Orquestador] ✓ 3/3 MCPs cargados
[Orquestador] 📡 Iniciando servidor stdio...
[Orquestador] Esperando conexión desde Copilot/Claude...

[Orquestador] ✓ Conectado. Servidor activo.
```

**Mantén este proceso corriendo. No cierres esta terminal.**

---

## 🔌 Paso 5: Configurar Claude Desktop

### En Windows

```powershell
notepad "$env:APPDATA\Claude\claude_desktop_config.json"
```

### En macOS/Linux

```bash
vim ~/.config/Claude/claude_desktop_config.json
```

### Contenido

```json
{
  "mcpServers": {
    "incidence-platform": {
      "command": "node",
      "args": ["C:\\ruta\\completa\\a\\incidence-platform\\orquestador.js"]
    }
  }
}
```

**Importante:** Reemplaza `C:\\ruta\\completa\\a\\` con la ruta real en tu sistema.

**Para encontrar la ruta:**

```bash
cd incidence-platform && pwd  # Linux/macOS
cd incidence-platform && echo %CD%  # Windows
```

---

## 🔄 Paso 6: Reiniciar Claude Desktop

1. Cierra Claude Desktop completamente
2. Abre Claude Desktop nuevamente
3. Espera a que cargue (20-30 segundos)

---

## 🎯 Paso 7: Probar Prompts

En una nueva ventana de Claude Desktop, prueba estos prompts:

### Prompt 1: ¿Qué endpoints expone el incidence-service?

**Esperado:** Lista de todos los endpoints del API

**Logs del Orquestador:**
```
[Orquestador] 🔧 Tool solicitada: listar_endpoints
[Orquestador] → Delegando a MCP: incidencias / listar_endpoints
[Orquestador] ← Respuesta recibida de incidencias
```

### Prompt 2: ¿Qué versión de Spring Boot?

**Esperado:** Spring Boot 3.5.13

**Logs:**
```
[Orquestador] 🔧 Tool solicitada: obtener_version_spring
[Orquestador] → Delegando a MCP: build / obtener_version_spring
```

### Prompt 3: ¿Cuántas notificaciones se han procesado?

**Esperado:** Número total de notificaciones en la BD

**Logs:**
```
[Orquestador] 🔧 Tool solicitada: obtener_estado_servicio
[Orquestador] → Delegando a MCP: notificaciones / obtener_estado_servicio
```

---

## 🐛 Troubleshooting

### El Orquestador no inicia

```bash
node orquestador.js
```

**Error:** `Cannot find module '@modelcontextprotocol/sdk'`

**Solución:**
```bash
npm install
```

**Error:** `openapi.yaml no encontrado`

**Solución:** Verifica que el archivo existe:
```bash
ls incidence-service/src/main/resources/openapi.yaml
```

---

### Las tools no aparecen en Claude Desktop

1. **Verifica que el Orquestador está corriendo:**
   ```bash
   ps aux | grep orquestador
   ```

2. **Comprueba la configuración:**
   ```bash
   cat ~/.config/Claude/claude_desktop_config.json  # Linux/macOS
   cat "%APPDATA%\Claude\claude_desktop_config.json"  # Windows
   ```

3. **Reinicia Claude Desktop** (cierra y abre nuevamente)

4. **Abre la consola de desarrollador en Claude:**
   - Cmd+Shift+P → "Developer"
   - Busca logs de conexión al Orquestador

---

### Un MCP no responde

Abre una terminal separada y prueba el MCP:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | node mcp-incidencias.js
```

**Esperado:** JSON con lista de tools

---

### Timeout ejecutando una tool

**Posible causa:** El notification-service no está disponible

**Solución:**

1. Asegúrate que notification-service está corriendo en `http://localhost:8081`
2. Verifica la variable de entorno:
   ```bash
   echo $NOTIFICATION_SERVICE_URL  # Linux/macOS
   echo %NOTIFICATION_SERVICE_URL%  # Windows
   ```

3. Si necesitas cambiar el URL:
   ```bash
   export NOTIFICATION_SERVICE_URL=http://localhost:8081
   node orquestador.js
   ```

---

## 📊 Validación Final

### Checklist de Éxito

- [ ] `npm install` completó sin errores
- [ ] Cada MCP inicia sin errores
- [ ] El Orquestador detecta todos los 3 MCPs
- [ ] claude_desktop_config.json está configurado
- [ ] Claude Desktop abre sin errores
- [ ] Al menos 3 prompts de prueba funcionan
- [ ] Los logs del Orquestador muestran delegación correcta

**Si todo está checkado:** ✅ **IMPLEMENTACIÓN COMPLETADA**

---

## 🎓 Próximos Pasos

1. **Documentar prompts frecuentes:** Crea un archivo con prompts útiles para tu equipo
2. **Añadir más MCPs:** Copia `mcp-nuevo.js` a `/mcps` y reinicia
3. **Integración CI/CD:** Valida MCPs en tu pipeline
4. **Monitoreo:** Agrega logs estructurados (JSON) para análisis

---

## 📞 Soporte

Si algo no funciona:

1. **Revisa los logs:** El Orquestador imprime todo en stderr
2. **Verifica archivos:** openapi.yaml, pom.xml deben existir
3. **Prueba MCPs aislados:** `node mcp-incidencias.js`
4. **Lee README.MCP.md:** Documentación completa

---

## 🎉 ¡Listo!

Ahora puedes consultar tu arquitectura usando **lenguaje natural desde Claude Desktop o GitHub Copilot**.

```
Usuario: "¿Qué dependencias de Kafka hay configuradas?"
         ↓
Claude: "Consultando MCP Build..."
         ↓
MCP Build: Parsea pom.xml
         ↓
Claude: "He encontrado spring-kafka versión [...]"
```

**Disfruta el poder de consultar tu stack técnico con IA.** 🚀


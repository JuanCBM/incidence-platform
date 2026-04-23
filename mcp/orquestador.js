#!/usr/bin/env node

/**
 * Orquestador MCP Central — Versión Completa y Funcional
 *
 * Este archivo es el PUNTO DE ENTRADA ÚNICO que se registra en
 * claude_desktop_config.json. Nunca se modifica al agregar nuevos MCPs.
 *
 * PRINCIPIOS:
 * 1. Carga todos los MCPs especializados al iniciar (una sola vez)
 * 2. Los MCPs se comunican por stdio (stdin/stdout)
 * 3. El descubrimiento es automático escaneando la carpeta raíz
 * 4. Sin lógica de negocio — solo enrutamiento
 *
 * PROTOCOLO MCP (handshake obligatorio):
 *   orquestador → hijo: initialize
 *   hijo → orquestador: { result: { protocolVersion, capabilities } }
 *   orquestador → hijo: notifications/initialized   (notificación, sin id)
 *   orquestador → hijo: tools/list
 *   hijo → orquestador: { result: { tools: [...] } }
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { spawn } from "child_process";
import path from "path";
import { fileURLToPath } from "url";
import fs from "fs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ─────────────────────────────────────────────────────────────────────
// CONFIGURACIÓN
// ─────────────────────────────────────────────────────────────────────

// Los MCPs especializados viven en la raíz del proyecto junto al orquestador.
// Si en el futuro se mueven a una subcarpeta /mcps, basta con cambiar esta constante.
const MCPS_FOLDER = __dirname;
// Excluye archivos de test (mcp-*.test.js) — solo MCPs reales
const DISCOVERY_PATTERN = /^mcp-(?!.*\.test\.js$)([^.]+)\.js$/;

// Registro de MCPs especializados: nombre → { tools, callTool }
const loadedMCPs = new Map();

// ─────────────────────────────────────────────────────────────────────
// DESCUBRIMIENTO Y CARGA DE MCPs
// ─────────────────────────────────────────────────────────────────────

function discoverMCPs() {
  console.error("[Orquestador] 🔍 Descubriendo MCPs...");

  if (!fs.existsSync(MCPS_FOLDER)) {
    console.error(`[Orquestador] ⚠️  Carpeta no encontrada: ${MCPS_FOLDER}`);
    return [];
  }

  const files = fs.readdirSync(MCPS_FOLDER);
  const mcpFiles = files.filter((f) => DISCOVERY_PATTERN.test(f));

  console.error(`[Orquestador] ✓ Encontrados ${mcpFiles.length} MCPs: ${mcpFiles.join(", ")}`);

  return mcpFiles.map((f) => ({
    filename: f,
    path: path.join(MCPS_FOLDER, f),
    name: f.replace(DISCOVERY_PATTERN, "$1"),
  }));
}

/**
 * Arranca un proceso MCP hijo, ejecuta el handshake completo del protocolo
 * y devuelve un objeto con las tools disponibles y una función callTool.
 *
 * El handshake correcto es:
 *   initialize → (esperar respuesta) → notifications/initialized → tools/list
 */
async function bootMCP(mcpPath, mcpName) {
  return new Promise((resolve, reject) => {
    console.error(`[Orquestador] 🚀 Iniciando: ${mcpName}`);

    const childProcess = spawn("node", [mcpPath], {
      stdio: ["pipe", "pipe", "inherit"],
      env: { ...process.env },
    });

    let responseBuffer = "";
    let nextId = 1;

    // Map de id → { resolve, reject, timeoutHandle }
    const pendingRequests = new Map();

    // ── Dispatcher central ──────────────────────────────────────────
    // Un único listener en stdout maneja TODAS las respuestas del hijo.
    // Así evitamos race conditions por múltiples listeners.
    childProcess.stdout.on("data", (data) => {
      responseBuffer += data.toString();
      const parts = responseBuffer.split("\n");
      responseBuffer = parts.pop(); // dejar lo incompleto para la siguiente vez

      for (const part of parts) {
        if (!part.trim()) continue;
        try {
          const msg = JSON.parse(part);
          // Solo procesar mensajes con id (respuestas a requests)
          if (msg.id !== undefined && pendingRequests.has(msg.id)) {
            const pending = pendingRequests.get(msg.id);
            pendingRequests.delete(msg.id);
            clearTimeout(pending.timeoutHandle);
            if (msg.error) {
              pending.reject(new Error(msg.error.message || JSON.stringify(msg.error)));
            } else {
              pending.resolve(msg.result);
            }
          }
        } catch (e) {
          console.error(`[Orquestador] Error parseando respuesta de ${mcpName}:`, e.message);
        }
      }
    });

    childProcess.on("error", (err) => {
      console.error(`[Orquestador] ✗ Error de proceso en ${mcpName}:`, err.message);
      reject(err);
    });

    childProcess.on("exit", (code) => {
      if (code !== 0 && code !== null) {
        console.error(`[Orquestador] ✗ ${mcpName} terminó con código ${code}`);
      }
    });

    // ── Helpers de comunicación ──────────────────────────────────────

    /**
     * Envía una request JSON-RPC y devuelve una Promise con el resultado.
     */
    function sendRequest(method, params = {}, timeoutMs = 10000) {
      const id = nextId++;
      return new Promise((res, rej) => {
        const timeoutHandle = setTimeout(() => {
          pendingRequests.delete(id);
          rej(new Error(`Timeout (${timeoutMs}ms) esperando respuesta a "${method}" de ${mcpName}`));
        }, timeoutMs);

        pendingRequests.set(id, { resolve: res, reject: rej, timeoutHandle });

        const message = JSON.stringify({ jsonrpc: "2.0", id, method, params });
        childProcess.stdin.write(message + "\n");
      });
    }

    /**
     * Envía una notificación JSON-RPC (sin id, sin esperar respuesta).
     */
    function sendNotification(method, params = {}) {
      const message = JSON.stringify({ jsonrpc: "2.0", method, params });
      childProcess.stdin.write(message + "\n");
    }

    // ── Handshake y bootstrap ────────────────────────────────────────

    // Dar 200ms al proceso para que arranque antes de hablar con él
    setTimeout(async () => {
      try {
        // Paso 1: initialize (obligatorio según especificación MCP)
        await sendRequest("initialize", {
          protocolVersion: "2024-11-05",
          capabilities: { tools: {} },
          clientInfo: { name: "orquestador", version: "1.0.0" },
        });

        // Paso 2: notificación initialized (el hijo espera esto antes de aceptar tools/list)
        sendNotification("notifications/initialized");

        // Paso 3: pedir la lista de tools
        const listResult = await sendRequest("tools/list");
        const tools = listResult?.tools || [];

        console.error(
          `[Orquestador] ✓ MCP cargado: ${mcpName} (${tools.length} tools: ${tools.map((t) => t.name).join(", ")})`
        );

        // ── callTool: función reutilizable para delegar ejecuciones ──
        function callTool(toolName, args) {
          console.error(`[Orquestador] → Delegando ${toolName} a ${mcpName}`);
          return sendRequest(
            "tools/call",
            { name: toolName, arguments: args || {} },
            30000 // 30s timeout para ejecución de tools
          );
        }

        resolve({ name: mcpName, tools, callTool });
      } catch (err) {
        console.error(`[Orquestador] ✗ Fallo en bootstrap de ${mcpName}:`, err.message);
        reject(err);
      }
    }, 200);
  });
}

/**
 * Carga todos los MCPs descubiertos en paralelo.
 * Los fallos individuales no bloquean al resto.
 */
async function loadAllMCPs() {
  const discovered = discoverMCPs();

  if (discovered.length === 0) {
    console.error("[Orquestador] ⚠️  No se encontraron archivos mcp-*.js");
    return;
  }

  const results = await Promise.all(
    discovered.map((mcp) =>
      bootMCP(mcp.path, mcp.name).catch((err) => {
        console.error(`[Orquestador] ✗ No se pudo cargar ${mcp.name}: ${err.message}`);
        return null;
      })
    )
  );

  for (const result of results) {
    if (result) {
      loadedMCPs.set(result.name, result);
    }
  }

  console.error(
    `[Orquestador] ✓ ${loadedMCPs.size}/${discovered.length} MCPs cargados correctamente`
  );
}

// ─────────────────────────────────────────────────────────────────────
// SERVIDOR MCP DEL ORQUESTADOR
// ─────────────────────────────────────────────────────────────────────

const server = new Server(
  { name: "incidence-platform-orchestrator", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

/**
 * ListTools: agrega las tools de todos los MCPs cargados.
 */
server.setRequestHandler(ListToolsRequestSchema, async () => {
  const allTools = [];

  for (const mcp of loadedMCPs.values()) {
    for (const tool of mcp.tools) {
      allTools.push(tool);
    }
  }

  console.error(`[Orquestador] 📋 Enumerando ${allTools.length} tools totales`);
  return { tools: allTools };
});

/**
 * CallTool: enruta la llamada al MCP especializado que expone esa tool.
 * El resultado del hijo se reenvía tal cual — sin doble serialización.
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  console.error(`[Orquestador] 🔧 Tool solicitada: ${name}`);

  // Encontrar el MCP que tiene esta tool
  let targetMCP = null;
  for (const mcp of loadedMCPs.values()) {
    if (mcp.tools.some((t) => t.name === name)) {
      targetMCP = mcp;
      break;
    }
  }

  if (!targetMCP) {
    return {
      content: [
        {
          type: "text",
          text: `Tool desconocida: "${name}". Ningún MCP la proporciona.\nMCPs cargados: ${[...loadedMCPs.keys()].join(", ")}`,
        },
      ],
      isError: true,
    };
  }

  try {
    // El hijo devuelve directamente { content: [...], isError?: boolean }
    const result = await targetMCP.callTool(name, args);
    console.error(`[Orquestador] ← Respuesta recibida de ${targetMCP.name} para ${name}`);
    return result;
  } catch (error) {
    console.error(`[Orquestador] ✗ Error ejecutando ${name}:`, error.message);
    return {
      content: [
        {
          type: "text",
          text: `Error ejecutando "${name}" en ${targetMCP.name}: ${error.message}`,
        },
      ],
      isError: true,
    };
  }
});

// ─────────────────────────────────────────────────────────────────────
// INICIALIZACIÓN
// ─────────────────────────────────────────────────────────────────────

async function main() {
  console.error("");
  console.error("╔═══════════════════════════════════════════════════════════════════╗");
  console.error("║                  ORQUESTADOR MCP v1.0                            ║");
  console.error("║              Incidence Platform — Punto de entrada único          ║");
  console.error("║                                                                   ║");
  console.error("║  Auto-descubre mcp-*.js en la carpeta raíz del proyecto          ║");
  console.error("║  Añadir un nuevo MCP = soltar mcp-nuevo.js. Sin más cambios.     ║");
  console.error("║                                                                   ║");
  console.error("║  https://modelcontextprotocol.io                                 ║");
  console.error("╚═══════════════════════════════════════════════════════════════════╝");
  console.error("");

  // Cargar todos los MCPs y hacer el handshake con cada uno
  await loadAllMCPs();

  console.error("[Orquestador] 📡 Iniciando servidor stdio...");

  const transport = new StdioServerTransport();
  await server.connect(transport);

  console.error("[Orquestador] ✓ Conectado. Servidor activo.");
}

main().catch((error) => {
  console.error("[ERROR FATAL]", error);
  process.exit(1);
});

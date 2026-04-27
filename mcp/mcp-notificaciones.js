import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

/**
 * MCP Notificaciones
 *
 * Fuente de datos: API REST del notification-service
 * Por qué esta arquitectura:
 * - El MCP es totalmente desacoplado del servicio
 * - Si la API cambia, solo se actualiza este MCP
 * - El servicio no necesita "saber" que existe un MCP consultándolo
 * - El MCP puede implementar reintentos, cachés locales, etc.
 */

// Configuración
const NOTIFICATION_SERVICE_URL =
  process.env.NOTIFICATION_SERVICE_URL || "http://127.0.0.1:8081";

const REQUEST_TIMEOUT = 10000; // 10 segundos

/**
 * Tool 1: obtener_notificaciones
 * Consulta el endpoint GET /notificaciones del notification-service
 */
async function obtenerNotificaciones() {
  try {
    const response = await fetch(
      `${NOTIFICATION_SERVICE_URL}/notificaciones`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
        signal: AbortSignal.timeout(REQUEST_TIMEOUT),
      }
    );

    if (!response.ok) {
      return `notification-service respondió con estado ${response.status}`;
    }

    const data = await response.json();
    return data;
  } catch (error) {
    // Regla: Nunca devuelvas stack trace. El modelo de IA necesita texto legible
    if (error.name === "AbortError") {
      return `notification-service no disponible: timeout después de ${REQUEST_TIMEOUT / 1000}s`;
    }
    return `notification-service no disponible: ${error.message}`;
  }
}

/**
 * Tool 2: obtener_estado_servicio
 * Devuelve información de estado consultando el endpoint
 */
async function obtenerEstadoServicio() {
  try {
    const response = await fetch(
      `${NOTIFICATION_SERVICE_URL}/notificaciones`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
        signal: AbortSignal.timeout(REQUEST_TIMEOUT),
      }
    );

    if (!response.ok) {
      return {
        estado: "ERROR",
        razon: `Endpoint devolvió status ${response.status}`,
        disponible: false,
      };
    }

    const data = await response.json();

    if (!data.estadoServicio) {
      return {
        estado: "DESCONOCIDO",
        razon: "Respuesta no contiene estadoServicio",
        disponible: true,
      };
    }

    return {
      estado: data.estadoServicio.estado || "OK",
      totalNotificacionesProcesadas: data.estadoServicio.totalNotificacionesProcesadas || 0,
      ultimaActualizacion: data.estadoServicio.ultimaActualizacion || null,
      disponible: true,
    };
  } catch (error) {
    if (error.name === "AbortError") {
      return {
        estado: "TIMEOUT",
        razon: `No respondió en ${REQUEST_TIMEOUT / 1000}s`,
        disponible: false,
      };
    }
    return {
      estado: "ERROR",
      razon: error.message,
      disponible: false,
    };
  }
}

// ─────────────────────────────────────────────────────────────────────
// Inicialización del servidor MCP
// ─────────────────────────────────────────────────────────────────────

const server = new Server(
  { name: "mcp-notificaciones", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

// Registrar tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "obtener_notificaciones",
      description:
        "Devuelve la lista de las últimas 50 notificaciones procesadas por el notification-service, incluyendo evento, mensaje y fecha de recepción",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "obtener_estado_servicio",
      description:
        "Devuelve el estado actual del notification-service: si está disponible, cuántas notificaciones ha procesado en total, y cuándo fue la última actualización",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
  ],
}));

// Manejar llamadas a tools
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    let result;

    switch (name) {
      case "obtener_notificaciones":
        result = await obtenerNotificaciones();
        break;

      case "obtener_estado_servicio":
        result = await obtenerEstadoServicio();
        break;

      default:
        return {
          content: [
            {
              type: "text",
              text: `Tool desconocida: ${name}`,
            },
          ],
          isError: true,
        };
    }

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2),
        },
      ],
    };
  } catch (error) {
    console.error(`[ERROR] En tool ${name}:`, error);
    return {
      content: [
        {
          type: "text",
          text: `Error al ejecutar ${name}: ${error.message}`,
        },
      ],
      isError: true,
    };
  }
});

// Iniciar servidor con stdio transport
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error(
    "[MCP Notificaciones] Servidor iniciado. Escuchando en stdio..."
  );
  console.error(`[MCP Notificaciones] Consultará: ${NOTIFICATION_SERVICE_URL}`);
}

main().catch((error) => {
  console.error("[ERROR] Fallo al iniciar servidor:", error);
  process.exit(1);
});

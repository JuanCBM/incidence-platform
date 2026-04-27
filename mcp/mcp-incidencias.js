import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import fs from "fs";
import path from "path";
import yaml from "yaml";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Configuración
const OPENAPI_PATH = path.resolve(
  __dirname,
  "../incidence-service/src/main/resources/openapi.yaml"
);

let cachedSpec = null;

/**
 * Parseador de OpenAPI al iniciar
 * Por qué: Según las reglas, parseamos UNA SOLA VEZ al iniciar, no en cada llamada
 */
function loadOpenApiSpec() {
  console.error("[MCP Incidencias] Parseando openapi.yaml...");

  if (!fs.existsSync(OPENAPI_PATH)) {
    console.error(
      `[ERROR] openapi.yaml no encontrado en: ${OPENAPI_PATH}`
    );
    process.exit(1);
  }

  try {
    const content = fs.readFileSync(OPENAPI_PATH, "utf-8");
    const spec = yaml.parse(content);

    if (!spec || !spec.paths) {
      console.error("[ERROR] openapi.yaml no tiene estructura válida");
      process.exit(1);
    }

    console.error("[MCP Incidencias] ✓ openapi.yaml parseado correctamente");
    return spec;
  } catch (error) {
    console.error(`[ERROR] Fallo al parsear openapi.yaml: ${error.message}`);
    process.exit(1);
  }
}

/**
 * Tool 1: listar_endpoints
 * Devuelve todos los endpoints del API con sus métodos HTTP
 */
function listarEndpoints() {
  const endpoints = [];

  for (const [pathStr, pathItem] of Object.entries(cachedSpec.paths)) {
    // Ignorar parámetros de OpenAPI (comienzan con x-)
    const methods = Object.keys(pathItem)
      .filter((key) => !key.startsWith("x-") && !key.startsWith("parameters"))
      .filter((key) => ["get", "post", "put", "delete", "patch"].includes(key.toLowerCase()));

    for (const method of methods) {
      const operation = pathItem[method];
      endpoints.push({
        method: method.toUpperCase(),
        path: pathStr,
        summary: operation.summary || "(sin descripción)",
        operationId: operation.operationId || "N/A",
      });
    }
  }

  return {
    total: endpoints.length,
    endpoints: endpoints.sort((a, b) => a.path.localeCompare(b.path)),
  };
}

/**
 * Tool 2: obtener_schema_endpoint
 * Devuelve el schema de request/response para un endpoint específico
 */
function obtenerSchemaEndpoint(endpoint, method) {
  const pathItem = cachedSpec.paths[endpoint];

  if (!pathItem) {
    return `El endpoint "${endpoint}" no existe en el openapi.yaml`;
  }

  const operation = pathItem[method.toLowerCase()];
  if (!operation) {
    return `El método ${method.toUpperCase()} no existe para el endpoint "${endpoint}"`;
  }

  const result = {
    endpoint: endpoint,
    method: method.toUpperCase(),
    summary: operation.summary,
    requestBody: null,
    responses: {},
  };

  // Extraer request body
  if (operation.requestBody && operation.requestBody.content) {
    const jsonContent = operation.requestBody.content["application/json"];
    if (jsonContent) {
      result.requestBody = jsonContent.schema || jsonContent;
    }
  }

  // Extraer respuestas
  if (operation.responses) {
    for (const [statusCode, response] of Object.entries(operation.responses)) {
      if (response.content && response.content["application/json"]) {
        result.responses[statusCode] = response.content["application/json"].schema;
      } else {
        result.responses[statusCode] = response.description;
      }
    }
  }

  return result;
}

/**
 * Tool 3: listar_codigos_error
 * Devuelve códigos de error definidos en los endpoints
 */
function listarCodigosError() {
  const errors = new Set();

  for (const pathItem of Object.values(cachedSpec.paths)) {
    for (const operation of Object.values(pathItem).filter((v) => v && typeof v === 'object' && v.responses)) {
      for (const statusCode of Object.keys(operation.responses)) {
        // Consideramos códigos 4xx y 5xx como errores
        if (statusCode.match(/^[45]\d{2}$/)) {
          errors.add(parseInt(statusCode));
        }
      }
    }
  }

  return {
    codigosError: Array.from(errors).sort((a, b) => a - b),
    descripcion: "Códigos HTTP de error encontrados en el OpenAPI spec",
  };
}

// ─────────────────────────────────────────────────────────────────────
// Inicialización del servidor MCP
// ─────────────────────────────────────────────────────────────────────

const server = new Server(
  { name: "mcp-incidencias", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

// Cargar spec al iniciar
cachedSpec = loadOpenApiSpec();

// Registrar tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "listar_endpoints",
      description:
        "Devuelve todos los endpoints disponibles del incidence-service con sus métodos HTTP (GET, POST, PUT, DELETE)",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "obtener_schema_endpoint",
      description:
        "Devuelve el schema de request y response para un endpoint específico. Útil para entender qué datos enviar y qué recibirás",
      inputSchema: {
        type: "object",
        properties: {
          endpoint: {
            type: "string",
            description:
              'El path del endpoint (ej: "/usuarios", "/incidencias/{id}")',
          },
          method: {
            type: "string",
            description: "Método HTTP (GET, POST, PUT, DELETE, PATCH)",
          },
        },
        required: ["endpoint", "method"],
      },
    },
    {
      name: "listar_codigos_error",
      description:
        "Devuelve los códigos HTTP de error (4xx, 5xx) que pueden devolver los endpoints del incidence-service",
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
      case "listar_endpoints":
        result = listarEndpoints();
        break;

      case "obtener_schema_endpoint":
        result = obtenerSchemaEndpoint(args.endpoint, args.method);
        break;

      case "listar_codigos_error":
        result = listarCodigosError();
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
    "[MCP Incidencias] Servidor iniciado. Escuchando en stdio..."
  );
}

main().catch((error) => {
  console.error("[ERROR] Fallo al iniciar servidor:", error);
  process.exit(1);
});

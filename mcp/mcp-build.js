import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import fs from "fs";
import path from "path";
import xml2js from "xml2js";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Configuración de pom.xml
const POM_PATHS = {
  incidencias: path.resolve(__dirname, "../incidence-service/pom.xml"),
  notificaciones: path.resolve(__dirname, "../notification-service/pom.xml"),
};

let cachedPoms = {};

/**
 * Parseador de pom.xml al iniciar
 * Por qué: Según las reglas, parseamos UNA SOLA VEZ al iniciar
 */
async function loadPomFiles() {
  console.error("[MCP Build] Parseando pom.xml de microservicios...");

  const parser = new xml2js.Parser({ explicitArray: false });

  for (const [nombre, rutaPom] of Object.entries(POM_PATHS)) {
    if (!fs.existsSync(rutaPom)) {
      console.error(`[ERROR] pom.xml no encontrado en: ${rutaPom}`);
      process.exit(1);
    }

    try {
      const content = fs.readFileSync(rutaPom, "utf-8");
      const parsed = await parser.parseStringPromise(content);

      if (!parsed || !parsed.project) {
        console.error(`[ERROR] pom.xml no tiene estructura válida: ${nombre}`);
        process.exit(1);
      }

      cachedPoms[nombre] = parsed.project;
      console.error(`[MCP Build] ✓ ${nombre} parseado`);
    } catch (error) {
      console.error(`[ERROR] Fallo al parsear pom.xml (${nombre}): ${error.message}`);
      process.exit(1);
    }
  }

  console.error("[MCP Build] ✓ Todos los pom.xml parseados correctamente");
}

/**
 * Tool 1: obtener_version_java
 */
function obtenerVersionJava() {
  const versions = {};

  for (const [nombre, pom] of Object.entries(cachedPoms)) {
    const properties = pom.properties || {};
    versions[nombre] = {
      javaVersion: properties["java.version"] || "No especificada",
    };
  }

  return versions;
}

/**
 * Tool 2: obtener_version_spring
 */
function obtenerVersionSpring() {
  const versions = {};

  for (const [nombre, pom] of Object.entries(cachedPoms)) {
    let springVersion = "No configurado";

    // Buscar en parent
    if (pom.parent && pom.parent.version) {
      springVersion = pom.parent.version;
    }

    versions[nombre] = {
      springBootVersion: springVersion,
    };
  }

  return versions;
}

/**
 * Tool 3: listar_dependencias
 * Devuelve dependencias principales (JPA, Kafka, Flyway, etc.)
 */
function listarDependencias() {
  const resultado = {};

  for (const [nombre, pom] of Object.entries(cachedPoms)) {
    const deps = [];
    const dependencies = pom.dependencies;

    if (dependencies && dependencies.dependency) {
      const depList = Array.isArray(dependencies.dependency)
        ? dependencies.dependency
        : [dependencies.dependency];

      for (const dep of depList) {
        // Filtrar principales
        if (
          dep.groupId &&
          dep.artifactId &&
          (dep.groupId.includes("spring") ||
            dep.groupId.includes("kafka") ||
            dep.groupId.includes("flyway") ||
            dep.groupId.includes("postgres") ||
            dep.groupId.includes("javax") ||
            dep.groupId.includes("hibernate") ||
            dep.artifactId.includes("jpa") ||
            dep.artifactId.includes("mail"))
        ) {
          deps.push({
            groupId: dep.groupId,
            artifactId: dep.artifactId,
            version: dep.version || "(heredada del parent)",
            scope: dep.scope || "compile",
          });
        }
      }
    }

    resultado[nombre] = {
      total: deps.length,
      dependencias: deps.sort((a, b) =>
        `${a.groupId}:${a.artifactId}`.localeCompare(`${b.groupId}:${b.artifactId}`)
      ),
    };
  }

  return resultado;
}

/**
 * Tool 4: listar_plugins_maven
 */
function listarPluginsMaven() {
  const resultado = {};

  for (const [nombre, pom] of Object.entries(cachedPoms)) {
    const plugins = [];
    const build = pom.build;

    if (build && build.plugins && build.plugins.plugin) {
      const pluginList = Array.isArray(build.plugins.plugin)
        ? build.plugins.plugin
        : [build.plugins.plugin];

      for (const plugin of pluginList) {
        plugins.push({
          groupId: plugin.groupId || "org.apache.maven.plugins",
          artifactId: plugin.artifactId,
          version: plugin.version || "(default)",
        });
      }
    }

    resultado[nombre] = {
      total: plugins.length,
      plugins: plugins,
    };
  }

  return resultado;
}

// ─────────────────────────────────────────────────────────────────────
// Inicialización del servidor MCP
// ─────────────────────────────────────────────────────────────────────

const server = new Server(
  { name: "mcp-build", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

// Cargar poms al iniciar
await loadPomFiles();

// Registrar tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "obtener_version_java",
      description:
        "Devuelve la versión de Java configurada en los pom.xml de ambos microservicios",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "obtener_version_spring",
      description:
        "Devuelve la versión de Spring Boot configurada en los pom.xml de ambos microservicios",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "listar_dependencias",
      description:
        "Devuelve las dependencias principales configuradas (JPA, Kafka, Flyway, PostgreSQL, Mail, etc.) en ambos microservicios",
      inputSchema: {
        type: "object",
        properties: {},
        required: [],
      },
    },
    {
      name: "listar_plugins_maven",
      description:
        "Devuelve los plugins Maven configurados en ambos microservicios (compilador, generador de código, etc.)",
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
      case "obtener_version_java":
        result = obtenerVersionJava();
        break;

      case "obtener_version_spring":
        result = obtenerVersionSpring();
        break;

      case "listar_dependencias":
        result = listarDependencias();
        break;

      case "listar_plugins_maven":
        result = listarPluginsMaven();
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
  console.error("[MCP Build] Servidor iniciado. Escuchando en stdio...");
}

main().catch((error) => {
  console.error("[ERROR] Fallo al iniciar servidor:", error);
  process.exit(1);
});

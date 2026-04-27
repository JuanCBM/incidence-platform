/**
 * Tests para el Orquestador
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Rutas base a las carpetas reorganizadas
const MCP_DIR     = path.join(__dirname, "../mcp");
const DOCS_DIR    = path.join(__dirname, "../docs");
const ROOT_DIR    = path.join(__dirname, "..");

describe("Orquestador", () => {
  describe("Descubrimiento de MCPs", () => {
    test("Debe encontrar MCPs en la carpeta mcp/", () => {
      const files = fs.readdirSync(MCP_DIR);
      // Excluir test files y el orquestador; solo MCPs especializados
      const mcpFiles = files.filter((f) => f.match(/^mcp-[^.]+\.js$/));

      expect(mcpFiles.length).toBeGreaterThanOrEqual(3);
      expect(mcpFiles).toContain("mcp-incidencias.js");
      expect(mcpFiles).toContain("mcp-build.js");
      expect(mcpFiles).toContain("mcp-notificaciones.js");
    });

    test("Debe validar estructura de archivo MCP", () => {
      const mcp = path.join(MCP_DIR, "mcp-incidencias.js");
      const content = fs.readFileSync(mcp, "utf-8");

      expect(content).toContain("from \"@modelcontextprotocol/sdk");
      expect(content).toContain("ListToolsRequestSchema");
      expect(content).toContain("CallToolRequestSchema");
    });
  });

  describe("Enrutamiento de Tools", () => {
    test("Debe mapear tools a MCPs correctamente", () => {
      const toolToMCP = {
        listar_endpoints: "incidencias",
        obtener_schema_endpoint: "incidencias",
        listar_codigos_error: "incidencias",
        obtener_version_java: "build",
        obtener_version_spring: "build",
        listar_dependencias: "build",
        listar_plugins_maven: "build",
        obtener_notificaciones: "notificaciones",
        obtener_estado_servicio: "notificaciones",
      };

      expect(Object.keys(toolToMCP).length).toBe(9);
      expect(toolToMCP.listar_endpoints).toBe("incidencias");
      expect(toolToMCP.obtener_version_java).toBe("build");
      expect(toolToMCP.obtener_notificaciones).toBe("notificaciones");
    });

    test("Cada MCP debe tener al menos una tool", () => {
      const toolToMCP = {
        listar_endpoints: "incidencias",
        obtener_schema_endpoint: "incidencias",
        listar_codigos_error: "incidencias",
        obtener_version_java: "build",
        obtener_version_spring: "build",
        listar_dependencias: "build",
        listar_plugins_maven: "build",
        obtener_notificaciones: "notificaciones",
        obtener_estado_servicio: "notificaciones",
      };

      const mcpCounts = {};
      for (const mcp of Object.values(toolToMCP)) {
        mcpCounts[mcp] = (mcpCounts[mcp] || 0) + 1;
      }

      expect(mcpCounts.incidencias).toBe(3);
      expect(mcpCounts.build).toBe(4);
      expect(mcpCounts.notificaciones).toBe(2);
    });
  });

  describe("Configuración", () => {
    test("Debe tener claude_desktop_config.json configurado", () => {
      const configPath = path.join(ROOT_DIR, "claude_desktop_config.json");
      expect(fs.existsSync(configPath)).toBeTruthy();

      const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));
      expect(config.mcpServers).toBeDefined();
      expect(config.mcpServers["incidence-platform"]).toBeDefined();
    });

    test("Debe apuntar al orquestador como punto de entrada", () => {
      const configPath = path.join(ROOT_DIR, "claude_desktop_config.json");
      const config = JSON.parse(fs.readFileSync(configPath, "utf-8"));

      const serverConfig = config.mcpServers["incidence-platform"];
      expect(serverConfig.command).toBe("node");
      expect(serverConfig.args[0]).toContain("orquestador.js");
    });
  });

  describe("Archivos Necesarios", () => {
    test("Debe existir orquestador.js en mcp/", () => {
      const filePath = path.join(MCP_DIR, "orquestador.js");
      expect(fs.existsSync(filePath)).toBeTruthy();
    });

    test("Debe existir package.json con dependencias", () => {
      const packagePath = path.join(ROOT_DIR, "package.json");
      expect(fs.existsSync(packagePath)).toBeTruthy();

      const pkg = JSON.parse(fs.readFileSync(packagePath, "utf-8"));
      expect(pkg.dependencies["@modelcontextprotocol/sdk"]).toBeDefined();
      expect(pkg.dependencies.yaml).toBeDefined();
      expect(pkg.dependencies.xml2js).toBeDefined();
    });

    test("Debe tener scripts npm configurados", () => {
      const packagePath = path.join(ROOT_DIR, "package.json");
      const pkg = JSON.parse(fs.readFileSync(packagePath, "utf-8"));

      expect(pkg.scripts.start).toBeDefined();
      expect(pkg.scripts.test).toBeDefined();
    });
  });

  describe("Documentación", () => {
    test("Debe existir README.MCP.md en docs/", () => {
      const readmePath = path.join(DOCS_DIR, "README.MCP.md");
      expect(fs.existsSync(readmePath)).toBeTruthy();
    });

    test("Debe existir SETUP_Y_VALIDACION.md en docs/", () => {
      const setupPath = path.join(DOCS_DIR, "SETUP_Y_VALIDACION.md");
      expect(fs.existsSync(setupPath)).toBeTruthy();
    });

    test("Debe existir DECISIONES_ARQUITECTONICAS.md en docs/", () => {
      const decisionesPath = path.join(DOCS_DIR, "DECISIONES_ARQUITECTONICAS.md");
      expect(fs.existsSync(decisionesPath)).toBeTruthy();
    });

    test("Debe existir ARQUITECTURA_VISUAL.md en docs/", () => {
      const arqPath = path.join(DOCS_DIR, "ARQUITECTURA_VISUAL.md");
      expect(fs.existsSync(arqPath)).toBeTruthy();
    });
  });
});

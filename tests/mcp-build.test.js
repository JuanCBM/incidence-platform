/**
 * Tests para MCP Build
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import xml2js from "xml2js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

describe("MCP Build", () => {
  let poms = {};
  const parser = new xml2js.Parser({ explicitArray: false });

  beforeAll(async () => {
    const pomPaths = {
      incidencias: path.resolve(__dirname, "../incidence-service/pom.xml"),
      notificaciones: path.resolve(__dirname, "../notification-service/pom.xml"),
    };

    for (const [nombre, rutaPom] of Object.entries(pomPaths)) {
      if (!fs.existsSync(rutaPom)) {
        throw new Error(`pom.xml no encontrado en ${rutaPom}`);
      }

      const content = fs.readFileSync(rutaPom, "utf-8");
      const parsed = await parser.parseStringPromise(content);
      poms[nombre] = parsed.project;
    }
  });

  describe("Parseo de pom.xml", () => {
    test("Debe cargar ambos pom.xml correctamente", () => {
      expect(poms.incidencias).toBeDefined();
      expect(poms.notificaciones).toBeDefined();
    });

    test("Debe contener propiedades de Java", () => {
      const props = poms.incidencias.properties;
      expect(props).toBeDefined();
      expect(props["java.version"]).toBeDefined();
    });
  });

  describe("Tool: obtener_version_java", () => {
    test("Debe extraer versión de Java de ambos servicios", () => {
      const versions = {};

      for (const [nombre, pom] of Object.entries(poms)) {
        const properties = pom.properties || {};
        versions[nombre] = properties["java.version"] || "No especificada";
      }

      expect(versions.incidencias).toBeDefined();
      expect(versions.notificaciones).toBeDefined();
      expect(versions.incidencias).toBe("17");
    });
  });

  describe("Tool: obtener_version_spring", () => {
    test("Debe extraer versión de Spring Boot", () => {
      const versions = {};

      for (const [nombre, pom] of Object.entries(poms)) {
        let springVersion = "No configurado";

        if (pom.parent && pom.parent.version) {
          springVersion = pom.parent.version;
        }

        versions[nombre] = springVersion;
      }

      expect(versions.incidencias).not.toBe("No configurado");
      expect(versions.incidencias).toMatch(/3\.\d+\.\d+/);
    });
  });

  describe("Tool: listar_dependencias", () => {
    test("Debe extraer dependencias de los servicios", () => {
      const dependencies = {};

      for (const [nombre, pom] of Object.entries(poms)) {
        const deps = [];
        const depList = pom.dependencies?.dependency
          ? Array.isArray(pom.dependencies.dependency)
            ? pom.dependencies.dependency
            : [pom.dependencies.dependency]
          : [];

        for (const dep of depList) {
          if (dep.groupId || dep.artifactId) {
            deps.push({
              groupId: dep.groupId,
              artifactId: dep.artifactId,
            });
          }
        }

        dependencies[nombre] = deps;
      }

      expect(dependencies.incidencias.length).toBeGreaterThan(0);
      expect(dependencies.notificaciones.length).toBeGreaterThan(0);
    });

    test("Debe incluir dependencias principales", () => {
      const incidenciasDeps = poms.incidencias.dependencies?.dependency
        ? Array.isArray(poms.incidencias.dependencies.dependency)
          ? poms.incidencias.dependencies.dependency
          : [poms.incidencias.dependencies.dependency]
        : [];

      const hasSpring = incidenciasDeps.some((d) =>
        d.groupId?.includes("spring")
      );
      const hasPostgres = incidenciasDeps.some((d) =>
        d.groupId?.includes("postgresql") || d.artifactId?.includes("postgresql")
      );

      expect(hasSpring || hasPostgres).toBeTruthy();
    });
  });

  describe("Tool: listar_plugins_maven", () => {
    test("Debe extraer plugins Maven", () => {
      const incidenciasPom = poms.incidencias;
      const plugins = [];

      const pluginList = incidenciasPom.build?.plugins?.plugin
        ? Array.isArray(incidenciasPom.build.plugins.plugin)
          ? incidenciasPom.build.plugins.plugin
          : [incidenciasPom.build.plugins.plugin]
        : [];

      for (const plugin of pluginList) {
        if (plugin.artifactId) {
          plugins.push({
            artifactId: plugin.artifactId,
          });
        }
      }

      expect(plugins.length).toBeGreaterThan(0);
    });
  });
});

// Variable global pom para algunos tests
let pom = {};

/**
 * Tests para MCP Incidencias
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import yaml from "yaml";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

describe("MCP Incidencias", () => {
  let spec = null;

  beforeAll(() => {
    const OPENAPI_PATH = path.resolve(
      __dirname,
      "../incidence-service/src/main/resources/openapi.yaml"
    );

    if (!fs.existsSync(OPENAPI_PATH)) {
      throw new Error(`openapi.yaml no encontrado en ${OPENAPI_PATH}`);
    }

    const content = fs.readFileSync(OPENAPI_PATH, "utf-8");
    spec = yaml.parse(content);
  });

  describe("Parseo de openapi.yaml", () => {
    test("Debe cargar el archivo openapi.yaml correctamente", () => {
      expect(spec).toBeDefined();
      expect(spec.openapi).toBe("3.0.3");
    });

    test("Debe contener paths", () => {
      expect(spec.paths).toBeDefined();
      expect(Object.keys(spec.paths).length).toBeGreaterThan(0);
    });

    test("Debe contener path /usuarios", () => {
      expect(spec.paths["/usuarios"]).toBeDefined();
    });
  });

  describe("Tool: listar_endpoints", () => {
    test("Debe extraer endpoints correctamente", () => {
      const endpoints = [];

      for (const [pathStr, pathItem] of Object.entries(spec.paths)) {
        const methods = Object.keys(pathItem)
          .filter((key) => !key.startsWith("x-") && !key.startsWith("parameters"))
          .filter((key) =>
            ["get", "post", "put", "delete", "patch"].includes(key.toLowerCase())
          );

        for (const method of methods) {
          endpoints.push({
            method: method.toUpperCase(),
            path: pathStr,
          });
        }
      }

      expect(endpoints.length).toBeGreaterThan(0);
      expect(endpoints.some((e) => e.path === "/usuarios")).toBeTruthy();
    });
  });

  describe("Tool: listar_codigos_error", () => {
    test("Debe extraer códigos de error", () => {
      const errors = new Set();

      for (const pathItem of Object.values(spec.paths)) {
        for (const operation of Object.values(pathItem).filter(
          (v) => v && typeof v === "object" && v.responses
        )) {
          for (const statusCode of Object.keys(operation.responses)) {
            if (statusCode.match(/^[45]\d{2}$/)) {
              errors.add(parseInt(statusCode));
            }
          }
        }
      }

      expect(errors.size).toBeGreaterThan(0);
      expect(errors.has(400)).toBeTruthy();
    });
  });
});

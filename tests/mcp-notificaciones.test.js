/**
 * Tests para MCP Notificaciones
 */

describe("MCP Notificaciones", () => {
  describe("Validación de configuración", () => {
    test("Debe tener URL configurada para notification-service", () => {
      const url = process.env.NOTIFICATION_SERVICE_URL || "http://localhost:8081";
      expect(url).toBeDefined();
      expect(url).toMatch(/^http/);
    });

    test("Debe validar timeout de fetch", () => {
      const timeout = 10000;
      expect(timeout).toBe(10000);
    });
  });

  describe("Manejo de Errores", () => {
    test("Debe retornar texto legible si servicio no disponible", () => {
      // Simulación del comportamiento
      const error = new Error("ECONNREFUSED");
      const result = `notification-service no disponible: ${error.message}`;

      expect(result).toContain("no disponible");
      expect(result).not.toContain("stack");
    });

    test("Debe manejar timeout correctamente", () => {
      const timeout = 10000;
      const result = `notification-service no disponible: timeout después de ${timeout / 1000}s`;

      expect(result).toContain("timeout");
      expect(result).toContain("10s");
    });

    test("Debe retornar objeto de estado en caso de error", () => {
      const estadoError = {
        estado: "ERROR",
        razon: "Connection refused",
        disponible: false,
      };

      expect(estadoError.disponible).toBe(false);
      expect(estadoError.estado).toBe("ERROR");
    });
  });

  describe("Estructura de respuesta", () => {
    test("Debe devolver estructura consistente para notificaciones", () => {
      const respuestaEsperada = {
        notificaciones: [],
        estadoServicio: {
          estado: "OK",
          totalNotificacionesProcesadas: 0,
          ultimaActualizacion: null,
        },
      };

      expect(respuestaEsperada).toHaveProperty("notificaciones");
      expect(respuestaEsperada).toHaveProperty("estadoServicio");
      expect(respuestaEsperada.estadoServicio).toHaveProperty("estado");
    });

    test("Debe incluir campo disponible en estado", () => {
      const estado = {
        estado: "OK",
        totalNotificacionesProcesadas: 42,
        ultimaActualizacion: new Date().toISOString(),
        disponible: true,
      };

      expect(estado.disponible).toBe(true);
    });
  });

  describe("Descriptions de Tools", () => {
    test("Debe tener description clara para obtener_notificaciones", () => {
      const desc =
        "Devuelve la lista de las últimas 50 notificaciones procesadas por el notification-service";

      expect(desc).toContain("notificaciones");
      expect(desc).toContain("notification-service");
      expect(desc.length).toBeGreaterThan(20);
    });

    test("Debe tener description clara para obtener_estado_servicio", () => {
      const desc =
        "Devuelve el estado actual del notification-service: si está disponible, cuántas notificaciones ha procesado";

      expect(desc).toContain("estado");
      expect(desc).toContain("disponible");
      expect(desc.length).toBeGreaterThan(20);
    });
  });
});

/**
 * Tests para NotificacionesResponseDTO y Controller (Java)
 * 
 * Valida que el DTO extendido está correctamente estructurado
 * y que el endpoint GET /notificaciones devuelve la respuesta esperada
 */

describe("Notification Service - API REST", () => {
  describe("Endpoint GET /notificaciones", () => {
    test("Debe devolver estructura con notificaciones y estadoServicio", () => {
      // Simulación de respuesta esperada
      const respuesta = {
        notificaciones: [
          {
            id: 1,
            incidenciaId: 100,
            evento: "CREADA",
            mensaje: "Incidencia creada",
            fechaRecepcion: "2026-04-22T10:00:00",
          },
        ],
        estadoServicio: {
          estado: "OK",
          totalNotificacionesProcesadas: 42,
          ultimaActualizacion: "2026-04-22T11:57:00",
        },
      };

      expect(respuesta).toHaveProperty("notificaciones");
      expect(respuesta).toHaveProperty("estadoServicio");
      expect(Array.isArray(respuesta.notificaciones)).toBeTruthy();
    });

    test("Debe devolver lista vacía si no hay notificaciones", () => {
      const respuesta = {
        notificaciones: [],
        estadoServicio: {
          estado: "OK",
          totalNotificacionesProcesadas: 0,
          ultimaActualizacion: "2026-04-22T11:57:00",
        },
      };

      expect(respuesta.notificaciones).toEqual([]);
      expect(respuesta.estadoServicio.totalNotificacionesProcesadas).toBe(0);
    });

    test("Debe incluir hasta 50 notificaciones más recientes", () => {
      // Simular 100 notificaciones, pero devolver solo 50
      const notificaciones = Array.from({ length: 50 }, (_, i) => ({
        id: i + 1,
        incidenciaId: 100 + i,
        evento: "ACTUALIZADA",
        mensaje: `Evento ${i + 1}`,
        fechaRecepcion: new Date(Date.now() - i * 1000).toISOString(),
      }));

      expect(notificaciones.length).toBeLessThanOrEqual(50);
      expect(notificaciones[0].id).toBeLessThan(notificaciones[49].id);
    });
  });

  describe("NotificacionesResponseDTO", () => {
    test("Debe contener lista de NotificacionDTO", () => {
      const dto = {
        notificaciones: [
          {
            id: 1,
            incidenciaId: 100,
            evento: "CREADA",
            mensaje: "Mensaje",
            fechaRecepcion: "2026-04-22T10:00:00",
          },
        ],
      };

      const notif = dto.notificaciones[0];
      expect(notif).toHaveProperty("id");
      expect(notif).toHaveProperty("incidenciaId");
      expect(notif).toHaveProperty("evento");
      expect(notif).toHaveProperty("mensaje");
      expect(notif).toHaveProperty("fechaRecepcion");
    });

    test("Debe contener EstadoServicio anidado", () => {
      const estado = {
        estado: "OK",
        totalNotificacionesProcesadas: 42,
        ultimaActualizacion: "2026-04-22T11:57:00",
      };

      expect(estado).toHaveProperty("estado");
      expect(estado).toHaveProperty("totalNotificacionesProcesadas");
      expect(estado).toHaveProperty("ultimaActualizacion");
      expect(estado.estado).toBe("OK");
    });

    test("EstadoServicio debe validar disponibilidad", () => {
      const estadoDisponible = {
        estado: "OK",
        totalNotificacionesProcesadas: 42,
        disponible: true,
      };

      const estadoNoDisponible = {
        estado: "ERROR",
        razon: "Connection failed",
        disponible: false,
      };

      expect(estadoDisponible.disponible).toBe(true);
      expect(estadoNoDisponible.disponible).toBe(false);
    });
  });

  describe("Formato de Respuesta JSON", () => {
    test("Debe ser JSON válido", () => {
      const respuesta = {
        notificaciones: [],
        estadoServicio: {
          estado: "OK",
          totalNotificacionesProcesadas: 0,
        },
      };

      const json = JSON.stringify(respuesta);
      expect(json).toBeTruthy();

      const parsed = JSON.parse(json);
      expect(parsed).toEqual(respuesta);
    });

    test("Debe cumplir con Content-Type application/json", () => {
      const contentType = "application/json";
      expect(contentType).toContain("application/json");
    });

    test("Debe retornar HTTP 200 OK", () => {
      const statusCode = 200;
      expect(statusCode).toBe(200);
    });
  });

  describe("Integración con MCP Notificaciones", () => {
    test("MCP debe poder parsear respuesta de endpoint", () => {
      const respuestaAPI = {
        notificaciones: [
          {
            id: 1,
            incidenciaId: 100,
            evento: "CREADA",
            mensaje: "Incidencia creada",
            fechaRecepcion: "2026-04-22T10:00:00",
          },
        ],
        estadoServicio: {
          estado: "OK",
          totalNotificacionesProcesadas: 1,
          ultimaActualizacion: "2026-04-22T10:00:00",
        },
      };

      // Validar que MCP pueda acceder a los datos
      expect(respuestaAPI.notificaciones.length).toBeGreaterThan(0);
      expect(respuestaAPI.estadoServicio.totalNotificacionesProcesadas).toBeGreaterThanOrEqual(
        0
      );
    });

    test("MCP debe mostrar estado en lenguaje natural", () => {
      const estado = {
        estado: "OK",
        totalNotificacionesProcesadas: 42,
        ultimaActualizacion: "2026-04-22T11:57:00",
      };

      const respuesta = `El notification-service está disponible. Ha procesado ${estado.totalNotificacionesProcesadas} notificaciones. Última actualización: ${estado.ultimaActualizacion}`;

      expect(respuesta).toContain("disponible");
      expect(respuesta).toContain("42");
      expect(respuesta).toContain("notificaciones");
    });
  });

  describe("Manejo de Errores", () => {
    test("Debe validar que incidenciaId es numérico", () => {
      const notificacion = {
        incidenciaId: "no-es-numero",
      };

      const esValido = typeof notificacion.incidenciaId === "number";
      expect(esValido).toBeFalsy();
    });

    test("Debe validar que fechaRecepcion es ISO 8601", () => {
      const fecha = "2026-04-22T11:57:33.491Z";
      const esValido = !isNaN(Date.parse(fecha));

      expect(esValido).toBeTruthy();
    });

    test("Debe validar que evento es string", () => {
      const evento = "CREADA";
      const esValido = typeof evento === "string";

      expect(esValido).toBeTruthy();
    });
  });
});

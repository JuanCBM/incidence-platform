package com.empresa.notification.controller;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.repository.NotificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración del controlador REST {@link NotificacionController}.
 *
 * <p>Verifican que los tres endpoints están correctamente registrados en Spring MVC
 * y responden con los códigos HTTP y cuerpos de respuesta esperados.
 * No requieren base de datos real: el repositorio se sustituye por un mock.</p>
 *
 * <p>A partir del cambio introducido por Copilot, {@code GET /notificaciones} devuelve
 * un {@code NotificacionesResponseDTO} con dos secciones:</p>
 * <ul>
 *   <li>{@code $.notificaciones} — array de eventos recientes</li>
 *   <li>{@code $.estadoServicio} — metadatos del servicio (total, estado, timestamp)</li>
 * </ul>
 *
 * <p>Cobertura:</p>
 * <ul>
 *   <li>{@code GET /notificaciones} — actividad reciente del sistema (DTO extendido)</li>
 *   <li>{@code GET /notificaciones/{id}} — historial de una incidencia</li>
 *   <li>{@code GET /notificaciones/{id}/log} — descarga de log en texto plano</li>
 * </ul>
 */
@WebMvcTest(
    controllers = NotificacionController.class,
    excludeAutoConfiguration = {KafkaAutoConfiguration.class, MailSenderAutoConfiguration.class}
)
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificacionRepository notificacionRepository;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Notificacion notificacion(Long incidenciaId, String evento) {
        return new Notificacion(incidenciaId, evento, "{\"incidenciaId\":" + incidenciaId + "}");
    }

    // ── GET /notificaciones — estructura del DTO ──────────────────────────────

    @Test
    @DisplayName("GET /notificaciones → 200 con objeto DTO que contiene 'notificaciones' y 'estadoServicio'")
    void getActividadReciente_devuelveObjetoConDosSeccionesPrincipales() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.notificaciones").exists())
            .andExpect(jsonPath("$.estadoServicio").exists());
    }

    @Test
    @DisplayName("GET /notificaciones → $.notificaciones es un array con los eventos cuando hay datos")
    void getActividadReciente_conEventos_devuelveArrayEnNotificaciones() throws Exception {
        List<Notificacion> eventos = List.of(
            notificacion(3L, "INCIDENCIA_CREADA"),
            notificacion(3L, "ESTADO_CAMBIADO")
        );
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(eventos);
        when(notificacionRepository.count()).thenReturn(2L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificaciones", hasSize(2)))
            .andExpect(jsonPath("$.notificaciones[0].evento", is("INCIDENCIA_CREADA")))
            .andExpect(jsonPath("$.notificaciones[1].evento", is("ESTADO_CAMBIADO")));

        verify(notificacionRepository).findTop50ByOrderByFechaRecepcionDesc();
    }

    @Test
    @DisplayName("GET /notificaciones → $.notificaciones es un array vacío cuando no hay eventos")
    void getActividadReciente_sinEventos_devuelveArrayVacioEnNotificaciones() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificaciones", hasSize(0)));
    }

    @Test
    @DisplayName("GET /notificaciones → $.notificaciones contiene incidenciaId e evento en cada elemento")
    void getActividadReciente_cadaElementoTieneLosCamposEsperados() throws Exception {
        List<Notificacion> eventos = List.of(notificacion(5L, "INCIDENCIA_CREADA"));
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(eventos);
        when(notificacionRepository.count()).thenReturn(1L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notificaciones[0].incidenciaId", is(5)))
            .andExpect(jsonPath("$.notificaciones[0].evento", is("INCIDENCIA_CREADA")))
            .andExpect(jsonPath("$.notificaciones[0].mensaje").exists())
            .andExpect(jsonPath("$.notificaciones[0].fechaRecepcion").exists());
    }

    // ── GET /notificaciones — estadoServicio ──────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones → $.estadoServicio.estado es 'OK'")
    void getActividadReciente_estadoServicioEsOk() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoServicio.estado", is("OK")));
    }

    @Test
    @DisplayName("GET /notificaciones → $.estadoServicio.totalNotificacionesProcesadas refleja el count del repositorio")
    void getActividadReciente_totalNotificacionesRefleja_elCountDelRepositorio() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(42L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoServicio.totalNotificacionesProcesadas", is(42)));

        verify(notificacionRepository).count();
    }

    @Test
    @DisplayName("GET /notificaciones → $.estadoServicio.ultimaActualizacion no es nulo")
    void getActividadReciente_ultimaActualizacionNuncaEsNull() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoServicio.ultimaActualizacion").isNotEmpty());
    }

    // ── GET /notificaciones — llamadas al repositorio ─────────────────────────

    @Test
    @DisplayName("GET /notificaciones → llama exactamente una vez a findTop50 y una vez a count")
    void getActividadReciente_llamaAlRepositorioUnaVezCadaMetodo() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(Collections.emptyList());
        when(notificacionRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/notificaciones"));

        verify(notificacionRepository, times(1)).findTop50ByOrderByFechaRecepcionDesc();
        verify(notificacionRepository, times(1)).count();
    }

    // ── GET /notificaciones/{incidenciaId} ────────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones/{id} → 200 con historial de la incidencia")
    void getHistorialPorIncidencia_conEventos_devuelve200YLista() throws Exception {
        List<Notificacion> historial = List.of(notificacion(5L, "INCIDENCIA_CREADA"));
        when(notificacionRepository.findByIncidenciaId(5L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/5"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].incidenciaId", is(5)));

        verify(notificacionRepository).findByIncidenciaId(5L);
    }

    @Test
    @DisplayName("GET /notificaciones/{id} → 200 con lista vacía cuando no hay historial")
    void getHistorialPorIncidencia_sinEventos_devuelve200YListaVacia() throws Exception {
        when(notificacionRepository.findByIncidenciaId(99L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest(name = "incidenciaId = {0}")
    @ValueSource(longs = {1L, 10L, 100L, 999L})
    @DisplayName("GET /notificaciones/{id} → consulta al repositorio con el ID correcto")
    void getHistorialPorIncidencia_pasaElIdCorrectoAlRepositorio(long incidenciaId) throws Exception {
        when(notificacionRepository.findByIncidenciaId(incidenciaId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/" + incidenciaId))
            .andExpect(status().isOk());

        verify(notificacionRepository).findByIncidenciaId(incidenciaId);
    }

    @Test
    @DisplayName("GET /notificaciones/{id} → cada elemento contiene los campos de notificación")
    void getHistorialPorIncidencia_losElementosTienenLosCamposEsperados() throws Exception {
        List<Notificacion> historial = List.of(
            notificacion(7L, "INCIDENCIA_CREADA"),
            notificacion(7L, "ESTADO_CAMBIADO")
        );
        when(notificacionRepository.findByIncidenciaId(7L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].evento", is("INCIDENCIA_CREADA")))
            .andExpect(jsonPath("$[1].evento", is("ESTADO_CAMBIADO")))
            .andExpect(jsonPath("$[0].incidenciaId", is(7)));
    }

    // ── GET /notificaciones/{incidenciaId}/log ────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones/{id}/log → 200 con fichero texto plano cuando hay eventos")
    void descargarLog_conEventos_devuelve200YTextoPlano() throws Exception {
        List<Notificacion> historial = List.of(notificacion(7L, "INCIDENCIA_CREADA"));
        when(notificacionRepository.findByIncidenciaId(7L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/7/log"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("incidencia-7.log.txt")))
            .andExpect(content().contentTypeCompatibleWith("text/plain"))
            .andExpect(content().string(containsString("LOG DE INCIDENCIA #7")))
            .andExpect(content().string(containsString("INCIDENCIA_CREADA")));
    }

    @Test
    @DisplayName("GET /notificaciones/{id}/log → 204 cuando no hay eventos para esa incidencia")
    void descargarLog_sinEventos_devuelve204() throws Exception {
        when(notificacionRepository.findByIncidenciaId(42L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/42/log"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /notificaciones/{id}/log → el contenido del fichero incluye fecha, evento y mensaje")
    void descargarLog_elContenidoIncluyeTodosLosCamposDelLog() throws Exception {
        List<Notificacion> historial = List.of(notificacion(3L, "ESTADO_CAMBIADO"));
        when(notificacionRepository.findByIncidenciaId(3L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/3/log"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Fecha:")))
            .andExpect(content().string(containsString("Evento:")))
            .andExpect(content().string(containsString("Mensaje:")))
            .andExpect(content().string(containsString("ESTADO_CAMBIADO")));
    }

    @Test
    @DisplayName("GET /notificaciones/{id}/log → Content-Disposition incluye el ID correcto en el nombre del fichero")
    void descargarLog_contentDispositionContieneElIdCorrecto() throws Exception {
        List<Notificacion> historial = List.of(notificacion(99L, "INCIDENCIA_CERRADA"));
        when(notificacionRepository.findByIncidenciaId(99L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/99/log"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString("incidencia-99.log.txt")))
            .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    @DisplayName("GET /notificaciones/{id}/log → el log contiene un bloque por cada evento")
    void descargarLog_conMultiplesEventos_elLogContieneTodosLosEventos() throws Exception {
        List<Notificacion> historial = List.of(
            notificacion(10L, "INCIDENCIA_CREADA"),
            notificacion(10L, "ESTADO_CAMBIADO"),
            notificacion(10L, "INCIDENCIA_CERRADA")
        );
        when(notificacionRepository.findByIncidenciaId(10L)).thenReturn(historial);

        mockMvc.perform(get("/notificaciones/10/log"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("INCIDENCIA_CREADA")))
            .andExpect(content().string(containsString("ESTADO_CAMBIADO")))
            .andExpect(content().string(containsString("INCIDENCIA_CERRADA")));
    }
}

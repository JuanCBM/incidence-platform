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
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
 * <p>Cobertura:</p>
 * <ul>
 *   <li>{@code GET /notificaciones} — actividad reciente del sistema</li>
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

    private Notificacion notificacion(Long id, Long incidenciaId, String evento) {
        Notificacion n = new Notificacion(incidenciaId, evento, "{\"incidenciaId\":" + incidenciaId + "}");
        return n;
    }

    // ── GET /notificaciones ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones → 200 con lista de eventos cuando hay datos")
    void getActividadReciente_conEventos_devuelve200YLista() throws Exception {
        List<Notificacion> eventos = List.of(
            notificacion(1L, 3L, "INCIDENCIA_CREADA"),
            notificacion(2L, 3L, "ESTADO_CAMBIADO")
        );
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc()).thenReturn(eventos);

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].evento", is("INCIDENCIA_CREADA")))
            .andExpect(jsonPath("$[1].evento", is("ESTADO_CAMBIADO")));

        verify(notificacionRepository).findTop50ByOrderByFechaRecepcionDesc();
    }

    @Test
    @DisplayName("GET /notificaciones → 200 con lista vacía cuando no hay eventos")
    void getActividadReciente_sinEventos_devuelve200YListaVacia() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc())
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /notificaciones → llama exactamente una vez al repositorio")
    void getActividadReciente_llamaAlRepositorioUnaVez() throws Exception {
        when(notificacionRepository.findTop50ByOrderByFechaRecepcionDesc())
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones"));

        verify(notificacionRepository, times(1)).findTop50ByOrderByFechaRecepcionDesc();
    }

    // ── GET /notificaciones/{incidenciaId} ────────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones/{id} → 200 con historial de la incidencia")
    void getHistorialPorIncidencia_conEventos_devuelve200YLista() throws Exception {
        List<Notificacion> historial = List.of(
            notificacion(1L, 5L, "INCIDENCIA_CREADA")
        );
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
        when(notificacionRepository.findByIncidenciaId(99L))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest(name = "incidenciaId = {0}")
    @ValueSource(longs = {1L, 10L, 100L, 999L})
    @DisplayName("GET /notificaciones/{id} → consulta al repositorio con el ID correcto")
    void getHistorialPorIncidencia_pasaElIdCorrectoAlRepositorio(long incidenciaId) throws Exception {
        when(notificacionRepository.findByIncidenciaId(incidenciaId))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/" + incidenciaId))
            .andExpect(status().isOk());

        verify(notificacionRepository).findByIncidenciaId(incidenciaId);
    }

    // ── GET /notificaciones/{incidenciaId}/log ────────────────────────────────

    @Test
    @DisplayName("GET /notificaciones/{id}/log → 200 con fichero texto plano cuando hay eventos")
    void descargarLog_conEventos_devuelve200YTextoPlano() throws Exception {
        List<Notificacion> historial = List.of(
            notificacion(1L, 7L, "INCIDENCIA_CREADA")
        );
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
        when(notificacionRepository.findByIncidenciaId(42L))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/notificaciones/42/log"))
            .andExpect(status().isNoContent());
    }
}

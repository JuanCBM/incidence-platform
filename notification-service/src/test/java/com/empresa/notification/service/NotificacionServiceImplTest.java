package com.empresa.notification.service;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.dto.EstadoIncidencia;
import com.empresa.notification.dto.EventoIncidencia;
import com.empresa.notification.dto.IncidenciaEventoDTO;
import com.empresa.notification.dto.Prioridad;
import com.empresa.notification.repository.NotificacionRepository;
import com.empresa.notification.service.impl.NotificacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void procesarEvento_guardaNotificacionCorrectamente() {
        IncidenciaEventoDTO evento = new IncidenciaEventoDTO();
        evento.setIncidenciaId(1L);
        evento.setEvento(EventoIncidencia.INCIDENCIA_CREADA);
        evento.setTitulo("Error en login");
        evento.setDescripcion("El usuario no puede iniciar sesión");
        evento.setEstado(EstadoIncidencia.ABIERTA);
        evento.setPrioridad(Prioridad.ALTA);

        Notificacion guardada = new Notificacion(1L, "INCIDENCIA_CREADA", "{}");
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(guardada);

        Notificacion resultado = notificacionService.procesarEvento(evento, "{}");

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIncidenciaId());
        assertEquals("INCIDENCIA_CREADA", resultado.getEvento());
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void procesarEvento_llamaAlRepositorioUnaVez() {
        IncidenciaEventoDTO evento = new IncidenciaEventoDTO();
        evento.setIncidenciaId(2L);
        evento.setEvento(EventoIncidencia.ESTADO_CAMBIADO);
        evento.setTitulo("Lentitud en BD");
        evento.setDescripcion("La base de datos responde lento");
        evento.setEstado(EstadoIncidencia.EN_PROGRESO);
        evento.setPrioridad(Prioridad.MEDIA);

        when(notificacionRepository.save(any(Notificacion.class)))
                .thenReturn(new Notificacion(2L, "ESTADO_CAMBIADO", "{}"));

        notificacionService.procesarEvento(evento, "{}");

        verify(notificacionRepository, times(1)).save(any(Notificacion.class));
    }

    @Test
    void procesarEvento_usaElEventoComoNombreEnLaNotificacion() {
        IncidenciaEventoDTO evento = new IncidenciaEventoDTO();
        evento.setIncidenciaId(3L);
        evento.setEvento(EventoIncidencia.INCIDENCIA_CERRADA);
        evento.setTitulo("Bug formulario");
        evento.setDescripcion("El formulario de alta falla");
        evento.setEstado(EstadoIncidencia.CERRADA);
        evento.setPrioridad(Prioridad.BAJA);

        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocation -> {
            Notificacion n = invocation.getArgument(0);
            assertEquals("INCIDENCIA_CERRADA", n.getEvento());
            assertEquals(3L, n.getIncidenciaId());
            return n;
        });

        notificacionService.procesarEvento(evento, "{}");

        verify(notificacionRepository).save(any(Notificacion.class));
    }
}

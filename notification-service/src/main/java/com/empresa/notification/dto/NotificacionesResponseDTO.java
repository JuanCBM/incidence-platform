package com.empresa.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.empresa.notification.domain.Notificacion;

/**
 * DTO extendido para respuestas de notificaciones
 * Por qué: Separa la representación de la BD de la API REST
 */
public class NotificacionesResponseDTO {

    private List<NotificacionDTO> notificaciones;
    private EstadoServicio estadoServicio;

    public NotificacionesResponseDTO(List<Notificacion> notificaciones, long totalNotificaciones) {
        this.notificaciones = notificaciones.stream()
                .map(n -> new NotificacionDTO(n.getId(), n.getIncidenciaId(), n.getEvento(), n.getMensaje(), n.getFechaRecepcion()))
                .toList();
        this.estadoServicio = new EstadoServicio(totalNotificaciones, LocalDateTime.now());
    }

    // Getters
    public List<NotificacionDTO> getNotificaciones() {
        return notificaciones;
    }

    public EstadoServicio getEstadoServicio() {
        return estadoServicio;
    }

    // ─────────────────────────────────────────────────────────────────
    // DTO anidado: NotificacionDTO
    // ─────────────────────────────────────────────────────────────────

    public static class NotificacionDTO {
        private Long id;
        private Long incidenciaId;
        private String evento;
        private String mensaje;
        private LocalDateTime fechaRecepcion;

        public NotificacionDTO(Long id, Long incidenciaId, String evento, String mensaje, LocalDateTime fechaRecepcion) {
            this.id = id;
            this.incidenciaId = incidenciaId;
            this.evento = evento;
            this.mensaje = mensaje;
            this.fechaRecepcion = fechaRecepcion;
        }

        public Long getId() { return id; }
        public Long getIncidenciaId() { return incidenciaId; }
        public String getEvento() { return evento; }
        public String getMensaje() { return mensaje; }
        public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
    }

    // ─────────────────────────────────────────────────────────────────
    // DTO anidado: EstadoServicio
    // ─────────────────────────────────────────────────────────────────

    public static class EstadoServicio {
        private long totalNotificacionesProcesadas;
        private LocalDateTime ultimaActualizacion;
        private String estado = "OK";

        public EstadoServicio(long totalNotificacionesProcesadas, LocalDateTime ultimaActualizacion) {
            this.totalNotificacionesProcesadas = totalNotificacionesProcesadas;
            this.ultimaActualizacion = ultimaActualizacion;
        }

        public long getTotalNotificacionesProcesadas() { return totalNotificacionesProcesadas; }
        public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
        public String getEstado() { return estado; }
    }
}

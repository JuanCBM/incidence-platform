package com.empresa.notification.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.dto.NotificacionesResponseDTO;
import com.empresa.notification.repository.NotificacionRepository;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Devuelve los 50 eventos más recientes de todo el sistema,
     * ordenados de más nuevo a más antiguo, junto con estadísticas del servicio.
     * Usado por Angular para el panel de actividad reciente y por MCP Notificaciones.
     *
     * GET /notificaciones
     */
    @GetMapping("/notificaciones")
    public ResponseEntity<NotificacionesResponseDTO> obtenerActividadReciente() {
        List<Notificacion> notificaciones = notificacionRepository.findTop50ByOrderByFechaRecepcionDesc();
        long totalNotificaciones = notificacionRepository.count();
        
        return ResponseEntity.ok(new NotificacionesResponseDTO(notificaciones, totalNotificaciones));
    }

    /**
     * Devuelve el historial de eventos de una incidencia.
     * Usado por Angular para mostrar el detalle del ticket.
     *
     * GET /notificaciones/{incidenciaId}
     */
    @GetMapping("/notificaciones/{incidenciaId}")
    public ResponseEntity<List<Notificacion>> obtenerPorIncidencia(@PathVariable Long incidenciaId) {
        return ResponseEntity.ok(notificacionRepository.findByIncidenciaId(incidenciaId));
    }

    /**
     * Descarga el historial completo de una incidencia como fichero .txt
     * Usado por Angular para el botón "Descargar log".
     *
     * GET /notificaciones/{incidenciaId}/log
     */
    @GetMapping("/notificaciones/{incidenciaId}/log")
    public ResponseEntity<byte[]> descargarLog(@PathVariable Long incidenciaId) {
        List<Notificacion> notificaciones = notificacionRepository.findByIncidenciaId(incidenciaId);

        if (notificaciones.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("LOG DE INCIDENCIA #").append(incidenciaId).append("\n");
        sb.append("=".repeat(40)).append("\n\n");

        for (Notificacion n : notificaciones) {
            sb.append("Fecha:   ").append(n.getFechaRecepcion()).append("\n");
            sb.append("Evento:  ").append(n.getEvento()).append("\n");
            sb.append("Mensaje: ").append(n.getMensaje()).append("\n");
            sb.append("-".repeat(40)).append("\n");
        }

        byte[] contenido = sb.toString().getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"incidencia-" + incidenciaId + ".log.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(contenido);
    }
}

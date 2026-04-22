package com.empresa.notification.controller;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.repository.NotificacionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    public NotificacionController(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Devuelve los 50 eventos más recientes de todo el sistema,
     * ordenados de más nuevo a más antiguo.
     * Usado por Angular para el panel de actividad reciente.
     *
     * GET /notificaciones
     */
    @GetMapping
    public ResponseEntity<List<Notificacion>> obtenerActividadReciente() {
        List<Notificacion> eventos = notificacionRepository.findTop50ByOrderByFechaRecepcionDesc();
        if (eventos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(eventos);
    }

    /**
     * Devuelve el historial de eventos de una incidencia.
     * Usado por Angular para mostrar el detalle del ticket.
     *
     * GET /notificaciones/{incidenciaId}
     */
    @GetMapping("/{incidenciaId}")
    public ResponseEntity<List<Notificacion>> obtenerPorIncidencia(@PathVariable Long incidenciaId) {
        List<Notificacion> notificaciones = notificacionRepository.findByIncidenciaId(incidenciaId);
        if (notificaciones.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(notificaciones);
    }

    /**
     * Descarga el historial completo de una incidencia como fichero .txt
     * Usado por Angular para el botón "Descargar log".
     *
     * GET /notificaciones/{incidenciaId}/log
     */
    @GetMapping("/{incidenciaId}/log")
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

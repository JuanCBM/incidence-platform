package com.empresa.notification.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long incidenciaId;

    private String evento;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    private LocalDateTime fechaRecepcion;

    public Notificacion() {}

    public Notificacion(Long incidenciaId, String evento, String mensaje) {
        this.incidenciaId = incidenciaId;
        this.evento = evento;
        this.mensaje = mensaje;
        this.fechaRecepcion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getIncidenciaId() { return incidenciaId; }
    public String getEvento() { return evento; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
}

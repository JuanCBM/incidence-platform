package com.empresa.notification.dto;

public class IncidenciaEventoDTO {

    private Long incidenciaId;
    private EventoIncidencia evento;
    private String titulo;
    private String descripcion;
    private EstadoIncidencia estado;
    private Prioridad prioridad;
    private String emailUsuario;

    public IncidenciaEventoDTO() {}

    public Long getIncidenciaId() { return incidenciaId; }
    public EventoIncidencia getEvento() { return evento; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public EstadoIncidencia getEstado() { return estado; }
    public Prioridad getPrioridad() { return prioridad; }
    public String getEmailUsuario() { return emailUsuario; }

    public void setIncidenciaId(Long incidenciaId) { this.incidenciaId = incidenciaId; }
    public void setEvento(EventoIncidencia evento) { this.evento = evento; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setEstado(EstadoIncidencia estado) { this.estado = estado; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public void setEmailUsuario(String emailUsuario) { this.emailUsuario = emailUsuario; }
}

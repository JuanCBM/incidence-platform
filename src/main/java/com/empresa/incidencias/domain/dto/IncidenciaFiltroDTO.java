package com.empresa.incidencias.domain.dto;

import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Prioridad;

public class IncidenciaFiltroDTO {

    private Long usuarioId;
    private EstadoIncidencia estado;
    private Prioridad prioridad;
    private int page = 0;
    private int size = 20;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public EstadoIncidencia getEstado() { return estado; }
    public void setEstado(EstadoIncidencia estado) { this.estado = estado; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

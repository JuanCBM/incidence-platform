package com.empresa.incidencias.domain.dto;

import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidenciaFiltroDTO {

    private Long usuarioId;
    private EstadoIncidencia estado;
    private Prioridad prioridad;
    private int page = 0;
    private int size = 20;
    private String sort;

}

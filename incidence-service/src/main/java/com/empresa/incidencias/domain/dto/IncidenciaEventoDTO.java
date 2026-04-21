package com.empresa.incidencias.domain.dto;

import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaEventoDTO {

    private String tipoEvento;
    private Long incidenciaId;
    private String titulo;
    private EstadoIncidencia estado;
    private Prioridad prioridad;
    private Long usuarioId;
    private LocalDateTime timestamp;
}

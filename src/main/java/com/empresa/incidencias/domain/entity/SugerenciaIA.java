package com.empresa.incidencias.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sugerencia_ia")
public class SugerenciaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "incidencia_id")
    private Incidencia incidencia;

    @Column(name = "prompt_ejecutado", columnDefinition = "TEXT")
    private String promptEjecutado;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

}

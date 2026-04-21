package com.empresa.incidencias.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Constructor requerido por la proyección JPQL con "new"
@Getter
@AllArgsConstructor
public class UsuarioResumenDTO {

    private final Long id;
    private final String nombre;
    private final String email;
}

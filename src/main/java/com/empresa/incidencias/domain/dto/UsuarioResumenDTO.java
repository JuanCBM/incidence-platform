package com.empresa.incidencias.domain.dto;

public class UsuarioResumenDTO {

    private final Long id;
    private final String nombre;
    private final String email;

    // Constructor requerido por la proyección JPQL con "new"
    public UsuarioResumenDTO(Long id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
}

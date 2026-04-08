package com.empresa.incidencias.repository;

import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Rol;
import com.empresa.incidencias.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Método derivado — Spring Data genera la query automáticamente
    Optional<Usuario> findByEmail(String email);

    // JPQL explícita — buscar todos los usuarios de un rol concreto
    @Query("SELECT u FROM Usuario u WHERE u.rol = :rol ORDER BY u.nombre ASC")
    List<Usuario> findByRol(@Param("rol") Rol rol);

    // Proyección DTO — devuelve solo id, nombre y email sin cargar la entidad completa
    @Query("SELECT new com.empresa.incidencias.domain.dto.UsuarioResumenDTO(u.id, u.nombre, u.email) " +
           "FROM Usuario u ORDER BY u.nombre ASC")
    List<UsuarioResumenDTO> findAllAsResumen();
}

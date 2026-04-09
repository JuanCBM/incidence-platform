package com.empresa.incidencias.repository;

import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    List<Incidencia> findByEstado(EstadoIncidencia estado);

    @Query("SELECT i FROM Incidencia i WHERE i.usuarioAsignado.id = :usuarioId AND i.estado = :estado")
    List<Incidencia> findByUsuarioIdAndEstado(@Param("usuarioId") Long usuarioId,
                                              @Param("estado") EstadoIncidencia estado);

    Page<Incidencia> findByEstado(EstadoIncidencia estado, Pageable pageable);

    @Query("""
           SELECT i FROM Incidencia i
           WHERE (:usuarioId IS NULL OR i.usuarioAsignado.id = :usuarioId)
             AND (:estado    IS NULL OR i.estado    = :estado)
             AND (:prioridad IS NULL OR i.prioridad = :prioridad)
           ORDER BY i.fechaCreacion DESC
           """)
    Page<Incidencia> buscarConFiltros(@Param("usuarioId")  Long usuarioId,
                                      @Param("estado")     EstadoIncidencia estado,
                                      @Param("prioridad")  Prioridad prioridad,
                                      Pageable pageable);
}

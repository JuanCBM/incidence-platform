package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.dto.IncidenciaFiltroDTO;
import com.empresa.incidencias.domain.entity.Incidencia;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface IncidenciaService {

    Page<Incidencia> buscarIncidencias(IncidenciaFiltroDTO filtro);

    Optional<Incidencia> obtenerIncidenciaPorId(Long id);

    Incidencia crearIncidencia(Incidencia incidencia);

    Incidencia actualizarIncidencia(Long id, Incidencia incidencia);

    void eliminarIncidencia(Long id);
}
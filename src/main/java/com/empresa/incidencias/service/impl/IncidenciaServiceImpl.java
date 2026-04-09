package com.empresa.incidencias.service.impl;

import com.empresa.incidencias.domain.dto.IncidenciaFiltroDTO;
import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.repository.IncidenciaRepository;
import com.empresa.incidencias.service.IncidenciaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class IncidenciaServiceImpl implements IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;

    public IncidenciaServiceImpl(IncidenciaRepository incidenciaRepository) {
        this.incidenciaRepository = incidenciaRepository;
    }

    @Override
    public Page<Incidencia> buscarIncidencias(IncidenciaFiltroDTO filtro) {
        Sort sort = Sort.by(Sort.Direction.DESC, "fechaCreacion");
        if (filtro.getSort() != null && !filtro.getSort().isBlank()) {
            String[] parts = filtro.getSort().split(",");
            Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, parts[0]);
        }
        PageRequest pageable = PageRequest.of(filtro.getPage(), filtro.getSize(), sort);
        return incidenciaRepository.buscarConFiltros(filtro.getUsuarioId(), filtro.getEstado(), filtro.getPrioridad(), pageable);
    }

    @Override
    public Optional<Incidencia> obtenerIncidenciaPorId(Long id) {
        return incidenciaRepository.findById(id);
    }

    @Override
    public Incidencia crearIncidencia(Incidencia incidencia) {
        return incidenciaRepository.save(incidencia);
    }

    @Override
    public Incidencia actualizarIncidencia(Long id, Incidencia incidencia) {
        Incidencia existente = incidenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada"));
        existente.setTitulo(incidencia.getTitulo());
        existente.setDescripcion(incidencia.getDescripcion());
        existente.setEstado(incidencia.getEstado());
        existente.setPrioridad(incidencia.getPrioridad());
        existente.setUsuarioAsignado(incidencia.getUsuarioAsignado());
        return incidenciaRepository.save(existente);
    }

    @Override
    public void eliminarIncidencia(Long id) {
        if (!incidenciaRepository.existsById(id)) {
            throw new IllegalArgumentException("Incidencia no encontrada");
        }
        incidenciaRepository.deleteById(id);
    }
}
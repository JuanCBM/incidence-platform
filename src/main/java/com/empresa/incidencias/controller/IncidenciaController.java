package com.empresa.incidencias.controller;

import com.empresa.incidencias.api.IncidenciasApi;
import com.empresa.incidencias.domain.dto.IncidenciaFiltroDTO;
import com.empresa.incidencias.domain.entity.*;
import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import com.empresa.incidencias.model.*;
import com.empresa.incidencias.service.IncidenciaService;
import com.empresa.incidencias.service.SugerenciaIAService;
import com.empresa.incidencias.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class IncidenciaController implements IncidenciasApi {

    private final IncidenciaService incidenciaService;
    private final UsuarioService usuarioService;
    private final SugerenciaIAService sugerenciaIAService;

    public IncidenciaController(IncidenciaService incidenciaService,
                                UsuarioService usuarioService,
                                SugerenciaIAService sugerenciaIAService) {
        this.incidenciaService = incidenciaService;
        this.usuarioService = usuarioService;
        this.sugerenciaIAService = sugerenciaIAService;
    }

    @Override
    public ResponseEntity<List<IncidenciaDTO>> listarIncidencias() {
        List<IncidenciaDTO> dtos = incidenciaService.listarIncidencias().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<IncidenciaDTO> obtenerIncidencia(Long id) {
        return incidenciaService.obtenerIncidenciaPorId(id)
                .map(i -> ResponseEntity.ok(toDto(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<IncidenciaDTO> crearIncidencia(IncidenciaCreateDTO body) {
        try {
            Incidencia creada = incidenciaService.crearIncidencia(toEntity(body));
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(creada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<IncidenciaDTO> actualizarIncidencia(Long id, IncidenciaCreateDTO body) {
        try {
            Incidencia actualizada = incidenciaService.actualizarIncidencia(id, toEntity(body));
            return ResponseEntity.ok(toDto(actualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<Void> eliminarIncidencia(Long id) {
        try {
            incidenciaService.eliminarIncidencia(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<PageIncidenciaDTO> buscarIncidencias(
            Long usuarioId, EstadoIncidencia estado, Prioridad prioridad,
            Integer page, Integer size, String sort) {

        IncidenciaFiltroDTO filtro = new IncidenciaFiltroDTO();
        filtro.setUsuarioId(usuarioId);
        filtro.setEstado(estado);
        filtro.setPrioridad(prioridad);
        filtro.setPage(page != null ? page : 0);
        filtro.setSize(size != null ? size : 20);
        filtro.setSort(sort);

        Page<Incidencia> resultado = incidenciaService.buscarIncidencias(filtro);

        PageIncidenciaDTO pageDto = new PageIncidenciaDTO()
                .content(resultado.getContent().stream().map(this::toDto).collect(Collectors.toList()))
                .totalElements(resultado.getTotalElements())
                .totalPages(resultado.getTotalPages())
                .number(resultado.getNumber())
                .size(resultado.getSize());

        return ResponseEntity.ok(pageDto);
    }

    @Override
    public ResponseEntity<SugerenciaIADTO> generarSugerencia(Long id) {
        Incidencia incidencia = incidenciaService.obtenerIncidenciaPorId(id).orElse(null);
        if (incidencia == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            SugerenciaIA sugerencia = sugerenciaIAService.generarSugerencia(incidencia);
            return ResponseEntity.status(HttpStatus.CREATED).body(toSugerenciaDto(sugerencia));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private IncidenciaDTO toDto(Incidencia i) {
        return new IncidenciaDTO()
                .id(i.getId())
                .titulo(i.getTitulo())
                .descripcion(i.getDescripcion())
                .estado(i.getEstado())
                .prioridad(i.getPrioridad())
                .fechaCreacion(i.getFechaCreacion())
                .fechaActualizacion(i.getFechaActualizacion())
                .usuarioAsignado(toUsuarioDto(i.getUsuarioAsignado()));
    }

    private UsuarioDTO toUsuarioDto(Usuario u) {
        if (u == null) return null;
        return new UsuarioDTO()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .rol(u.getRol())
                .fechaAlta(u.getFechaAlta());
    }

    private Incidencia toEntity(IncidenciaCreateDTO dto) {
        Usuario usuario = usuarioService.obtenerUsuarioPorId(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        Incidencia inc = new Incidencia();
        inc.setTitulo(dto.getTitulo());
        inc.setDescripcion(dto.getDescripcion());
        inc.setEstado(dto.getEstado());
        inc.setPrioridad(dto.getPrioridad());
        inc.setUsuarioAsignado(usuario);
        return inc;
    }

    private SugerenciaIADTO toSugerenciaDto(SugerenciaIA s) {
        return new SugerenciaIADTO()
                .id(s.getId())
                .incidenciaId(s.getIncidencia().getId())
                .promptEjecutado(s.getPromptEjecutado())
                .respuesta(s.getRespuesta())
                .fechaGeneracion(s.getFechaGeneracion());
    }
}
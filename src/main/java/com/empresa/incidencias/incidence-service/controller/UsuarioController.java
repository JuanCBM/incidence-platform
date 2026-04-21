package com.empresa.incidencias.controller;

import com.empresa.incidencias.api.UsuariosApi;
import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Usuario;
import com.empresa.incidencias.model.UsuarioCreateDTO;
import com.empresa.incidencias.model.UsuarioDTO;
import com.empresa.incidencias.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UsuarioController implements UsuariosApi {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<UsuarioDTO> dtos = usuarioService.listarTodos().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<UsuarioDTO> obtenerUsuario(Long id) {
        return usuarioService.obtenerUsuarioPorId(id)
                .map(u -> ResponseEntity.ok(toDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UsuarioDTO> crearUsuario(UsuarioCreateDTO body) {
        try {
            Usuario creado = usuarioService.crearUsuario(toEntity(body));
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(creado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<UsuarioDTO> actualizarUsuario(Long id, UsuarioCreateDTO body) {
        try {
            Usuario actualizado = usuarioService.actualizarUsuario(id, toEntity(body));
            return ResponseEntity.ok(toDto(actualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<Void> eliminarUsuario(Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── mappers ───────────────────────────────────────────────────────────────

    private UsuarioDTO toDto(Usuario u) {
        return new UsuarioDTO()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .rol(u.getRol())
                .fechaAlta(u.getFechaAlta());
    }

    private Usuario toEntity(UsuarioCreateDTO dto) {
        Usuario u = new Usuario();
        u.setNombre(dto.getNombre());
        u.setEmail(dto.getEmail());
        u.setRol(dto.getRol());
        return u;
    }
}
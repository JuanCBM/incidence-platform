package com.empresa.incidencias.service.impl;

import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Usuario;
import com.empresa.incidencias.repository.UsuarioRepository;
import com.empresa.incidencias.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<UsuarioResumenDTO> listarUsuarios() {
        return usuarioRepository.findAllAsResumen();
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Usuario crearUsuario(Usuario usuario) {
        // Validación básica
        usuarioRepository.findByEmail(usuario.getEmail())
                .ifPresent(u -> { throw new IllegalArgumentException("Email ya registrado"); });
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario actualizarUsuario(Long id, Usuario usuario) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        existente.setNombre(usuario.getNombre());
        existente.setEmail(usuario.getEmail());
        existente.setRol(usuario.getRol());
        return usuarioRepository.save(existente);
    }

    @Override
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}
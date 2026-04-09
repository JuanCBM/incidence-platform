package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {

    List<UsuarioResumenDTO> listarUsuarios();

    Optional<Usuario> obtenerUsuarioPorId(Long id);

    Usuario crearUsuario(Usuario usuario);

    Usuario actualizarUsuario(Long id, Usuario usuario);

    void eliminarUsuario(Long id);
}

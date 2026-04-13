package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Rol;
import com.empresa.incidencias.domain.entity.Usuario;
import com.empresa.incidencias.repository.UsuarioRepository;
import com.empresa.incidencias.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListarUsuariosResumen() {
        UsuarioResumenDTO dto = new UsuarioResumenDTO(1L, "Juan", "juan@mail.com");

        when(usuarioRepository.findAllAsResumen()).thenReturn(List.of(dto));

        List<UsuarioResumenDTO> result = usuarioService.listarUsuarios();
        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getNombre());
    }

    @Test
    void testObtenerUsuarioPorId() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@mail.com");
        usuario.setRol(Rol.SOPORTE);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> result = usuarioService.obtenerUsuarioPorId(1L);
        assertTrue(result.isPresent());
        assertEquals("Ana", result.get().getNombre());
        assertEquals(Rol.SOPORTE, result.get().getRol());
    }

    // ── crearUsuario ──────────────────────────────────────────────────────────

    @Test
    void crearUsuario_guardaYDevuelveUsuario() {
        Usuario usuario = new Usuario();
        usuario.setEmail("nuevo@mail.com");
        usuario.setNombre("Nuevo");
        usuario.setRol(Rol.USUARIO);

        when(usuarioRepository.findByEmail("nuevo@mail.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario result = usuarioService.crearUsuario(usuario);

        assertEquals("Nuevo", result.getNombre());
        assertEquals(Rol.USUARIO, result.getRol());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void crearUsuario_lanzaExcepcionSiEmailDuplicado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("duplicado@mail.com");

        when(usuarioRepository.findByEmail("duplicado@mail.com"))
                .thenReturn(Optional.of(new Usuario()));

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(usuario));
        verify(usuarioRepository, never()).save(any());
    }

    // ── actualizarUsuario ─────────────────────────────────────────────────────

    @Test
    void actualizarUsuario_actualizaDatosCorrectamente() {
        Usuario existente = new Usuario();
        existente.setId(1L);
        existente.setNombre("Viejo");
        existente.setEmail("viejo@mail.com");
        existente.setRol(Rol.USUARIO);

        Usuario nuevoDatos = new Usuario();
        nuevoDatos.setNombre("Nuevo");
        nuevoDatos.setEmail("nuevo@mail.com");
        nuevoDatos.setRol(Rol.SOPORTE);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        Usuario result = usuarioService.actualizarUsuario(1L, nuevoDatos);

        assertEquals("Nuevo", result.getNombre());
        assertEquals("nuevo@mail.com", result.getEmail());
        assertEquals(Rol.SOPORTE, result.getRol());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void actualizarUsuario_lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(99L, new Usuario()));
        verify(usuarioRepository, never()).save(any());
    }

    // ── eliminarUsuario ───────────────────────────────────────────────────────

    @Test
    void eliminarUsuario_eliminaCorrectamente() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void eliminarUsuario_lanzaExcepcionSiNoExiste() {
        when(usuarioRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.eliminarUsuario(99L));
        verify(usuarioRepository, never()).deleteById(any());
    }
}
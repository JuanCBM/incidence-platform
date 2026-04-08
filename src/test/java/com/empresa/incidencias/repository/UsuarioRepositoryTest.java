package com.empresa.incidencias.repository;

import com.empresa.incidencias.domain.dto.UsuarioResumenDTO;
import com.empresa.incidencias.domain.entity.Rol;
import com.empresa.incidencias.domain.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();

        usuarioRepository.save(usuario("Ana García",    "ana@empresa.com",    Rol.ADMIN));
        usuarioRepository.save(usuario("Carlos López",  "carlos@empresa.com", Rol.SOPORTE));
        usuarioRepository.save(usuario("María Pérez",   "maria@empresa.com",  Rol.SOPORTE));
        usuarioRepository.save(usuario("Juan Martínez", "juan@empresa.com",   Rol.USUARIO));
    }

    // ── findByEmail ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "findByEmail: email={0} → existe={1}")
    @CsvSource({
        "ana@empresa.com,    true",
        "carlos@empresa.com, true",
        "noexiste@test.com,  false"
    })
    void findByEmail_devuelveResultadoCorrecto(String email, boolean debeExistir) {
        Optional<Usuario> resultado = usuarioRepository.findByEmail(email.trim());
        assertThat(resultado.isPresent()).isEqualTo(debeExistir);
    }

    // ── findByRol ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "findByRol: rol={0}")
    @EnumSource(Rol.class)
    void findByRol_devuelveUsuariosDelRolIndicado(Rol rol) {
        List<Usuario> resultado = usuarioRepository.findByRol(rol);
        assertThat(resultado).isNotNull();
        assertThat(resultado).allMatch(u -> u.getRol() == rol);
    }

    @ParameterizedTest(name = "findByRol: rol={0} → total={1}")
    @CsvSource({
        "ADMIN,   1",
        "SOPORTE, 2",
        "USUARIO, 1"
    })
    void findByRol_devuelveCantidadCorrecta(Rol rol, int expectedCount) {
        List<Usuario> resultado = usuarioRepository.findByRol(rol);
        assertThat(resultado).hasSize(expectedCount);
    }

    // ── findAllAsResumen ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "findAllAsResumen: total esperado={0}")
    @CsvSource({"4"})
    void findAllAsResumen_devuelveTodosComoProyeccion(int totalEsperado) {
        List<UsuarioResumenDTO> resultado = usuarioRepository.findAllAsResumen();
        assertThat(resultado).hasSize(totalEsperado);
        assertThat(resultado).allMatch(dto -> dto.getId() != null
                && dto.getNombre() != null
                && dto.getEmail() != null);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Usuario usuario(String nombre, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        return u;
    }
}

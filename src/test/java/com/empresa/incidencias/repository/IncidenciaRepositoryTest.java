package com.empresa.incidencias.repository;

import com.empresa.incidencias.domain.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class IncidenciaRepositoryTest {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario soporte;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        incidenciaRepository.deleteAll();
        usuarioRepository.deleteAll();

        soporte = usuarioRepository.save(usuario("Soporte 1", "soporte@empresa.com", Rol.SOPORTE));
        usuario = usuarioRepository.save(usuario("Usuario 1", "usuario@empresa.com", Rol.USUARIO));

        incidenciaRepository.save(incidencia("Error login",     EstadoIncidencia.ABIERTA,     Prioridad.ALTA,   soporte));
        incidenciaRepository.save(incidencia("Lentitud DB",     EstadoIncidencia.EN_PROGRESO, Prioridad.MEDIA,  soporte));
        incidenciaRepository.save(incidencia("Pantalla blanca", EstadoIncidencia.ABIERTA,     Prioridad.BAJA,   usuario));
        incidenciaRepository.save(incidencia("Email roto",      EstadoIncidencia.CERRADA,     Prioridad.CRITICA, soporte));
        incidenciaRepository.save(incidencia("Bug formulario",  EstadoIncidencia.ABIERTA,     Prioridad.MEDIA,  usuario));
    }

    // ── findByEstado (método derivado) ────────────────────────────────────────

    @ParameterizedTest(name = "findByEstado: estado={0} → total={1}")
    @CsvSource({
        "ABIERTA,     3",
        "EN_PROGRESO, 1",
        "CERRADA,     1"
    })
    void findByEstado_devuelveCantidadCorrecta(EstadoIncidencia estado, int expectedCount) {
        List<Incidencia> resultado = incidenciaRepository.findByEstado(estado);
        assertThat(resultado).hasSize(expectedCount);
        assertThat(resultado).allMatch(i -> i.getEstado() == estado);
    }

    // ── findByUsuarioIdAndEstado (JPQL) ───────────────────────────────────────

    @ParameterizedTest(name = "findByUsuarioIdAndEstado: usuario={0}, estado={1} → total={2}")
    @MethodSource("filtroUsuarioEstadoArgs")
    void findByUsuarioIdAndEstado_filtraCorrectamente(String usuarioRef, EstadoIncidencia estado, int expectedCount) {
        Long usuarioId = "soporte".equals(usuarioRef) ? soporte.getId() : usuario.getId();
        List<Incidencia> resultado = incidenciaRepository.findByUsuarioIdAndEstado(usuarioId, estado);
        assertThat(resultado).hasSize(expectedCount);
    }

    static Stream<Arguments> filtroUsuarioEstadoArgs() {
        return Stream.of(
            Arguments.of("soporte", EstadoIncidencia.ABIERTA,     1),
            Arguments.of("soporte", EstadoIncidencia.EN_PROGRESO, 1),
            Arguments.of("soporte", EstadoIncidencia.CERRADA,     1),
            Arguments.of("usuario", EstadoIncidencia.ABIERTA,     2),
            Arguments.of("usuario", EstadoIncidencia.CERRADA,     0)
        );
    }

    // ── findByEstado paginada ──────────────────────────────────────────────────

    @ParameterizedTest(name = "findByEstado paginada: estado={0}, pageSize={1} → paginasEsperadas={2}")
    @CsvSource({
        "ABIERTA, 2, 2",
        "ABIERTA, 5, 1",
        "CERRADA, 5, 1"
    })
    void findByEstadoPaginada_devuelvePageCorrectamente(EstadoIncidencia estado, int pageSize, int totalPagesEsperadas) {
        Page<Incidencia> resultado = incidenciaRepository.findByEstado(estado, PageRequest.of(0, pageSize));
        assertThat(resultado.getTotalPages()).isEqualTo(totalPagesEsperadas);
        assertThat(resultado.getContent()).allMatch(i -> i.getEstado() == estado);
    }

    // ── buscarConFiltros (filtros opcionales) ─────────────────────────────────

    @ParameterizedTest(name = "buscarConFiltros: usuarioId={0}, estado={1}, prioridad={2} → total={3}")
    @MethodSource("filtrosCombinados")
    void buscarConFiltros_respetaFiltrosOpcionales(Long usuarioIdOffset,
                                                    EstadoIncidencia estado,
                                                    Prioridad prioridad,
                                                    int expectedCount) {
        // usuarioIdOffset: 0=null (sin filtro), 1=soporte, 2=usuario
        Long usuarioId = switch ((int) (long) usuarioIdOffset) {
            case 1 -> soporte.getId();
            case 2 -> usuario.getId();
            default -> null;
        };
        Page<Incidencia> resultado = incidenciaRepository.buscarConFiltros(
                usuarioId, estado, prioridad, PageRequest.of(0, 20));
        assertThat(resultado.getTotalElements()).isEqualTo(expectedCount);
    }

    static Stream<Arguments> filtrosCombinados() {
        return Stream.of(
            // Sin filtros → todas (5)
            Arguments.of(0L, null, null, 5),
            // Solo estado ABIERTA → 3
            Arguments.of(0L, EstadoIncidencia.ABIERTA, null, 3),
            // Solo prioridad ALTA → 1
            Arguments.of(0L, null, Prioridad.ALTA, 1),
            // Estado + prioridad → soporte + ABIERTA + ALTA = 1
            Arguments.of(1L, EstadoIncidencia.ABIERTA, Prioridad.ALTA, 1),
            // Usuario sin incidencias cerradas → 0
            Arguments.of(2L, EstadoIncidencia.CERRADA, null, 0)
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Usuario usuario(String nombre, String email, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setRol(rol);
        return u;
    }

    private Incidencia incidencia(String titulo, EstadoIncidencia estado, Prioridad prioridad, Usuario asignado) {
        Incidencia i = new Incidencia();
        i.setTitulo(titulo);
        i.setDescripcion("Descripción de " + titulo);
        i.setEstado(estado);
        i.setPrioridad(prioridad);
        i.setUsuarioAsignado(asignado);
        return i;
    }
}

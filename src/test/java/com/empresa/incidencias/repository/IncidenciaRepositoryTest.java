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

        soporte = usuarioRepository.save(new UsuarioBuilder().nombre("Soporte 1").email("soporte@empresa.com").rol(Rol.SOPORTE).build());
        usuario = usuarioRepository.save(new UsuarioBuilder().nombre("Usuario 1").email("usuario@empresa.com").rol(Rol.USUARIO).build());

        incidenciaRepository.save(new IncidenciaBuilder().titulo("Error login").estado(EstadoIncidencia.ABIERTA).prioridad(Prioridad.ALTA).asignado(soporte).build());
        incidenciaRepository.save(new IncidenciaBuilder().titulo("Lentitud DB").estado(EstadoIncidencia.EN_PROGRESO).prioridad(Prioridad.MEDIA).asignado(soporte).build());
        incidenciaRepository.save(new IncidenciaBuilder().titulo("Pantalla blanca").estado(EstadoIncidencia.ABIERTA).prioridad(Prioridad.BAJA).asignado(usuario).build());
        incidenciaRepository.save(new IncidenciaBuilder().titulo("Email roto").estado(EstadoIncidencia.CERRADA).prioridad(Prioridad.CRITICA).asignado(soporte).build());
        incidenciaRepository.save(new IncidenciaBuilder().titulo("Bug formulario").estado(EstadoIncidencia.ABIERTA).prioridad(Prioridad.MEDIA).asignado(usuario).build());
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

    // ── builders ──────────────────────────────────────────────────────────────

    static class UsuarioBuilder {
        private final Usuario usuario = new Usuario();

        UsuarioBuilder nombre(String nombre)  { usuario.setNombre(nombre); return this; }
        UsuarioBuilder email(String email)    { usuario.setEmail(email);   return this; }
        UsuarioBuilder rol(Rol rol)           { usuario.setRol(rol);       return this; }
        Usuario build()                       { return usuario; }
    }

    static class IncidenciaBuilder {
        private final Incidencia incidencia = new Incidencia();

        IncidenciaBuilder titulo(String titulo) {
            incidencia.setTitulo(titulo);
            incidencia.setDescripcion("Descripción de " + titulo);
            return this;
        }
        IncidenciaBuilder estado(EstadoIncidencia estado)   { incidencia.setEstado(estado);          return this; }
        IncidenciaBuilder prioridad(Prioridad prioridad)    { incidencia.setPrioridad(prioridad);    return this; }
        IncidenciaBuilder asignado(Usuario asignado)        { incidencia.setUsuarioAsignado(asignado); return this; }
        Incidencia build()                                  { return incidencia; }
    }
}

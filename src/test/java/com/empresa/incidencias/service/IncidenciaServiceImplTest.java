package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.dto.IncidenciaFiltroDTO;
import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import com.empresa.incidencias.repository.IncidenciaRepository;
import com.empresa.incidencias.service.impl.IncidenciaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class IncidenciaServiceImplTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @InjectMocks
    private IncidenciaServiceImpl incidenciaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBuscarConFiltros() {
        Incidencia i = new Incidencia();
        i.setId(1L);
        i.setTitulo("Error filtro");
        IncidenciaFiltroDTO filtro = new IncidenciaFiltroDTO();
        filtro.setEstado(EstadoIncidencia.ABIERTA);
        filtro.setPrioridad(Prioridad.ALTA);
        filtro.setPage(0);
        filtro.setSize(10);

        Page<Incidencia> page = new PageImpl<>(List.of(i));
        when(incidenciaRepository.buscarConFiltros(
                any(), any(), any(), any(PageRequest.class)
        )).thenReturn(page);

        Page<Incidencia> result = incidenciaService.buscarIncidencias(filtro);
        assertEquals(1, result.getContent().size());
        assertEquals("Error filtro", result.getContent().get(0).getTitulo());
    }

    @Test
    void testObtenerIncidenciaPorId() {
        Incidencia i = new Incidencia();
        i.setId(1L);
        i.setTitulo("Test ID");

        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(i));

        Optional<Incidencia> result = incidenciaService.obtenerIncidenciaPorId(1L);
        assertTrue(result.isPresent());
        assertEquals("Test ID", result.get().getTitulo());
    }

    // ── crearIncidencia ───────────────────────────────────────────────────────

    @Test
    void crearIncidencia_guardaYDevuelveIncidencia() {
        Incidencia incidencia = new Incidencia();
        incidencia.setTitulo("Nueva incidencia");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setPrioridad(Prioridad.MEDIA);

        when(incidenciaRepository.save(incidencia)).thenReturn(incidencia);

        Incidencia result = incidenciaService.crearIncidencia(incidencia);

        assertEquals("Nueva incidencia", result.getTitulo());
        assertEquals(EstadoIncidencia.ABIERTA, result.getEstado());
        verify(incidenciaRepository).save(incidencia);
    }

    // ── actualizarIncidencia ──────────────────────────────────────────────────

    @Test
    void actualizarIncidencia_actualizaDatosCorrectamente() {
        Incidencia existente = new Incidencia();
        existente.setId(1L);
        existente.setTitulo("Vieja");
        existente.setEstado(EstadoIncidencia.ABIERTA);
        existente.setPrioridad(Prioridad.BAJA);

        Incidencia nuevaDatos = new Incidencia();
        nuevaDatos.setTitulo("Nueva");
        nuevaDatos.setDescripcion("Descripción actualizada");
        nuevaDatos.setEstado(EstadoIncidencia.CERRADA);
        nuevaDatos.setPrioridad(Prioridad.ALTA);

        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(incidenciaRepository.save(existente)).thenReturn(existente);

        Incidencia result = incidenciaService.actualizarIncidencia(1L, nuevaDatos);

        assertEquals("Nueva", result.getTitulo());
        assertEquals("Descripción actualizada", result.getDescripcion());
        assertEquals(EstadoIncidencia.CERRADA, result.getEstado());
        assertEquals(Prioridad.ALTA, result.getPrioridad());
        verify(incidenciaRepository).save(existente);
    }

    @Test
    void actualizarIncidencia_lanzaExcepcionSiNoExiste() {
        when(incidenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> incidenciaService.actualizarIncidencia(99L, new Incidencia()));
        verify(incidenciaRepository, never()).save(any());
    }

    // ── eliminarIncidencia ────────────────────────────────────────────────────

    @Test
    void eliminarIncidencia_eliminaCorrectamente() {
        when(incidenciaRepository.existsById(1L)).thenReturn(true);

        incidenciaService.eliminarIncidencia(1L);

        verify(incidenciaRepository).deleteById(1L);
    }

    @Test
    void eliminarIncidencia_lanzaExcepcionSiNoExiste() {
        when(incidenciaRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> incidenciaService.eliminarIncidencia(99L));
        verify(incidenciaRepository, never()).deleteById(any());
    }
}
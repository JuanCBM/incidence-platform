package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.entity.EstadoIncidencia;
import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.Prioridad;
import com.empresa.incidencias.domain.entity.SugerenciaIA;
import com.empresa.incidencias.infrastructure.CopilotClient;
import com.empresa.incidencias.repository.SugerenciaIARepository;
import com.empresa.incidencias.service.impl.SugerenciaIAServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SugerenciaIAServiceImplTest {

    @Mock
    private SugerenciaIARepository sugerenciaIARepository;

    @Mock
    private CopilotClient copilotClient;

    @InjectMocks
    private SugerenciaIAServiceImpl sugerenciaIAService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerarSugerenciaConIncidencia() throws Exception {
        // Creamos la incidencia
        Incidencia incidencia = new Incidencia();
        incidencia.setId(Long.valueOf(1));
        incidencia.setTitulo("Test incidencia");
        incidencia.setDescripcion("Descripción de prueba");
        incidencia.setEstado(EstadoIncidencia.ABIERTA);
        incidencia.setPrioridad(Prioridad.ALTA);
        incidencia.setFechaCreacion(LocalDateTime.of(2026, 4, 9, 10, 0));

        // Mock de CopilotClient: devuelve texto simulado
        when(copilotClient.ejecutarPrompt(anyString()))
                .thenReturn("Paso 1, Paso 2");

        // Mock del repositorio: devuelve una sugerencia con ID simulado
        SugerenciaIA sugerenciaGuardada = new SugerenciaIA();
        sugerenciaGuardada.setId(Long.valueOf(100));
        when(sugerenciaIARepository.save(any(SugerenciaIA.class)))
                .thenReturn(sugerenciaGuardada);

        // Llamada al servicio
        SugerenciaIA result = sugerenciaIAService.generarSugerencia(incidencia);

        // Verificaciones
        assertNotNull(result);
        assertEquals(Long.valueOf(100), result.getId());
        verify(sugerenciaIARepository).save(any(SugerenciaIA.class));
        verify(copilotClient).ejecutarPrompt(anyString());
    }
}
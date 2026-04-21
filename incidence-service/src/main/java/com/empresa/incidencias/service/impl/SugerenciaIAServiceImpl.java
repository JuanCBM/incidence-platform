package com.empresa.incidencias.service.impl;

import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.SugerenciaIA;
import com.empresa.incidencias.infrastructure.CopilotClient;
import com.empresa.incidencias.repository.SugerenciaIARepository;
import com.empresa.incidencias.service.SugerenciaIAService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Transactional
public class SugerenciaIAServiceImpl implements SugerenciaIAService {

    private final SugerenciaIARepository sugerenciaIARepository;
    private final CopilotClient copilotClient;

    public SugerenciaIAServiceImpl(SugerenciaIARepository sugerenciaIARepository,
                                   CopilotClient copilotClient) {
        this.sugerenciaIARepository = sugerenciaIARepository;
        this.copilotClient = copilotClient;
    }

    @Override
    public SugerenciaIA generarSugerencia(Incidencia incidencia) throws Exception {
        // Cargar plantilla
        Path plantilla = Path.of(".copilot/prompt-sugerencia-resolucion.txt");
        String prompt = Files.readString(plantilla);

        // Sustituir variables
        String promptInterpolado = prompt
                .replace("{{TITULO}}", incidencia.getTitulo())
                .replace("{{DESCRIPCION}}", Objects.toString(incidencia.getDescripcion(), ""))
                .replace("{{ESTADO}}", incidencia.getEstado().name())
                .replace("{{PRIORIDAD}}", incidencia.getPrioridad().name())
                .replace("{{FECHA_CREACION}}", incidencia.getFechaCreacion().toString());

        // Ejecutar Copilot
        String respuesta;
        try {
            respuesta = copilotClient.ejecutarPrompt(promptInterpolado);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Copilot CLI no disponible", e);
        }

        // Persistir resultado
        SugerenciaIA sugerencia = new SugerenciaIA();
        sugerencia.setIncidencia(incidencia);
        sugerencia.setPromptEjecutado(promptInterpolado);
        sugerencia.setRespuesta(respuesta);
        sugerencia.setFechaGeneracion(LocalDateTime.now());

        return sugerenciaIARepository.save(sugerencia);
    }
}
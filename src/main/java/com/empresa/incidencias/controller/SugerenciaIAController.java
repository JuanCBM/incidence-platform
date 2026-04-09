package com.empresa.incidencias.controller;

import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.SugerenciaIA;
import com.empresa.incidencias.repository.IncidenciaRepository;
import com.empresa.incidencias.service.SugerenciaIAService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidencias")
public class SugerenciaIAController {

    private final IncidenciaRepository incidenciaRepository;
    private final SugerenciaIAService sugerenciaIAService;

    public SugerenciaIAController(IncidenciaRepository incidenciaRepository,
                                  SugerenciaIAService sugerenciaIAService) {
        this.incidenciaRepository = incidenciaRepository;
        this.sugerenciaIAService = sugerenciaIAService;
    }

    @PostMapping("/{id}/sugerencia")
    public ResponseEntity<?> generarSugerencia(@PathVariable Long id) {
        Incidencia incidencia = incidenciaRepository.findById(id).orElse(null);
        if (incidencia == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Incidencia no encontrada");
        }

        try {
            SugerenciaIA sugerencia = sugerenciaIAService.generarSugerencia(incidencia);
            return ResponseEntity.status(HttpStatus.CREATED).body(sugerencia);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Copilot CLI no disponible");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar la sugerencia");
        }
    }
}
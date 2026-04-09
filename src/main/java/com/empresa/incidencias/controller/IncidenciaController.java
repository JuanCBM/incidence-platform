package com.empresa.incidencias.controller;

import com.empresa.incidencias.domain.dto.IncidenciaFiltroDTO;
import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.service.IncidenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidencias")
public class IncidenciaController {

    @Autowired
    private IncidenciaService incidenciaService;

    @GetMapping("/buscar")
    public ResponseEntity<Page<Incidencia>> buscarIncidencias(IncidenciaFiltroDTO filtro) {
        return ResponseEntity.ok(incidenciaService.buscarIncidencias(filtro));
    }
}


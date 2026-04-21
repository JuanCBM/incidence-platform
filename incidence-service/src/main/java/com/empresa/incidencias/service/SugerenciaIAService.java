package com.empresa.incidencias.service;

import com.empresa.incidencias.domain.entity.Incidencia;
import com.empresa.incidencias.domain.entity.SugerenciaIA;

public interface SugerenciaIAService {
    SugerenciaIA generarSugerencia(Incidencia incidencia) throws Exception;
}
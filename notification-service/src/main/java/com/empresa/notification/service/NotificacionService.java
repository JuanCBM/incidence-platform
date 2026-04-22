package com.empresa.notification.service;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.dto.IncidenciaEventoDTO;

public interface NotificacionService {

    Notificacion procesarEvento(IncidenciaEventoDTO evento, String mensajeOriginal);
}

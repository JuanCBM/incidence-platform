package com.empresa.notification.service.impl;

import com.empresa.notification.domain.Notificacion;
import com.empresa.notification.dto.IncidenciaEventoDTO;
import com.empresa.notification.repository.NotificacionRepository;
import com.empresa.notification.service.EmailService;
import com.empresa.notification.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    private final NotificacionRepository notificacionRepository;
    private final EmailService emailService;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository,
                                   EmailService emailService) {
        this.notificacionRepository = notificacionRepository;
        this.emailService = emailService;
    }

    @Override
    public Notificacion procesarEvento(IncidenciaEventoDTO evento, String mensajeOriginal) {
        log.info("[NOTIFICACION] Evento recibido: tipo={}, incidenciaId={}, titulo='{}', estado={}, prioridad={}",
                evento.getEvento(),
                evento.getIncidenciaId(),
                evento.getTitulo(),
                evento.getEstado(),
                evento.getPrioridad());

        Notificacion notificacion = new Notificacion(
                evento.getIncidenciaId(),
                evento.getEvento().name(),
                mensajeOriginal
        );

        Notificacion guardada = notificacionRepository.save(notificacion);
        log.info("[NOTIFICACION] Guardada en BD con id={}", guardada.getId());

        emailService.enviarNotificacion(evento);

        return guardada;
    }
}

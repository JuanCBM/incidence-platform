package com.empresa.notification.consumer;

import com.empresa.notification.dto.IncidenciaEventoDTO;
import com.empresa.notification.service.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidenciaEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaEventoConsumer.class);

    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public IncidenciaEventoConsumer(NotificacionService notificacionService,
                                    ObjectMapper objectMapper) {
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "incidencias-eventos", groupId = "notification-group")
    public void consumir(String mensaje) {
        try {
            IncidenciaEventoDTO evento = objectMapper.readValue(mensaje, IncidenciaEventoDTO.class);
            notificacionService.procesarEvento(evento, mensaje);
        } catch (Exception e) {
            log.error("[NOTIFICACION] Error procesando mensaje: {}", e.getMessage());
        }
    }
}

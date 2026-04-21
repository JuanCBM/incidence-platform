package com.empresa.incidencias.infrastructure;

import com.empresa.incidencias.domain.dto.IncidenciaEventoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class IncidenciaKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaKafkaProducer.class);
    static final String TOPIC = "incidencias-eventos";

    private final KafkaTemplate<String, IncidenciaEventoDTO> kafkaTemplate;

    public IncidenciaKafkaProducer(KafkaTemplate<String, IncidenciaEventoDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(IncidenciaEventoDTO evento) {
        kafkaTemplate.send(TOPIC, String.valueOf(evento.getIncidenciaId()), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Error publicando evento Kafka para incidencia {}: {}", evento.getIncidenciaId(), ex.getMessage());
                    } else {
                        log.info("Evento '{}' publicado para incidencia {}", evento.getTipoEvento(), evento.getIncidenciaId());
                    }
                });
    }
}

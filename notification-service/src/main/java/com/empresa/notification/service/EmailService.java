package com.empresa.notification.service;

import com.empresa.notification.dto.IncidenciaEventoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${notificacion.mail.from}")
    private String mailFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarNotificacion(IncidenciaEventoDTO evento) {
        if (evento.getEmailUsuario() == null || evento.getEmailUsuario().isBlank()) {
            log.warn("[EMAIL] No se puede enviar notificación: emailUsuario no presente en el evento");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(mailFrom);
            mensaje.setTo(evento.getEmailUsuario());
            mensaje.setSubject(asunto(evento));
            mensaje.setText(cuerpo(evento));

            mailSender.send(mensaje);
            log.info("[EMAIL] Notificación enviada a {} para incidencia {}", evento.getEmailUsuario(), evento.getIncidenciaId());

        } catch (Exception e) {
            log.error("[EMAIL] Error al enviar notificación a {}: {}", evento.getEmailUsuario(), e.getMessage());
        }
    }

    private String asunto(IncidenciaEventoDTO evento) {
        return switch (evento.getEvento()) {
            case INCIDENCIA_CREADA  -> "Incidencia recibida: " + evento.getTitulo();
            case ESTADO_CAMBIADO    -> "Actualización en tu incidencia: " + evento.getTitulo();
            case INCIDENCIA_CERRADA -> "Incidencia resuelta: " + evento.getTitulo();
            default                 -> "Notificación sobre tu incidencia: " + evento.getTitulo();
        };
    }

    private String cuerpo(IncidenciaEventoDTO evento) {
        return switch (evento.getEvento()) {
            case INCIDENCIA_CREADA -> """
                    Hemos recibido tu incidencia correctamente.

                    Título:      %s
                    Descripción: %s
                    Prioridad:   %s
                    Estado:      %s

                    Nos pondremos en contacto contigo en cuanto sea procesada.
                    """.formatted(evento.getTitulo(), evento.getDescripcion(), evento.getPrioridad(), evento.getEstado());

            case ESTADO_CAMBIADO -> """
                    El estado de tu incidencia ha cambiado.

                    Título:  %s
                    Estado:  %s

                    Puedes consultar el detalle en la plataforma.
                    """.formatted(evento.getTitulo(), evento.getEstado());

            case INCIDENCIA_CERRADA -> """
                    Tu incidencia ha sido resuelta y cerrada.

                    Título: %s

                    Gracias por usar la plataforma de incidencias.
                    """.formatted(evento.getTitulo());

            default -> "Se ha producido un evento en tu incidencia: " + evento.getTitulo();
        };
    }
}

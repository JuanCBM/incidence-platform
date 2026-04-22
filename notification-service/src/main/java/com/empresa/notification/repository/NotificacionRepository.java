package com.empresa.notification.repository;

import com.empresa.notification.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByIncidenciaId(Long incidenciaId);

    List<Notificacion> findByEvento(String evento);

    List<Notificacion> findTop50ByOrderByFechaRecepcionDesc();
}

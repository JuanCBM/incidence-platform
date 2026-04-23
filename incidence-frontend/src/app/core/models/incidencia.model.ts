export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: 'ADMIN' | 'SOPORTE' | 'USUARIO';
  fechaAlta: string;
}

export interface Incidencia {
  id: number;
  titulo: string;
  descripcion: string;
  estado: 'ABIERTA' | 'EN_PROGRESO' | 'CERRADA';
  prioridad: 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface Notificacion {
  id: number;
  incidenciaId: number;
  evento: string;
  mensaje: string;
  fechaRecepcion: string;
}

export interface EstadoServicio {
  totalNotificacionesProcesadas: number;
  ultimaActualizacion: string;
  estado: string;
}

export interface NotificacionesResponseDTO {
  notificaciones: Notificacion[];
  estadoServicio: EstadoServicio;
}

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EstadoServicio, Notificacion } from '../../core/models/incidencia.model';
import { NotificacionService } from '../../core/services/notificacion.service';

@Component({
  selector: 'app-actividad',
  standalone: false,
  templateUrl: './actividad.component.html',
  styleUrl: './actividad.component.scss'
})
export class ActividadComponent implements OnInit {

  eventos: Notificacion[] = [];
  estadoServicio: EstadoServicio | null = null;
  cargando = true;
  error = false;
  emailUsuario = '';

  columnas: string[] = ['fecha', 'incidencia', 'evento', 'mensaje'];

  constructor(
    private notificacionService: NotificacionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.emailUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.emailUsuario) { this.router.navigate(['/auth/login']); return; }
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = false;
    this.notificacionService.getActividadReciente().subscribe({
      next: (data) => {
        this.eventos = data.notificaciones;
        this.estadoServicio = data.estadoServicio;
        this.cargando = false;
      },
      error: () => { this.error = true; this.cargando = false; }
    });
  }

  iconoEvento(evento: string): string {
    switch (evento) {
      case 'INCIDENCIA_CREADA': return 'add_circle';
      case 'ESTADO_CAMBIADO':   return 'sync';
      default:                  return 'notifications';
    }
  }

  colorEvento(evento: string): string {
    switch (evento) {
      case 'INCIDENCIA_CREADA': return 'evento-creada';
      case 'ESTADO_CAMBIADO':   return 'evento-cambio';
      default:                  return '';
    }
  }

  verIncidencia(id: number): void {
    this.router.navigate(['/incidencias/detalle', id]);
  }

  volver(): void { this.router.navigate(['/incidencias/bienvenida']); }
  cerrarSesion(): void { sessionStorage.clear(); this.router.navigate(['/auth/login']); }
}

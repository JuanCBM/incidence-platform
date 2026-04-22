import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Incidencia, Notificacion } from '../../core/models/incidencia.model';
import { IncidenciaService } from '../../core/services/incidencia.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-detalle',
  standalone: false,
  templateUrl: './detalle.component.html',
  styleUrl: './detalle.component.scss'
})
export class DetalleComponent implements OnInit {

  incidencia: Incidencia | null = null;
  notificaciones: Notificacion[] = [];
  cargando = true;
  error = false;
  emailUsuario = '';

  estados = ['ABIERTA', 'EN_PROGRESO', 'CERRADA'];
  estadoControl = new FormControl('', Validators.required);
  actualizando = false;
  errorActualizar = false;
  mostrarFormEstado = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private incidenciaService: IncidenciaService,
    private notificacionService: NotificacionService
  ) {}

  ngOnInit(): void {
    this.emailUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.emailUsuario) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarDatos(id);
  }

  cargarDatos(id: number): void {
    this.cargando = true;
    this.error = false;

    this.incidenciaService.getIncidenciaById(id).subscribe({
      next: (inc) => {
        this.incidencia = inc;
        this.cargarNotificaciones(id);
      },
      error: () => {
        this.error = true;
        this.cargando = false;
      }
    });
  }

  cargarNotificaciones(id: number): void {
    this.notificacionService.getNotificacionesPorIncidencia(id).subscribe({
      next: (notifs) => {
        this.notificaciones = notifs;
        this.cargando = false;
      },
      error: () => {
        // Las notificaciones son opcionales, no bloqueamos la vista
        this.cargando = false;
      }
    });
  }

  abrirCambioEstado(): void {
    if (this.incidencia) {
      this.estadoControl.setValue(this.incidencia.estado);
      this.mostrarFormEstado = true;
      this.errorActualizar = false;
    }
  }

  cancelarCambioEstado(): void {
    this.mostrarFormEstado = false;
    this.errorActualizar = false;
  }

  guardarEstado(): void {
    if (!this.incidencia || this.estadoControl.invalid) return;
    this.actualizando = true;
    this.errorActualizar = false;
    const payload = { ...this.incidencia, estado: this.estadoControl.value };
    this.incidenciaService.actualizarIncidencia(this.incidencia.id, payload).subscribe({
      next: (inc) => {
        this.incidencia = inc;
        this.actualizando = false;
        this.mostrarFormEstado = false;
        this.cargarNotificaciones(inc.id);
      },
      error: () => { this.errorActualizar = true; this.actualizando = false; }
    });
  }

  descargarLog(): void {
    if (!this.incidencia) return;
    this.notificacionService.descargarLog(this.incidencia.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `incidencia-${this.incidencia!.id}.log.txt`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  volver(): void {
    this.router.navigate(['/incidencias/lista']);
  }

  cerrarSesion(): void {
    sessionStorage.clear();
    this.router.navigate(['/auth/login']);
  }

  colorEstado(estado: string): string {
    switch (estado) {
      case 'ABIERTA':     return 'chip-abierta';
      case 'EN_PROGRESO': return 'chip-progreso';
      case 'CERRADA':     return 'chip-cerrada';
      default:            return '';
    }
  }

  colorPrioridad(prioridad: string): string {
    switch (prioridad) {
      case 'CRITICA': return 'chip-critica';
      case 'ALTA':    return 'chip-alta';
      case 'MEDIA':   return 'chip-media';
      case 'BAJA':    return 'chip-baja';
      default:        return '';
    }
  }
}

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Incidencia } from '../../core/models/incidencia.model';
import { IncidenciaService } from '../../core/services/incidencia.service';
import { FormControl } from '@angular/forms';

@Component({
  selector: 'app-lista',
  standalone: false,
  templateUrl: './lista.component.html',
  styleUrl: './lista.component.scss'
})
export class ListaComponent implements OnInit {

  incidencias: Incidencia[] = [];
  incidenciasFiltradas: Incidencia[] = [];
  cargando = true;
  error = false;
  emailUsuario = '';

  columnas: string[] = ['id', 'titulo', 'estado', 'prioridad', 'fechaCreacion', 'acciones'];

  filtroTexto = new FormControl('');
  filtroEstado = new FormControl('');
  filtroprioridad = new FormControl('');
  estados = ['', 'ABIERTA', 'EN_PROGRESO', 'CERRADA'];
  prioridades = ['', 'BAJA', 'MEDIA', 'ALTA', 'CRITICA'];

  constructor(private incidenciaService: IncidenciaService, private router: Router) {}

  ngOnInit(): void {
    this.emailUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.emailUsuario) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.filtroTexto.valueChanges.subscribe(() => this.aplicarFiltros());
    this.filtroEstado.valueChanges.subscribe(() => this.aplicarFiltros());
    this.filtroprioridad.valueChanges.subscribe(() => this.aplicarFiltros());
    this.cargarIncidencias();
  }

  cargarIncidencias(): void {
    this.cargando = true;
    this.error = false;
    this.incidenciaService.getIncidencias().subscribe({
      next: (data) => {
        this.incidencias = data;
        this.aplicarFiltros();
        this.cargando = false;
      },
      error: () => {
        this.error = true;
        this.cargando = false;
      }
    });
  }

  aplicarFiltros(): void {
    const texto = (this.filtroTexto.value || '').toLowerCase();
    const estado = this.filtroEstado.value || '';
    const prioridad = this.filtroprioridad.value || '';

    this.incidenciasFiltradas = this.incidencias.filter(inc => {
      const coincideTexto = !texto ||
        inc.titulo.toLowerCase().includes(texto) ||
        inc.descripcion?.toLowerCase().includes(texto);
      const coincideEstado = !estado || inc.estado === estado;
      const coincidePrioridad = !prioridad || inc.prioridad === prioridad;
      return coincideTexto && coincideEstado && coincidePrioridad;
    });
  }

  limpiarFiltros(): void {
    this.filtroTexto.setValue('');
    this.filtroEstado.setValue('');
    this.filtroprioridad.setValue('');
  }

  verDetalle(id: number): void {
    this.router.navigate(['/incidencias/detalle', id]);
  }

  crear(): void {
    this.router.navigate(['/incidencias/crear']);
  }

  volver(): void {
    this.router.navigate(['/incidencias/bienvenida']);
  }

  cerrarSesion(): void {
    sessionStorage.clear();
    this.router.navigate(['/auth/login']);
  }

  colorEstado(estado: string): string {
    switch (estado) {
      case 'ABIERTA':     return 'warn';
      case 'EN_PROGRESO': return 'accent';
      case 'CERRADA':     return 'primary';
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

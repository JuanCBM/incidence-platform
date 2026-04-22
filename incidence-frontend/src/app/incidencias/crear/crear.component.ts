import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IncidenciaService } from '../../core/services/incidencia.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { Usuario } from '../../core/models/incidencia.model';

@Component({
  selector: 'app-crear',
  standalone: false,
  templateUrl: './crear.component.html',
  styleUrl: './crear.component.scss'
})
export class CrearComponent implements OnInit {

  form: FormGroup;
  guardando = false;
  error = false;
  usuarios: Usuario[] = [];
  emailUsuario = sessionStorage.getItem('emailUsuario') || '';

  estados = ['ABIERTA', 'EN_PROGRESO', 'CERRADA'];
  prioridades = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];

  constructor(
    private fb: FormBuilder,
    private incidenciaService: IncidenciaService,
    private usuarioService: UsuarioService,
    private router: Router
  ) {
    this.form = this.fb.group({
      titulo: ['', Validators.required],
      descripcion: ['', Validators.required],
      estado: ['ABIERTA', Validators.required],
      prioridad: ['MEDIA', Validators.required],
      usuarioId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    if (!this.emailUsuario) { this.router.navigate(['/auth/login']); return; }
    this.usuarioService.getUsuarios().subscribe({
      next: (data) => this.usuarios = data
    });
  }

  guardar(): void {
    if (this.form.invalid) return;
    this.guardando = true;
    this.error = false;
    this.incidenciaService.crearIncidencia(this.form.value).subscribe({
      next: () => this.router.navigate(['/incidencias/lista']),
      error: () => { this.error = true; this.guardando = false; }
    });
  }

  volver(): void { this.router.navigate(['/incidencias/lista']); }
  cerrarSesion(): void { sessionStorage.clear(); this.router.navigate(['/auth/login']); }
}

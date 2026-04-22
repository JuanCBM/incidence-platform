import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Usuario } from '../../core/models/incidencia.model';
import { UsuarioService } from '../../core/services/usuario.service';

@Component({
  selector: 'app-lista',
  standalone: false,
  templateUrl: './lista.component.html',
  styleUrl: './lista.component.scss'
})
export class ListaComponent implements OnInit {

  usuarios: Usuario[] = [];
  cargando = true;
  error = false;
  emailUsuario = '';
  columnas: string[] = ['id', 'nombre', 'email', 'rol', 'fechaAlta', 'acciones'];

  constructor(private usuarioService: UsuarioService, private router: Router) {}

  ngOnInit(): void {
    this.emailUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.emailUsuario) { this.router.navigate(['/auth/login']); return; }
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = false;
    this.usuarioService.getUsuarios().subscribe({
      next: (data) => { this.usuarios = data; this.cargando = false; },
      error: () => { this.error = true; this.cargando = false; }
    });
  }

  verDetalle(id: number): void { this.router.navigate(['/usuarios/detalle', id]); }
  crear(): void { this.router.navigate(['/usuarios/crear']); }
  volver(): void { this.router.navigate(['/incidencias/bienvenida']); }
  cerrarSesion(): void { sessionStorage.clear(); this.router.navigate(['/auth/login']); }

  colorRol(rol: string): string {
    switch (rol) {
      case 'ADMIN':   return 'chip-admin';
      case 'SOPORTE': return 'chip-soporte';
      default:        return 'chip-usuario';
    }
  }
}

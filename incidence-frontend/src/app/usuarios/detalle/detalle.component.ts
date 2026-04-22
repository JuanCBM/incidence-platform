import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Usuario } from '../../core/models/incidencia.model';
import { UsuarioService } from '../../core/services/usuario.service';

@Component({
  selector: 'app-detalle',
  standalone: false,
  templateUrl: './detalle.component.html',
  styleUrl: './detalle.component.scss'
})
export class DetalleComponent implements OnInit {

  usuario: Usuario | null = null;
  cargando = true;
  error = false;
  emailUsuario = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.emailUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.emailUsuario) { this.router.navigate(['/auth/login']); return; }

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.cargando = true;
    this.error = false;
    this.usuarioService.getUsuarioById(id).subscribe({
      next: (u) => { this.usuario = u; this.cargando = false; },
      error: () => { this.error = true; this.cargando = false; }
    });
  }

  volver(): void { this.router.navigate(['/usuarios/lista']); }
  cerrarSesion(): void { sessionStorage.clear(); this.router.navigate(['/auth/login']); }

  colorRol(rol: string): string {
    switch (rol) {
      case 'ADMIN':   return 'chip-admin';
      case 'SOPORTE': return 'chip-soporte';
      default:        return 'chip-usuario';
    }
  }
}

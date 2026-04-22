import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-bienvenida',
  standalone: false,
  templateUrl: './bienvenida.component.html',
  styleUrl: './bienvenida.component.scss'
})
export class BienvenidaComponent implements OnInit {

  nombreUsuario: string = '';

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.nombreUsuario = sessionStorage.getItem('emailUsuario') || '';
    if (!this.nombreUsuario) {
      this.router.navigate(['/auth/login']);
    }
  }

  verIncidencias(): void {
    this.router.navigate(['/incidencias/lista']);
  }

  verUsuarios(): void {
    this.router.navigate(['/usuarios/lista']);
  }

  verActividad(): void {
    this.router.navigate(['/incidencias/actividad']);
  }

  cerrarSesion(): void {
    sessionStorage.clear();
    this.router.navigate(['/auth/login']);
  }
}

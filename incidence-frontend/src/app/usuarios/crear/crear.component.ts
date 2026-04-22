import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService } from '../../core/services/usuario.service';

@Component({
  selector: 'app-crear',
  standalone: false,
  templateUrl: './crear.component.html',
  styleUrl: './crear.component.scss'
})
export class CrearComponent {

  form: FormGroup;
  guardando = false;
  error = false;
  emailUsuario = sessionStorage.getItem('emailUsuario') || '';

  roles = ['ADMIN', 'SOPORTE', 'USUARIO'];

  constructor(private fb: FormBuilder, private usuarioService: UsuarioService, private router: Router) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      rol: ['USUARIO', Validators.required]
    });
  }

  guardar(): void {
    if (this.form.invalid) return;
    this.guardando = true;
    this.error = false;
    this.usuarioService.crearUsuario(this.form.value).subscribe({
      next: () => this.router.navigate(['/usuarios/lista']),
      error: () => { this.error = true; this.guardando = false; }
    });
  }

  volver(): void { this.router.navigate(['/usuarios/lista']); }
  cerrarSesion(): void { sessionStorage.clear(); this.router.navigate(['/auth/login']); }
}

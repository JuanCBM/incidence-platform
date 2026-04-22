import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  loginForm: FormGroup;

  constructor(private fb: FormBuilder, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  entrar(): void {
    if (this.loginForm.valid) {
      const email = this.loginForm.value.email;
      // Guardamos el email en sessionStorage para usarlo en otras páginas
      sessionStorage.setItem('emailUsuario', email);
      this.router.navigate(['/incidencias/bienvenida']);
    }
  }

  get emailInvalido(): boolean {
    const control = this.loginForm.get('email');
    return !!(control?.invalid && control?.touched);
  }
}

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ListaComponent } from './lista/lista.component';
import { DetalleComponent } from './detalle/detalle.component';
import { CrearComponent } from './crear/crear.component';

const routes: Routes = [
  { path: 'lista', component: ListaComponent },
  { path: 'detalle/:id', component: DetalleComponent },
  { path: 'crear', component: CrearComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UsuariosRoutingModule { }

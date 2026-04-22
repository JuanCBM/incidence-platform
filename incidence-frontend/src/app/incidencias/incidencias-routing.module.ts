import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BienvenidaComponent } from './bienvenida/bienvenida.component';
import { ListaComponent } from './lista/lista.component';
import { DetalleComponent } from './detalle/detalle.component';
import { CrearComponent } from './crear/crear.component';
import { ActividadComponent } from './actividad/actividad.component';

const routes: Routes = [
  { path: 'bienvenida', component: BienvenidaComponent },
  { path: 'lista', component: ListaComponent },
  { path: 'detalle/:id', component: DetalleComponent },
  { path: 'crear', component: CrearComponent },
  { path: 'actividad', component: ActividadComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IncidenciasRoutingModule { }

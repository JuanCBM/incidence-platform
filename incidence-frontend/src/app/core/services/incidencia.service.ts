import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Incidencia } from '../models/incidencia.model';

@Injectable({
  providedIn: 'root'
})
export class IncidenciaService {

  private apiUrl = '/incidencias';

  constructor(private http: HttpClient) {}

  getIncidencias(): Observable<Incidencia[]> {
    return this.http.get<Incidencia[]>(this.apiUrl);
  }

  getIncidenciaById(id: number): Observable<Incidencia> {
    return this.http.get<Incidencia>(`${this.apiUrl}/${id}`);
  }

  crearIncidencia(incidencia: any): Observable<Incidencia> {
    return this.http.post<Incidencia>(this.apiUrl, incidencia);
  }

  actualizarIncidencia(id: number, incidencia: any): Observable<Incidencia> {
    return this.http.put<Incidencia>(`${this.apiUrl}/${id}`, incidencia);
  }
}

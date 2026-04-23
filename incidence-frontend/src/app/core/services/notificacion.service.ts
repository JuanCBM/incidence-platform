import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notificacion, NotificacionesResponseDTO } from '../models/incidencia.model';

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {

  private apiUrl = '/notificaciones';

  constructor(private http: HttpClient) {}

  getActividadReciente(): Observable<NotificacionesResponseDTO> {
    return this.http.get<NotificacionesResponseDTO>(this.apiUrl);
  }

  getNotificacionesPorIncidencia(incidenciaId: number): Observable<Notificacion[]> {
    return this.http.get<Notificacion[]>(`${this.apiUrl}/${incidenciaId}`);
  }

  descargarLog(incidenciaId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${incidenciaId}/log`, { responseType: 'blob' });
  }
}

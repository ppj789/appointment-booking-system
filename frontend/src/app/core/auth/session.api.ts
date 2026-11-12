import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginCredentials, SessionContext } from './session.models';

@Injectable({ providedIn: 'root' })
export class SessionApi {
  private readonly http = inject(HttpClient);
  private readonly sessionsUrl = '/api/v1/sessions';

  login(credentials: LoginCredentials): Observable<SessionContext> {
    return this.http.post<SessionContext>(this.sessionsUrl, credentials);
  }

  current(): Observable<SessionContext> {
    return this.http.get<SessionContext>(`${this.sessionsUrl}/current`);
  }

  logout(): Observable<void> {
    return this.http.delete<void>(`${this.sessionsUrl}/current`);
  }
}

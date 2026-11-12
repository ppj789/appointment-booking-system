import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AccountSetupApi {
  private readonly http = inject(HttpClient);

  complete(token: string, password: string): Observable<void> {
    return this.http.post<void>(
      '/api/v1/account-setup',
      { password },
      { headers: new HttpHeaders().set('X-Setup-Token', token) },
    );
  }
}

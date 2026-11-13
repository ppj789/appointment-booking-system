import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  GuestAppointmentCommand,
  GuestAppointmentConfirmation,
  GuestAvailability,
  GuestInvitationContext,
} from './guest-booking.models';

@Injectable({ providedIn: 'root' })
export class GuestBookingApi {
  private readonly http = inject(HttpClient);
  private readonly guestUrl = '/api/v1/guest';

  getInvitation(rawToken: string): Observable<GuestInvitationContext> {
    return this.http.get<GuestInvitationContext>(`${this.guestUrl}/invitation`, {
      headers: invitationHeaders(rawToken),
    });
  }

  getAvailability(rawToken: string, durationMinutes: number): Observable<GuestAvailability> {
    const params = new HttpParams().set('durationMinutes', durationMinutes).set('days', 14);
    return this.http.get<GuestAvailability>(`${this.guestUrl}/availability`, {
      headers: invitationHeaders(rawToken),
      params,
    });
  }

  confirmAppointment(
    rawToken: string,
    idempotencyKey: string,
    command: GuestAppointmentCommand,
  ): Observable<GuestAppointmentConfirmation> {
    return this.http.post<GuestAppointmentConfirmation>(`${this.guestUrl}/appointments`, command, {
      headers: invitationHeaders(rawToken).set('Idempotency-Key', idempotencyKey),
    });
  }
}

function invitationHeaders(rawToken: string): HttpHeaders {
  return new HttpHeaders({ 'X-Invitation-Token': rawToken });
}

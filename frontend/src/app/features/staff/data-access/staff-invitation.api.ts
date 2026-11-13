import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  InvitationActivation,
  InvitationDraft,
  InvitationDraftCommand,
  InvitationRevocation,
  InvitationSummary,
} from './staff-invitation.models';

@Injectable({ providedIn: 'root' })
export class StaffInvitationApi {
  private readonly http = inject(HttpClient);
  private readonly invitationsUrl = '/api/v1/me/invitations';

  list(): Observable<InvitationSummary[]> {
    return this.http.get<InvitationSummary[]>(this.invitationsUrl);
  }

  create(command: InvitationDraftCommand): Observable<InvitationDraft> {
    return this.http.post<InvitationDraft>(this.invitationsUrl, command);
  }

  update(invitationId: string, command: InvitationDraftCommand): Observable<InvitationDraft> {
    return this.http.patch<InvitationDraft>(`${this.invitationsUrl}/${invitationId}`, command);
  }

  activate(invitationId: string): Observable<InvitationActivation> {
    return this.http.post<InvitationActivation>(
      `${this.invitationsUrl}/${invitationId}/activate`,
      null,
    );
  }

  revoke(invitationId: string): Observable<InvitationRevocation> {
    return this.http.post<InvitationRevocation>(
      `${this.invitationsUrl}/${invitationId}/revoke`,
      null,
    );
  }
}

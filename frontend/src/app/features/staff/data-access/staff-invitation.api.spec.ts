import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StaffInvitationApi } from './staff-invitation.api';

describe('StaffInvitationApi', () => {
  let api: StaffInvitationApi;
  let http: HttpTestingController;
  const command = {
    title: 'Account review',
    description: 'Review the account application.',
    guest: { displayName: 'Guest', email: 'guest@example.test' },
    defaultDurationMinutes: 30,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StaffInvitationApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(StaffInvitationApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists and saves owned Invitation drafts', () => {
    api.list().subscribe();
    const list = http.expectOne('/api/v1/me/invitations');
    expect(list.request.method).toBe('GET');
    list.flush([]);

    api.create(command).subscribe();
    const create = http.expectOne('/api/v1/me/invitations');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(command);
    create.flush({ id: 'invitation-id', status: 'DRAFT', ...command });

    api.update('invitation-id', command).subscribe();
    const update = http.expectOne('/api/v1/me/invitations/invitation-id');
    expect(update.request.method).toBe('PATCH');
    expect(update.request.body).toEqual(command);
    update.flush({ id: 'invitation-id', status: 'DRAFT', ...command });
  });

  it('uses dedicated activation and revocation commands', () => {
    api.activate('invitation-id').subscribe();
    const activation = http.expectOne('/api/v1/me/invitations/invitation-id/activate');
    expect(activation.request.method).toBe('POST');
    expect(activation.request.body).toBeNull();
    activation.flush({
      id: 'invitation-id',
      status: 'ACTIVE',
      activatedAt: '2026-11-13T12:00:00Z',
      shareUrl: '/book#token=one-time',
    });

    api.revoke('invitation-id').subscribe();
    const revocation = http.expectOne('/api/v1/me/invitations/invitation-id/revoke');
    expect(revocation.request.method).toBe('POST');
    expect(revocation.request.body).toBeNull();
    revocation.flush({
      id: 'invitation-id',
      status: 'REVOKED',
      revokedAt: '2026-11-13T12:05:00Z',
    });
  });
});

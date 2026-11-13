import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { StaffInvitationApi } from './staff-invitation.api';
import {
  InvitationActivation,
  InvitationDraft,
  InvitationRevocation,
  InvitationSummary,
} from './staff-invitation.models';
import { StaffInvitationStore } from './staff-invitation.store';

describe('StaffInvitationStore', () => {
  const older: InvitationSummary = invitation('Older', '2026-11-12T10:00:00Z');
  const newer: InvitationSummary = invitation('Newer', '2026-11-13T10:00:00Z');
  const activation: InvitationActivation = {
    id: newer.id,
    status: 'ACTIVE',
    activatedAt: '2026-11-13T11:00:00Z',
    shareUrl: '/book#token=one-time-secret',
  };
  let api: {
    list: ReturnType<typeof vi.fn<() => Observable<InvitationSummary[]>>>;
    create: ReturnType<typeof vi.fn<() => Observable<InvitationDraft>>>;
    update: ReturnType<typeof vi.fn<() => Observable<InvitationDraft>>>;
    activate: ReturnType<typeof vi.fn<() => Observable<InvitationActivation>>>;
    revoke: ReturnType<typeof vi.fn<() => Observable<InvitationRevocation>>>;
  };
  let store: StaffInvitationStore;

  beforeEach(() => {
    api = {
      list: vi.fn(() => of([older, newer])),
      create: vi.fn(() => of(newer)),
      update: vi.fn(() => of(newer)),
      activate: vi.fn(() => of(activation)),
      revoke: vi.fn(() =>
        of({ id: newer.id, status: 'REVOKED', revokedAt: '2026-11-13T12:00:00Z' }),
      ),
    };
    TestBed.configureTestingModule({
      providers: [StaffInvitationStore, { provide: StaffInvitationApi, useValue: api }],
    });
    store = TestBed.inject(StaffInvitationStore);
  });

  it('sorts owned Invitations by most recent update', async () => {
    await store.load();

    expect(store.invitations().map((item) => item.title)).toEqual(['Newer', 'Older']);
  });

  it('returns the one-time activation result and reloads the capability-free list', async () => {
    const result = await store.activate(newer.id);

    expect(result).toEqual(activation);
    expect(api.activate).toHaveBeenCalledWith(newer.id);
    expect(api.list).toHaveBeenCalledOnce();
    expect(store.invitations()).toEqual([newer, older]);
  });
});

function invitation(title: string, updatedAt: string): InvitationSummary {
  return {
    id: `${title.toLowerCase()}-id`,
    status: 'DRAFT',
    title,
    description: 'Description',
    guest: { displayName: 'Guest', email: 'guest@example.test' },
    defaultDurationMinutes: 30,
    createdAt: updatedAt,
    updatedAt,
    activatedAt: null,
    consumedAt: null,
    revokedAt: null,
  };
}

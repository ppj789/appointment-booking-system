import { describe, expect, it } from 'vitest';
import { InvitationStatus, InvitationSummary } from '../../data-access/staff-invitation.models';
import { groupInvitations } from './invitation-groups';

describe('groupInvitations', () => {
  it('groups Invitations in the user-facing lifecycle order', () => {
    const groups = groupInvitations([
      invitation('revoked', 'REVOKED'),
      invitation('booked', 'CONSUMED'),
      invitation('active', 'ACTIVE'),
      invitation('draft', 'DRAFT'),
    ]);

    expect(groups.map((group) => group.label)).toEqual(['Draft', 'Active', 'Booked', 'Revoked']);
    expect(groups.map((group) => group.invitations[0]?.id)).toEqual([
      'draft',
      'active',
      'booked',
      'revoked',
    ]);
  });

  it('keeps an empty lifecycle group visible', () => {
    const groups = groupInvitations([invitation('active', 'ACTIVE')]);

    expect(groups).toHaveLength(4);
    expect(groups.find((group) => group.status === 'CONSUMED')?.invitations).toEqual([]);
  });
});

function invitation(id: string, status: InvitationStatus): InvitationSummary {
  return {
    id,
    status,
    title: `${id} title`,
    description: `${id} description`,
    guest: { displayName: 'Guest', email: 'guest@example.test' },
    defaultDurationMinutes: 30,
    createdAt: '2026-11-13T10:00:00Z',
    updatedAt: '2026-11-13T10:00:00Z',
    activatedAt: null,
    consumedAt: null,
    revokedAt: null,
  };
}

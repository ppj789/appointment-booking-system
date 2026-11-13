import {
  InvitationStatus,
  InvitationSummary,
  invitationStatusLabel,
} from '../../data-access/staff-invitation.models';

const invitationStatusOrder: InvitationStatus[] = ['DRAFT', 'ACTIVE', 'CONSUMED', 'REVOKED'];

const emptyMessages: Record<InvitationStatus, string> = {
  DRAFT: 'No draft Invitations.',
  ACTIVE: 'No active Invitations awaiting a booking.',
  CONSUMED: 'No booked Invitations.',
  REVOKED: 'No revoked Invitations.',
};

export interface InvitationGroup {
  status: InvitationStatus;
  label: string;
  emptyMessage: string;
  invitations: InvitationSummary[];
}

export function groupInvitations(invitations: InvitationSummary[]): InvitationGroup[] {
  return invitationStatusOrder.map((status) => ({
    status,
    label: invitationStatusLabel(status),
    emptyMessage: emptyMessages[status],
    invitations: invitations.filter((invitation) => invitation.status === status),
  }));
}

export type InvitationStatus = 'DRAFT' | 'ACTIVE' | 'CONSUMED' | 'REVOKED';

export interface InvitationGuest {
  displayName: string;
  email: string;
}

export interface InvitationDraft {
  id: string;
  status: InvitationStatus;
  title: string;
  description: string;
  guest: InvitationGuest;
  defaultDurationMinutes: number;
  createdAt: string;
  updatedAt: string;
}

export interface InvitationSummary extends InvitationDraft {
  activatedAt: string | null;
  consumedAt: string | null;
  revokedAt: string | null;
}

export interface InvitationDraftCommand {
  title: string;
  description: string;
  guest: InvitationGuest;
  defaultDurationMinutes: number;
}

export interface InvitationActivation {
  id: string;
  status: 'ACTIVE';
  activatedAt: string;
  shareUrl: string;
}

export interface InvitationRevocation {
  id: string;
  status: 'REVOKED';
  revokedAt: string;
}

export const invitationDurations = [15, 30, 45, 60, 120, 240] as const;

export function durationLabel(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = minutes / 60;
  return `${hours} ${hours === 1 ? 'hour' : 'hours'}`;
}

export function invitationStatusLabel(status: InvitationStatus): string {
  switch (status) {
    case 'DRAFT':
      return 'Draft';
    case 'ACTIVE':
      return 'Active';
    case 'CONSUMED':
      return 'Booked';
    case 'REVOKED':
      return 'Revoked';
  }
}

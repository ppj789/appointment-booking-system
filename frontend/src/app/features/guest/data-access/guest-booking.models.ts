export interface GuestBranch {
  publicCode: string;
  name: string;
  displayAddress: string;
  timezone: string;
}

export interface GuestInvitationContext {
  title: string;
  description: string;
  hostDisplayName: string;
  guestDisplayName: string;
  defaultDurationMinutes: number;
  supportedDurationsMinutes: number[];
  branch: GuestBranch;
}

export interface GuestAvailabilitySlot {
  startAt: string;
  endAt: string;
}

export interface GuestAvailability {
  durationMinutes: number;
  generatedAt: string;
  window: {
    from: string;
    toExclusive: string;
  };
  slots: GuestAvailabilitySlot[];
}

export interface GuestAppointmentCommand {
  startAt: string;
  durationMinutes: number;
}

export interface GuestAppointment {
  reference: string;
  title: string;
  description: string;
  hostDisplayName: string;
  guestDisplayName: string;
  guestEmail: string;
  startAt: string;
  endAt: string;
  createdAt: string;
  branch: GuestBranch;
}

export interface GuestAppointmentConfirmation {
  appointment: GuestAppointment;
  confirmation: {
    status: 'CONFIRMED';
    message: string;
    reference: string;
  };
}

export function guestDurationLabel(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = minutes / 60;
  return `${hours} ${hours === 1 ? 'hour' : 'hours'}`;
}

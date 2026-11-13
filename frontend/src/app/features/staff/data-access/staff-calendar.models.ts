export interface CalendarAppointment {
  id: string;
  reference: string;
  title: string;
  guestDisplayName: string;
  startAt: string;
  endAt: string;
}

export interface AvailabilityBlock {
  id: string;
  startAt: string;
  endAt: string;
}

export interface StaffCalendar {
  from: string;
  toExclusive: string;
  appointments: CalendarAppointment[];
  blocks: AvailabilityBlock[];
}

export interface CreateAvailabilityBlockCommand {
  startAt: string;
  endAt: string;
}

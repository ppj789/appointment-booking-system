import { GuestAvailabilitySlot } from '../../data-access/guest-booking.models';

export interface GuestSlotDateGroup {
  dateKey: string;
  label: string;
  slots: GuestAvailabilitySlot[];
}

export function groupGuestSlotsByBranchDate(
  slots: GuestAvailabilitySlot[],
  timezone: string,
  locale = 'en-ZA',
): GuestSlotDateGroup[] {
  const groups = new Map<string, GuestSlotDateGroup>();
  const dateFormatter = new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    weekday: 'short',
    day: '2-digit',
    month: 'short',
  });

  for (const slot of [...slots].sort((left, right) => left.startAt.localeCompare(right.startAt))) {
    const instant = new Date(slot.startAt);
    const dateKey = branchDateKey(instant, timezone);
    const group = groups.get(dateKey) ?? {
      dateKey,
      label: dateFormatter.format(instant),
      slots: [],
    };
    group.slots.push(slot);
    groups.set(dateKey, group);
  }
  return [...groups.values()];
}

export function formatGuestSlotTime(instant: string, timezone: string, locale = 'en-ZA'): string {
  return new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(instant));
}

function branchDateKey(instant: Date, timezone: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instant);
  return `${part(parts, 'year')}-${part(parts, 'month')}-${part(parts, 'day')}`;
}

function part(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): string {
  const value = parts.find((candidate) => candidate.type === type)?.value;
  if (!value) {
    throw new RangeError(`Unable to read ${type} in the Branch timezone.`);
  }
  return value;
}

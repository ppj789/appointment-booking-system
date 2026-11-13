import {
  branchLocalDateTimeToInstant,
  BranchCalendarDay,
} from '../../../../core/time/branch-calendar';
import { CreateAvailabilityBlockCommand } from '../../data-access/staff-calendar.models';

export interface CalendarDraftSelection {
  dayIndex: number;
  pointerId: number | null;
  anchorSlot: number;
  startMinutes: number;
  endMinutes: number;
}

export const MINUTES_PER_CALENDAR_SLOT = 15;
export const CALENDAR_SLOTS_PER_DAY = (24 * 60) / MINUTES_PER_CALENDAR_SLOT;

export function calendarSelectionFromSlots(
  dayIndex: number,
  pointerId: number | null,
  anchorSlot: number,
  currentSlot: number,
): CalendarDraftSelection {
  return {
    dayIndex,
    pointerId,
    anchorSlot,
    startMinutes: Math.min(anchorSlot, currentSlot) * MINUTES_PER_CALENDAR_SLOT,
    endMinutes: (Math.max(anchorSlot, currentSlot) + 1) * MINUTES_PER_CALENDAR_SLOT,
  };
}

export function calendarSelectionCommand(
  day: BranchCalendarDay,
  selection: CalendarDraftSelection,
  timezone: string,
): CreateAvailabilityBlockCommand {
  return {
    startAt: branchInstantAtMinute(day, selection.startMinutes, timezone),
    endAt: branchInstantAtMinute(day, selection.endMinutes, timezone),
  };
}

export function isCalendarSelectionFuture(
  command: CreateAvailabilityBlockCommand,
  now = new Date(),
): boolean {
  return new Date(command.startAt).getTime() > now.getTime();
}

function branchInstantAtMinute(day: BranchCalendarDay, minutes: number, timezone: string): string {
  if (minutes === 0) {
    return day.startAt;
  }
  if (minutes === 24 * 60) {
    return day.endAt;
  }
  const hour = Math.floor(minutes / 60)
    .toString()
    .padStart(2, '0');
  const minute = (minutes % 60).toString().padStart(2, '0');
  return branchLocalDateTimeToInstant(`${day.dateKey}T${hour}:${minute}`, timezone);
}

import { describe, expect, it } from 'vitest';
import { BranchCalendarDay } from '../../../../core/time/branch-calendar';
import {
  calendarSelectionAnnouncement,
  calendarSelectionCommand,
  calendarSelectionFromSlots,
  isCalendarSelectionFuture,
} from './staff-calendar-selection';

const DAY: BranchCalendarDay = {
  dateKey: '2026-11-16',
  label: 'Mon 16 Nov',
  startAt: '2026-11-15T22:00:00.000Z',
  endAt: '2026-11-16T22:00:00.000Z',
};

describe('calendar selection', () => {
  it('normalises a backwards drag and snaps it to 15-minute boundaries', () => {
    const selection = calendarSelectionFromSlots(2, 7, 34, 32);

    expect(selection).toEqual({
      dayIndex: 2,
      pointerId: 7,
      anchorSlot: 34,
      startMinutes: 480,
      endMinutes: 525,
    });
  });

  it('converts a Branch-local selection to UTC command boundaries', () => {
    const selection = calendarSelectionFromSlots(0, null, 32, 33);

    expect(calendarSelectionCommand(DAY, selection, 'Africa/Johannesburg')).toEqual({
      startAt: '2026-11-16T06:00:00.000Z',
      endAt: '2026-11-16T06:30:00.000Z',
    });
  });

  it('requires the selected start to be strictly in the future', () => {
    const command = calendarSelectionCommand(
      DAY,
      calendarSelectionFromSlots(0, null, 32, 33),
      'Africa/Johannesburg',
    );

    expect(isCalendarSelectionFuture(command, new Date('2026-11-16T05:59:59.999Z'))).toBe(true);
    expect(isCalendarSelectionFuture(command, new Date(command.startAt))).toBe(false);
  });

  it('announces the selected Branch-local interval for assistive technology', () => {
    const selection = calendarSelectionFromSlots(0, null, 32, 33);

    expect(calendarSelectionAnnouncement(DAY, selection, 'Africa/Johannesburg')).toBe(
      'Mon 16 Nov. Unavailable time selected from 08:00 to 08:30 in Africa/Johannesburg.',
    );
  });
});

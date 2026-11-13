import {
  branchLocalDateTimeToInstant,
  getBranchCalendarWeek,
  instantToBranchLocalInput,
} from './branch-calendar';

describe('Branch calendar time', () => {
  it('builds a Monday-to-Monday week in the Branch timezone', () => {
    const week = getBranchCalendarWeek('2026-11-13T10:00:00Z', 'Africa/Johannesburg');

    expect(week.from).toBe('2026-11-08T22:00:00.000Z');
    expect(week.toExclusive).toBe('2026-11-15T22:00:00.000Z');
    expect(week.days.map((day) => day.dateKey)).toEqual([
      '2026-11-09',
      '2026-11-10',
      '2026-11-11',
      '2026-11-12',
      '2026-11-13',
      '2026-11-14',
      '2026-11-15',
    ]);
  });

  it('round-trips Branch-local input without using the browser timezone', () => {
    const instant = branchLocalDateTimeToInstant('2026-11-13T14:30', 'Africa/Johannesburg');

    expect(instant).toBe('2026-11-13T12:30:00.000Z');
    expect(instantToBranchLocalInput(instant, 'Africa/Johannesburg')).toBe('2026-11-13T14:30');
  });
});

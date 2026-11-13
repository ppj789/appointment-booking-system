import { formatGuestSlotTime, groupGuestSlotsByBranchDate } from './guest-slot-groups';

describe('Guest slot presentation', () => {
  it('groups UTC slots by their Branch-local date', () => {
    const groups = groupGuestSlotsByBranchDate(
      [
        { startAt: '2026-11-16T22:15:00.000Z', endAt: '2026-11-16T22:45:00.000Z' },
        { startAt: '2026-11-16T21:30:00.000Z', endAt: '2026-11-16T22:00:00.000Z' },
      ],
      'Africa/Johannesburg',
    );

    expect(groups.map((group) => group.dateKey)).toEqual(['2026-11-16', '2026-11-17']);
    expect(groups[0].slots[0].startAt).toBe('2026-11-16T21:30:00.000Z');
  });

  it('formats time using the Branch timezone', () => {
    expect(formatGuestSlotTime('2026-11-16T06:30:00.000Z', 'Africa/Johannesburg')).toBe('08:30');
  });
});

import { formatBranchDateTime } from './branch-date-time';

describe('formatBranchDateTime', () => {
  it('formats a UTC instant in the controlled Branch timezone', () => {
    const formatted = formatBranchDateTime('2026-11-11T08:00:00Z', 'Africa/Johannesburg', 'en-ZA');

    expect(formatted).toContain('10:00');
    expect(formatted).not.toContain('08:00');
  });

  it('rejects an invalid instant', () => {
    expect(() => formatBranchDateTime('not-an-instant', 'Africa/Johannesburg')).toThrow(
      'A valid appointment instant is required.',
    );
  });
});

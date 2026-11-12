import { Pipe, PipeTransform } from '@angular/core';

export function formatBranchDateTime(
  instant: string | Date,
  timezone: string,
  locale = 'en-ZA',
): string {
  const value = instant instanceof Date ? instant : new Date(instant);
  if (Number.isNaN(value.getTime())) {
    throw new RangeError('A valid appointment instant is required.');
  }

  return new Intl.DateTimeFormat(locale, {
    timeZone: timezone,
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZoneName: 'short',
  }).format(value);
}

@Pipe({ name: 'branchDateTime' })
export class BranchDateTimePipe implements PipeTransform {
  transform(instant: string | Date, timezone: string): string {
    return formatBranchDateTime(instant, timezone);
  }
}

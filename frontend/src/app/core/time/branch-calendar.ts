export interface BranchCalendarDay {
  dateKey: string;
  label: string;
  startAt: string;
  endAt: string;
}

export interface BranchCalendarWeek {
  from: string;
  toExclusive: string;
  label: string;
  days: BranchCalendarDay[];
}

interface DateParts {
  year: number;
  month: number;
  day: number;
}

interface DateTimeParts extends DateParts {
  hour: number;
  minute: number;
}

export function getBranchCalendarWeek(
  instant: string | Date,
  timezone: string,
  locale = 'en-ZA',
): BranchCalendarWeek {
  const reference = instant instanceof Date ? instant : new Date(instant);
  assertValidDate(reference);
  const localDate = dateParts(reference, timezone);
  const dateAtUtc = new Date(Date.UTC(localDate.year, localDate.month - 1, localDate.day));
  const mondayOffset = (dateAtUtc.getUTCDay() + 6) % 7;
  dateAtUtc.setUTCDate(dateAtUtc.getUTCDate() - mondayOffset);

  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(dateAtUtc);
    date.setUTCDate(date.getUTCDate() + index);
    const nextDate = new Date(date);
    nextDate.setUTCDate(nextDate.getUTCDate() + 1);
    const dateKey = utcDateKey(date);
    return {
      dateKey,
      label: new Intl.DateTimeFormat(locale, {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        timeZone: 'UTC',
      }).format(date),
      startAt: branchLocalDateTimeToInstant(`${dateKey}T00:00`, timezone),
      endAt: branchLocalDateTimeToInstant(`${utcDateKey(nextDate)}T00:00`, timezone),
    };
  });

  const first = dateAtUtc;
  const last = new Date(dateAtUtc);
  last.setUTCDate(last.getUTCDate() + 6);
  const labelFormatter = new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  });

  return {
    from: days[0].startAt,
    toExclusive: days[6].endAt,
    label: `${labelFormatter.format(first)} – ${labelFormatter.format(last)}`,
    days,
  };
}

export function branchLocalDateTimeToInstant(value: string, timezone: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value);
  if (!match) {
    throw new RangeError('A valid Branch-local date and time is required.');
  }
  const expected: DateTimeParts = {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
    hour: Number(match[4]),
    minute: Number(match[5]),
  };
  const wallClockUtc = Date.UTC(
    expected.year,
    expected.month - 1,
    expected.day,
    expected.hour,
    expected.minute,
  );
  let candidate = wallClockUtc - timezoneOffsetMilliseconds(new Date(wallClockUtc), timezone);
  candidate = wallClockUtc - timezoneOffsetMilliseconds(new Date(candidate), timezone);
  const result = new Date(candidate);
  const actual = dateTimeParts(result, timezone);
  if (!sameDateTime(actual, expected)) {
    throw new RangeError('That local time does not exist in the Branch timezone.');
  }
  return result.toISOString();
}

export function instantToBranchLocalInput(instant: string | Date, timezone: string): string {
  const value = instant instanceof Date ? instant : new Date(instant);
  assertValidDate(value);
  const parts = dateTimeParts(value, timezone);
  return `${parts.year.toString().padStart(4, '0')}-${parts.month
    .toString()
    .padStart(2, '0')}-${parts.day.toString().padStart(2, '0')}T${parts.hour
    .toString()
    .padStart(2, '0')}:${parts.minute.toString().padStart(2, '0')}`;
}

export function branchMinutesOfDay(instant: string | Date, timezone: string): number {
  const value = instant instanceof Date ? instant : new Date(instant);
  assertValidDate(value);
  const parts = dateTimeParts(value, timezone);
  return parts.hour * 60 + parts.minute;
}

function timezoneOffsetMilliseconds(instant: Date, timezone: string): number {
  const parts = dateTimeParts(instant, timezone);
  return (
    Date.UTC(parts.year, parts.month - 1, parts.day, parts.hour, parts.minute) -
    Math.floor(instant.getTime() / 60_000) * 60_000
  );
}

function dateParts(value: Date, timezone: string): DateParts {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(value);
  return {
    year: partNumber(parts, 'year'),
    month: partNumber(parts, 'month'),
    day: partNumber(parts, 'day'),
  };
}

function dateTimeParts(value: Date, timezone: string): DateTimeParts {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(value);
  return {
    year: partNumber(parts, 'year'),
    month: partNumber(parts, 'month'),
    day: partNumber(parts, 'day'),
    hour: partNumber(parts, 'hour'),
    minute: partNumber(parts, 'minute'),
  };
}

function partNumber(parts: Intl.DateTimeFormatPart[], type: Intl.DateTimeFormatPartTypes): number {
  const value = parts.find((part) => part.type === type)?.value;
  if (!value) {
    throw new RangeError(`Unable to read ${type} in the Branch timezone.`);
  }
  return Number(value);
}

function sameDateTime(left: DateTimeParts, right: DateTimeParts): boolean {
  return (
    left.year === right.year &&
    left.month === right.month &&
    left.day === right.day &&
    left.hour === right.hour &&
    left.minute === right.minute
  );
}

function utcDateKey(value: Date): string {
  return `${value.getUTCFullYear().toString().padStart(4, '0')}-${(value.getUTCMonth() + 1)
    .toString()
    .padStart(2, '0')}-${value.getUTCDate().toString().padStart(2, '0')}`;
}

function assertValidDate(value: Date): void {
  if (Number.isNaN(value.getTime())) {
    throw new RangeError('A valid appointment instant is required.');
  }
}

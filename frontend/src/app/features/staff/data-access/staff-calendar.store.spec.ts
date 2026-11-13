import { TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { StaffCalendarApi } from './staff-calendar.api';
import { AvailabilityBlock, StaffCalendar } from './staff-calendar.models';
import { StaffCalendarStore } from './staff-calendar.store';

describe('StaffCalendarStore', () => {
  const calendar: StaffCalendar = {
    from: '2026-11-08T22:00:00.000Z',
    toExclusive: '2026-11-15T22:00:00.000Z',
    appointments: [],
    blocks: [],
  };
  const block: AvailabilityBlock = {
    id: 'block-id',
    startAt: '2026-11-13T12:00:00.000Z',
    endAt: '2026-11-13T13:00:00.000Z',
  };
  let api: {
    getCalendar: ReturnType<typeof vi.fn<() => Observable<StaffCalendar>>>;
    createBlock: ReturnType<typeof vi.fn<() => Observable<AvailabilityBlock>>>;
    removeBlock: ReturnType<typeof vi.fn<() => Observable<void>>>;
  };
  let store: StaffCalendarStore;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-11-13T10:00:00Z'));
    api = {
      getCalendar: vi.fn(() => of(calendar)),
      createBlock: vi.fn(() => of(block)),
      removeBlock: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      providers: [StaffCalendarStore, { provide: StaffCalendarApi, useValue: api }],
    });
    store = TestBed.inject(StaffCalendarStore);
  });

  afterEach(() => vi.useRealTimers());

  it('loads the current Branch-local Monday-to-Monday window', async () => {
    await store.initialise('Africa/Johannesburg');

    expect(api.getCalendar).toHaveBeenCalledWith(
      '2026-11-08T22:00:00.000Z',
      '2026-11-15T22:00:00.000Z',
    );
    expect(store.calendar()).toEqual(calendar);
  });

  it('refreshes the visible week after a successful block command', async () => {
    await store.initialise('Africa/Johannesburg');
    const command = { startAt: block.startAt, endAt: block.endAt };

    expect(await store.createBlock(command)).toBe(true);
    expect(api.createBlock).toHaveBeenCalledWith(command);
    expect(api.getCalendar).toHaveBeenCalledTimes(2);
  });
});

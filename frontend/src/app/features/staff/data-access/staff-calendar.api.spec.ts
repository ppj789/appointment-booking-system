import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StaffCalendarApi } from './staff-calendar.api';

describe('StaffCalendarApi', () => {
  let api: StaffCalendarApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [StaffCalendarApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(StaffCalendarApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads an explicit UTC calendar window', () => {
    const from = '2026-11-08T22:00:00.000Z';
    const to = '2026-11-15T22:00:00.000Z';

    api.getCalendar(from, to).subscribe();
    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/me/calendar' &&
        candidate.params.get('from') === from &&
        candidate.params.get('to') === to,
    );
    expect(request.request.method).toBe('GET');
    request.flush({ from, toExclusive: to, appointments: [], blocks: [] });
  });

  it('creates and removes Availability Blocks through owned endpoints', () => {
    const command = {
      startAt: '2026-11-13T12:00:00.000Z',
      endAt: '2026-11-13T13:00:00.000Z',
    };
    api.createBlock(command).subscribe();
    const create = http.expectOne('/api/v1/me/availability-blocks');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(command);
    create.flush({ id: 'block-id', ...command });

    api.removeBlock('block-id').subscribe();
    const remove = http.expectOne('/api/v1/me/availability-blocks/block-id');
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});

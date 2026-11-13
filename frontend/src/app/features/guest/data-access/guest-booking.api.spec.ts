import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GuestBookingApi } from './guest-booking.api';

describe('GuestBookingApi', () => {
  let api: GuestBookingApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GuestBookingApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(GuestBookingApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends the capability only through the Invitation header', () => {
    api.getInvitation('raw-capability').subscribe();

    const request = http.expectOne('/api/v1/guest/invitation');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('X-Invitation-Token')).toBe('raw-capability');
    expect(request.request.urlWithParams).not.toContain('raw-capability');
    request.flush({});
  });

  it('requests bounded availability for the selected duration', () => {
    api.getAvailability('raw-capability', 45).subscribe();

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/v1/guest/availability' &&
        candidate.params.get('durationMinutes') === '45' &&
        candidate.params.get('days') === '14',
    );
    expect(request.request.headers.get('X-Invitation-Token')).toBe('raw-capability');
    request.flush({});
  });

  it('confirms with separate capability and idempotency headers', () => {
    const command = { startAt: '2026-11-16T06:00:00.000Z', durationMinutes: 60 };
    api.confirmAppointment('raw-capability', 'idempotency-id', command).subscribe();

    const request = http.expectOne('/api/v1/guest/appointments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    expect(request.request.headers.get('X-Invitation-Token')).toBe('raw-capability');
    expect(request.request.headers.get('Idempotency-Key')).toBe('idempotency-id');
    request.flush({});
  });
});

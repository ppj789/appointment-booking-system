import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { GuestBookingApi } from './guest-booking.api';
import {
  GuestAppointmentCommand,
  GuestAppointmentConfirmation,
  GuestAvailability,
  GuestInvitationContext,
} from './guest-booking.models';
import { GuestBookingStore } from './guest-booking.store';

describe('GuestBookingStore', () => {
  const invitation: GuestInvitationContext = {
    title: 'Account discussion',
    description: 'Review account options.',
    hostDisplayName: 'Sam Staff',
    guestDisplayName: 'Alex Guest',
    defaultDurationMinutes: 60,
    supportedDurationsMinutes: [15, 30, 45, 60, 120, 240],
    branch: {
      publicCode: 'JHB-001',
      name: 'Johannesburg Demo Branch',
      displayAddress: '1 Demo Street, Johannesburg',
      timezone: 'Africa/Johannesburg',
    },
  };
  const availability: GuestAvailability = {
    durationMinutes: 60,
    generatedAt: '2026-11-13T10:00:00.000Z',
    window: {
      from: '2026-11-13T10:15:00.000Z',
      toExclusive: '2026-11-27T10:15:00.000Z',
    },
    slots: [{ startAt: '2026-11-16T06:00:00.000Z', endAt: '2026-11-16T07:00:00.000Z' }],
  };
  const confirmation: GuestAppointmentConfirmation = {
    appointment: {
      reference: 'APT-6H8KM4Q2TX',
      title: invitation.title,
      description: invitation.description,
      hostDisplayName: invitation.hostDisplayName,
      guestDisplayName: invitation.guestDisplayName,
      guestEmail: 'alex@example.test',
      startAt: availability.slots[0].startAt,
      endAt: availability.slots[0].endAt,
      createdAt: '2026-11-13T10:01:00.000Z',
      branch: invitation.branch,
    },
    confirmation: {
      status: 'CONFIRMED',
      message: 'Your appointment is confirmed.',
      reference: 'APT-6H8KM4Q2TX',
    },
  };
  let api: {
    getInvitation: ReturnType<
      typeof vi.fn<(rawToken: string) => Observable<GuestInvitationContext>>
    >;
    getAvailability: ReturnType<
      typeof vi.fn<(rawToken: string, durationMinutes: number) => Observable<GuestAvailability>>
    >;
    confirmAppointment: ReturnType<
      typeof vi.fn<
        (
          rawToken: string,
          idempotencyKey: string,
          command: GuestAppointmentCommand,
        ) => Observable<GuestAppointmentConfirmation>
      >
    >;
  };
  let store: GuestBookingStore;

  beforeEach(() => {
    api = {
      getInvitation: vi.fn(() => of(invitation)),
      getAvailability: vi.fn(() => of(availability)),
      confirmAppointment: vi.fn(() => of(confirmation)),
    };
    TestBed.configureTestingModule({
      providers: [GuestBookingStore, { provide: GuestBookingApi, useValue: api }],
    });
    store = TestBed.inject(GuestBookingStore);
  });

  it('loads context and availability using the Invitation default duration', async () => {
    await store.initialise('raw-capability');

    expect(api.getInvitation).toHaveBeenCalledWith('raw-capability');
    expect(api.getAvailability).toHaveBeenCalledWith('raw-capability', 60);
    expect(store.invitation()).toEqual(invitation);
    expect(store.availability()).toEqual(availability);
  });

  it('reuses the same idempotency key when a confirmation retry is required', async () => {
    api.confirmAppointment
      .mockReturnValueOnce(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 503,
              error: { code: 'SERVICE_UNAVAILABLE', detail: 'Try the request again later.' },
            }),
        ),
      )
      .mockReturnValueOnce(of(confirmation));
    await store.initialise('raw-capability');
    store.selectSlot(availability.slots[0]);

    expect(await store.confirmSelected()).toBe(false);
    expect(await store.confirmSelected()).toBe(true);

    const firstKey = api.confirmAppointment.mock.calls[0][1];
    const secondKey = api.confirmAppointment.mock.calls[1][1];
    expect(firstKey).toBe(secondKey);
    expect(store.confirmation()).toEqual(confirmation);
  });
});

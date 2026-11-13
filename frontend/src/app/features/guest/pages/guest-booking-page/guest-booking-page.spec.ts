import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { GuestBookingApi } from '../../data-access/guest-booking.api';
import { GuestBookingPage } from './guest-booking-page';

describe('GuestBookingPage', () => {
  it('captures the fragment capability and immediately removes it from the address bar', async () => {
    const location = { replaceState: vi.fn() };
    const api = {
      getInvitation: vi.fn(() =>
        of({
          title: 'Account discussion',
          description: 'Review account options.',
          hostDisplayName: 'Sam Staff',
          guestDisplayName: 'Alex Guest',
          defaultDurationMinutes: 60,
          supportedDurationsMinutes: [60],
          branch: {
            publicCode: 'JHB-001',
            name: 'Johannesburg Demo Branch',
            displayAddress: '1 Demo Street, Johannesburg',
            timezone: 'Africa/Johannesburg',
          },
        }),
      ),
      getAvailability: vi.fn(() =>
        of({
          durationMinutes: 60,
          generatedAt: '2026-11-13T10:00:00.000Z',
          window: {
            from: '2026-11-13T10:15:00.000Z',
            toExclusive: '2026-11-27T10:15:00.000Z',
          },
          slots: [],
        }),
      ),
      confirmAppointment: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [GuestBookingPage],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { fragment: 'token=raw-capability' } } },
        { provide: Location, useValue: location },
        { provide: GuestBookingApi, useValue: api },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(GuestBookingPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(location.replaceState).toHaveBeenCalledWith('/book');
    expect(api.getInvitation).toHaveBeenCalledWith('raw-capability');
  });
});

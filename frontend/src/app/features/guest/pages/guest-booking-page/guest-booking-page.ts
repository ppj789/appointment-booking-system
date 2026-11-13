import { Location } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute } from '@angular/router';
import { BranchDateTimePipe } from '../../../../core/time/branch-date-time';
import { AppError } from '../../../../core/http/app-error';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { LoadingState } from '../../../../shared/ui/loading-state/loading-state';
import { GuestAvailabilitySlot, guestDurationLabel } from '../../data-access/guest-booking.models';
import { GuestBookingStore } from '../../data-access/guest-booking.store';
import { formatGuestSlotTime, groupGuestSlotsByBranchDate } from './guest-slot-groups';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    BranchDateTimePipe,
    EmptyState,
    ErrorState,
    LoadingState,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  providers: [GuestBookingStore],
  selector: 'app-guest-booking-page',
  styleUrl: './guest-booking-page.scss',
  templateUrl: './guest-booking-page.html',
})
export class GuestBookingPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  protected readonly store = inject(GuestBookingStore);
  protected readonly selectedDate = signal<string | null>(null);
  protected readonly dateGroups = computed(() => {
    const invitation = this.store.invitation();
    const availability = this.store.availability();
    if (!invitation || !availability) {
      return [];
    }
    return groupGuestSlotsByBranchDate(availability.slots, invitation.branch.timezone);
  });
  protected readonly visibleSlots = computed(
    () => this.dateGroups().find((group) => group.dateKey === this.selectedDate())?.slots ?? [],
  );

  constructor() {
    effect(() => {
      const groups = this.dateGroups();
      const selectedDate = this.selectedDate();
      if (!groups.some((group) => group.dateKey === selectedDate)) {
        this.selectedDate.set(groups[0]?.dateKey ?? null);
      }
    });
  }

  ngOnInit(): void {
    const fragment = this.route.snapshot.fragment ?? '';
    const rawToken = new URLSearchParams(fragment).get('token');
    this.location.replaceState('/book');
    void this.store.initialise(rawToken);
  }

  protected durationLabel(minutes: number): string {
    return guestDurationLabel(minutes);
  }

  protected async changeDuration(durationMinutes: number): Promise<void> {
    this.selectedDate.set(null);
    await this.store.changeDuration(durationMinutes);
  }

  protected selectDate(dateKey: string): void {
    this.selectedDate.set(dateKey);
  }

  protected slotTime(slot: GuestAvailabilitySlot): string {
    const timezone = this.store.invitation()?.branch.timezone;
    return timezone ? formatGuestSlotTime(slot.startAt, timezone) : '';
  }

  protected isSelected(slot: GuestAvailabilitySlot): boolean {
    return this.store.selectedSlot()?.startAt === slot.startAt;
  }

  protected canRetry(error: AppError): boolean {
    return !['INVITATION_UNAVAILABLE', 'SLOT_UNAVAILABLE', 'RATE_LIMITED'].includes(
      error.code ?? '',
    );
  }
}

import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppError, toAppError } from '../../../core/http/app-error';
import { GuestBookingApi } from './guest-booking.api';
import {
  GuestAppointmentCommand,
  GuestAppointmentConfirmation,
  GuestAvailability,
  GuestAvailabilitySlot,
  GuestInvitationContext,
} from './guest-booking.models';

interface ConfirmationRequest {
  fingerprint: string;
  idempotencyKey: string;
}

@Injectable()
export class GuestBookingStore {
  private readonly api = inject(GuestBookingApi);
  private rawToken: string | null = null;
  private confirmationRequest: ConfirmationRequest | null = null;
  private availabilitySequence = 0;

  private readonly invitationState = signal<GuestInvitationContext | null>(null);
  private readonly availabilityState = signal<GuestAvailability | null>(null);
  private readonly durationState = signal<number | null>(null);
  private readonly selectedSlotState = signal<GuestAvailabilitySlot | null>(null);
  private readonly confirmationState = signal<GuestAppointmentConfirmation | null>(null);
  private readonly loadingState = signal(false);
  private readonly availabilityLoadingState = signal(false);
  private readonly confirmingState = signal(false);
  private readonly errorState = signal<AppError | null>(null);

  readonly invitation = this.invitationState.asReadonly();
  readonly availability = this.availabilityState.asReadonly();
  readonly durationMinutes = this.durationState.asReadonly();
  readonly selectedSlot = this.selectedSlotState.asReadonly();
  readonly confirmation = this.confirmationState.asReadonly();
  readonly loading = this.loadingState.asReadonly();
  readonly availabilityLoading = this.availabilityLoadingState.asReadonly();
  readonly confirming = this.confirmingState.asReadonly();
  readonly error = this.errorState.asReadonly();

  async initialise(rawToken: string | null): Promise<void> {
    if (!rawToken) {
      this.errorState.set({
        message: 'This booking link is unavailable. Ask the Staff member for a new link.',
        code: 'INVITATION_UNAVAILABLE',
      });
      return;
    }
    this.rawToken = rawToken;
    await this.loadJourney();
  }

  async retry(): Promise<void> {
    if (!this.rawToken) {
      return;
    }
    if (!this.invitationState()) {
      await this.loadJourney();
      return;
    }
    await this.loadAvailability();
  }

  async changeDuration(durationMinutes: number): Promise<void> {
    const invitation = this.invitationState();
    if (
      !invitation ||
      !invitation.supportedDurationsMinutes.includes(durationMinutes) ||
      durationMinutes === this.durationState()
    ) {
      return;
    }
    this.durationState.set(durationMinutes);
    this.selectedSlotState.set(null);
    this.confirmationRequest = null;
    await this.loadAvailability();
  }

  selectSlot(slot: GuestAvailabilitySlot): void {
    this.errorState.set(null);
    this.selectedSlotState.set(slot);
    this.confirmationRequest = null;
  }

  async confirmSelected(): Promise<boolean> {
    const rawToken = this.rawToken;
    const slot = this.selectedSlotState();
    const durationMinutes = this.durationState();
    if (!rawToken || !slot || !durationMinutes || this.confirmingState()) {
      return false;
    }

    const command: GuestAppointmentCommand = { startAt: slot.startAt, durationMinutes };
    const fingerprint = `${command.startAt}|${command.durationMinutes}`;
    if (this.confirmationRequest?.fingerprint !== fingerprint) {
      this.confirmationRequest = {
        fingerprint,
        idempotencyKey: globalThis.crypto.randomUUID(),
      };
    }

    this.errorState.set(null);
    this.confirmingState.set(true);
    try {
      const confirmation = await firstValueFrom(
        this.api.confirmAppointment(rawToken, this.confirmationRequest.idempotencyKey, command),
      );
      this.confirmationState.set(confirmation);
      this.rawToken = null;
      this.confirmationRequest = null;
      return true;
    } catch (error: unknown) {
      const appError = toAppError(error);
      if (appError.code === 'SLOT_UNAVAILABLE') {
        this.selectedSlotState.set(null);
        this.confirmationRequest = null;
        await this.loadAvailability(false);
      }
      if (appError.code === 'INVITATION_UNAVAILABLE') {
        this.invalidateInvitation();
      }
      this.errorState.set(appError);
      return false;
    } finally {
      this.confirmingState.set(false);
    }
  }

  private async loadJourney(): Promise<void> {
    const rawToken = this.rawToken;
    if (!rawToken) {
      return;
    }
    this.loadingState.set(true);
    this.errorState.set(null);
    try {
      const invitation = await firstValueFrom(this.api.getInvitation(rawToken));
      this.invitationState.set(invitation);
      this.durationState.set(invitation.defaultDurationMinutes);
      await this.loadAvailability(false);
    } catch (error: unknown) {
      const appError = toAppError(error);
      if (appError.code === 'INVITATION_UNAVAILABLE') {
        this.invalidateInvitation();
      }
      this.errorState.set(appError);
    } finally {
      this.loadingState.set(false);
    }
  }

  private async loadAvailability(clearError = true): Promise<void> {
    const rawToken = this.rawToken;
    const durationMinutes = this.durationState();
    if (!rawToken || !durationMinutes) {
      return;
    }
    const sequence = ++this.availabilitySequence;
    this.availabilityLoadingState.set(true);
    if (clearError) {
      this.errorState.set(null);
    }
    try {
      const availability = await firstValueFrom(
        this.api.getAvailability(rawToken, durationMinutes),
      );
      if (sequence === this.availabilitySequence) {
        this.availabilityState.set(availability);
        this.selectedSlotState.set(null);
      }
    } catch (error: unknown) {
      if (sequence === this.availabilitySequence) {
        const appError = toAppError(error);
        if (appError.code === 'INVITATION_UNAVAILABLE') {
          this.invalidateInvitation();
        }
        this.errorState.set(appError);
      }
    } finally {
      if (sequence === this.availabilitySequence) {
        this.availabilityLoadingState.set(false);
      }
    }
  }

  private invalidateInvitation(): void {
    this.rawToken = null;
    this.confirmationRequest = null;
    this.invitationState.set(null);
    this.availabilityState.set(null);
    this.durationState.set(null);
    this.selectedSlotState.set(null);
  }
}

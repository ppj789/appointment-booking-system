import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { BranchCalendarWeek, getBranchCalendarWeek } from '../../../core/time/branch-calendar';
import { AppError, toAppError } from '../../../core/http/app-error';
import { StaffCalendarApi } from './staff-calendar.api';
import { CreateAvailabilityBlockCommand, StaffCalendar } from './staff-calendar.models';

@Injectable()
export class StaffCalendarStore {
  private readonly api = inject(StaffCalendarApi);
  private readonly weekState = signal<BranchCalendarWeek | null>(null);
  private readonly calendarState = signal<StaffCalendar | null>(null);
  private readonly loadingState = signal(false);
  private readonly actionState = signal(false);
  private readonly errorState = signal<AppError | null>(null);
  private timezone = '';
  private requestSequence = 0;

  readonly week = this.weekState.asReadonly();
  readonly calendar = this.calendarState.asReadonly();
  readonly loading = this.loadingState.asReadonly();
  readonly actionPending = this.actionState.asReadonly();
  readonly error = this.errorState.asReadonly();

  async initialise(timezone: string): Promise<void> {
    this.timezone = timezone;
    await this.loadWeek(new Date());
  }

  async previousWeek(): Promise<void> {
    await this.shiftWeek(-7);
  }

  async nextWeek(): Promise<void> {
    await this.shiftWeek(7);
  }

  async currentWeek(): Promise<void> {
    await this.loadWeek(new Date());
  }

  async refresh(): Promise<void> {
    const week = this.weekState();
    if (week) {
      await this.fetchCalendar(week);
    }
  }

  async createBlock(command: CreateAvailabilityBlockCommand): Promise<boolean> {
    this.actionState.set(true);
    this.errorState.set(null);
    try {
      await firstValueFrom(this.api.createBlock(command));
      await this.refresh();
      return true;
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
      return false;
    } finally {
      this.actionState.set(false);
    }
  }

  async removeBlock(blockId: string): Promise<boolean> {
    this.actionState.set(true);
    this.errorState.set(null);
    try {
      await firstValueFrom(this.api.removeBlock(blockId));
      await this.refresh();
      return true;
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
      return false;
    } finally {
      this.actionState.set(false);
    }
  }

  clearError(): void {
    this.errorState.set(null);
  }

  private async shiftWeek(days: number): Promise<void> {
    const week = this.weekState();
    if (!week) {
      return;
    }
    const reference = new Date(new Date(week.from).getTime() + days * 24 * 60 * 60 * 1000);
    await this.loadWeek(reference);
  }

  private async loadWeek(reference: Date): Promise<void> {
    if (!this.timezone) {
      return;
    }
    const week = getBranchCalendarWeek(reference, this.timezone);
    this.weekState.set(week);
    await this.fetchCalendar(week);
  }

  private async fetchCalendar(week: BranchCalendarWeek): Promise<void> {
    const sequence = ++this.requestSequence;
    this.loadingState.set(true);
    this.errorState.set(null);
    try {
      const calendar = await firstValueFrom(this.api.getCalendar(week.from, week.toExclusive));
      if (sequence === this.requestSequence) {
        this.calendarState.set(calendar);
      }
    } catch (error: unknown) {
      if (sequence === this.requestSequence) {
        this.errorState.set(toAppError(error));
      }
    } finally {
      if (sequence === this.requestSequence) {
        this.loadingState.set(false);
      }
    }
  }
}

import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import {
  branchMinutesOfDay,
  BranchCalendarDay,
  BranchCalendarWeek,
} from '../../../../core/time/branch-calendar';
import { BranchDateTimePipe } from '../../../../core/time/branch-date-time';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { LoadingState } from '../../../../shared/ui/loading-state/loading-state';
import {
  CreateBlockDialog,
  CreateBlockDialogData,
} from '../create-block-dialog/create-block-dialog';
import {
  RemoveBlockDialog,
  RemoveBlockDialogData,
} from '../remove-block-dialog/remove-block-dialog';
import { StaffCalendarStore } from '../../data-access/staff-calendar.store';
import {
  AvailabilityBlock,
  CalendarAppointment,
  StaffCalendar,
} from '../../data-access/staff-calendar.models';
import {
  CALENDAR_SLOTS_PER_DAY,
  CalendarDraftSelection,
  calendarSelectionCommand,
  calendarSelectionFromSlots,
  isCalendarSelectionFuture,
  MINUTES_PER_CALENDAR_SLOT,
} from './staff-calendar-selection';

interface CalendarSegment {
  id: string;
  kind: 'appointment' | 'block';
  title: string;
  detail: string;
  time: string;
  top: number;
  height: number;
}

const HOUR_HEIGHT = 48;

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BranchDateTimePipe, EmptyState, ErrorState, LoadingState, MatButtonModule],
  providers: [StaffCalendarStore],
  selector: 'app-staff-calendar-panel',
  styleUrl: './staff-calendar-panel.scss',
  templateUrl: './staff-calendar-panel.html',
})
export class StaffCalendarPanel implements OnInit {
  private readonly dialog = inject(MatDialog);
  protected readonly store = inject(StaffCalendarStore);
  private readonly calendarScroll = viewChild<ElementRef<HTMLElement>>('calendarScroll');
  readonly timezone = input.required<string>();
  protected readonly hours = Array.from({ length: 24 }, (_, hour) => hour);
  protected readonly draftSelection = signal<CalendarDraftSelection | null>(null);
  protected readonly selectionError = signal<string | null>(null);
  protected readonly segmentsByDay = computed(() => {
    const calendar = this.store.calendar();
    const week = this.store.week();
    if (!calendar || !week) {
      return Array.from({ length: 7 }, () => [] as CalendarSegment[]);
    }
    return buildSegments(calendar, week, this.timezone());
  });
  protected readonly blocks = computed(() =>
    [...(this.store.calendar()?.blocks ?? [])].sort(
      (left, right) => new Date(left.startAt).getTime() - new Date(right.startAt).getTime(),
    ),
  );

  constructor() {
    effect(() => {
      const container = this.calendarScroll()?.nativeElement;
      if (container) {
        globalThis.requestAnimationFrame(() => {
          container.scrollTop = 8 * HOUR_HEIGHT;
        });
      }
    });
  }

  ngOnInit(): void {
    void this.store.initialise(this.timezone());
  }

  protected hourLabel(hour: number): string {
    return `${hour.toString().padStart(2, '0')}:00`;
  }

  protected async removeBlock(block: AvailabilityBlock): Promise<void> {
    this.store.clearError();
    const confirmed = await firstValueFrom(
      this.dialog
        .open<RemoveBlockDialog, RemoveBlockDialogData, boolean>(RemoveBlockDialog, {
          data: { block, timezone: this.timezone() },
          width: '32rem',
        })
        .afterClosed(),
    );
    if (confirmed) {
      await this.store.removeBlock(block.id);
    }
  }

  protected canRemove(block: AvailabilityBlock): boolean {
    return new Date(block.startAt).getTime() > Date.now();
  }

  protected startSelection(event: PointerEvent, dayIndex: number): void {
    if ((event.pointerType === 'mouse' && event.button !== 0) || this.store.actionPending()) {
      return;
    }
    this.selectionError.set(null);
    event.preventDefault();
    const target = event.currentTarget as HTMLElement;
    target.setPointerCapture(event.pointerId);
    const slot = pointerAnchorSlot(event, target);
    this.draftSelection.set(calendarSelectionFromSlots(dayIndex, event.pointerId, slot, slot));
  }

  protected updateSelection(event: PointerEvent, dayIndex: number): void {
    const selection = this.draftSelection();
    if (selection?.pointerId !== event.pointerId || selection.dayIndex !== dayIndex) {
      return;
    }
    event.preventDefault();
    const slot = pointerSelectionSlot(
      event,
      event.currentTarget as HTMLElement,
      selection.anchorSlot,
    );
    this.draftSelection.set(
      calendarSelectionFromSlots(dayIndex, selection.pointerId, selection.anchorSlot, slot),
    );
  }

  protected async finishSelection(event: PointerEvent, day: BranchCalendarDay): Promise<void> {
    const selection = this.draftSelection();
    if (selection?.pointerId !== event.pointerId) {
      return;
    }
    event.preventDefault();
    const finalSlot = pointerSelectionSlot(
      event,
      event.currentTarget as HTMLElement,
      selection.anchorSlot,
    );
    const finalSelection = calendarSelectionFromSlots(
      selection.dayIndex,
      selection.pointerId,
      selection.anchorSlot,
      finalSlot,
    );
    this.draftSelection.set(finalSelection);
    await this.confirmSelection(day, finalSelection);
  }

  protected cancelSelection(event: PointerEvent): void {
    if (this.draftSelection()?.pointerId === event.pointerId) {
      this.draftSelection.set(null);
    }
  }

  protected prepareKeyboardSelection(dayIndex: number): void {
    const selection = this.draftSelection();
    if (!selection || selection.dayIndex !== dayIndex) {
      const eightAmSlot = (8 * 60) / MINUTES_PER_CALENDAR_SLOT;
      this.draftSelection.set(calendarSelectionFromSlots(dayIndex, null, eightAmSlot, eightAmSlot));
    }
  }

  protected async handleCalendarKey(
    event: KeyboardEvent,
    dayIndex: number,
    day: BranchCalendarDay,
  ): Promise<void> {
    this.prepareKeyboardSelection(dayIndex);
    this.selectionError.set(null);
    const selection = this.draftSelection();
    if (!selection) {
      return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      await this.confirmSelection(day, selection);
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.draftSelection.set(null);
      return;
    }
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') {
      return;
    }
    event.preventDefault();
    const direction = event.key === 'ArrowDown' ? 1 : -1;
    if (event.shiftKey) {
      this.draftSelection.set(
        direction > 0
          ? {
              ...selection,
              pointerId: null,
              endMinutes: Math.min(selection.endMinutes + MINUTES_PER_CALENDAR_SLOT, 24 * 60),
            }
          : {
              ...selection,
              pointerId: null,
              startMinutes: Math.max(selection.startMinutes - MINUTES_PER_CALENDAR_SLOT, 0),
            },
      );
      return;
    }
    const durationSlots =
      (selection.endMinutes - selection.startMinutes) / MINUTES_PER_CALENDAR_SLOT;
    const startSlot = Math.min(
      Math.max(selection.startMinutes / MINUTES_PER_CALENDAR_SLOT + direction, 0),
      CALENDAR_SLOTS_PER_DAY - durationSlots,
    );
    this.draftSelection.set({
      ...selection,
      pointerId: null,
      anchorSlot: startSlot,
      startMinutes: startSlot * MINUTES_PER_CALENDAR_SLOT,
      endMinutes: (startSlot + durationSlots) * MINUTES_PER_CALENDAR_SLOT,
    });
  }

  private async confirmSelection(
    day: BranchCalendarDay,
    selection: CalendarDraftSelection,
  ): Promise<void> {
    this.store.clearError();
    const command = calendarSelectionCommand(day, selection, this.timezone());
    this.draftSelection.set(null);
    if (!isCalendarSelectionFuture(command)) {
      this.selectionError.set(
        'Unavailable time must start in the future. Choose a later time or another day.',
      );
      return;
    }
    this.selectionError.set(null);
    const confirmed = await firstValueFrom(
      this.dialog
        .open<CreateBlockDialog, CreateBlockDialogData, boolean>(CreateBlockDialog, {
          data: { timezone: this.timezone(), command },
          width: '34rem',
        })
        .afterClosed(),
    );
    if (confirmed) {
      await this.store.createBlock(command);
    }
  }

  protected elapsedMinutes(day: BranchCalendarDay): number {
    const now = new Date();
    if (now.getTime() >= new Date(day.endAt).getTime()) {
      return 24 * 60;
    }
    if (now.getTime() < new Date(day.startAt).getTime()) {
      return 0;
    }
    const currentMinutes = branchMinutesOfDay(now, this.timezone());
    return Math.min(
      Math.ceil(currentMinutes / MINUTES_PER_CALENDAR_SLOT) * MINUTES_PER_CALENDAR_SLOT,
      24 * 60,
    );
  }
}

function pointerAnchorSlot(event: PointerEvent, target: HTMLElement): number {
  const offset = event.clientY - target.getBoundingClientRect().top;
  const slotHeight = (HOUR_HEIGHT * MINUTES_PER_CALENDAR_SLOT) / 60;
  return Math.min(Math.max(Math.floor(offset / slotHeight), 0), CALENDAR_SLOTS_PER_DAY - 1);
}

function pointerSelectionSlot(
  event: PointerEvent,
  target: HTMLElement,
  anchorSlot: number,
): number {
  const offset = event.clientY - target.getBoundingClientRect().top;
  const slotHeight = (HOUR_HEIGHT * MINUTES_PER_CALENDAR_SLOT) / 60;
  const rawSlot = offset / slotHeight;
  const currentSlot =
    rawSlot >= anchorSlot ? Math.max(Math.ceil(rawSlot) - 1, anchorSlot) : Math.floor(rawSlot);
  return Math.min(Math.max(currentSlot, 0), CALENDAR_SLOTS_PER_DAY - 1);
}

function buildSegments(
  calendar: StaffCalendar,
  week: BranchCalendarWeek,
  timezone: string,
): CalendarSegment[][] {
  const result = Array.from({ length: 7 }, () => [] as CalendarSegment[]);
  const entries: Array<
    | { kind: 'appointment'; value: CalendarAppointment }
    | { kind: 'block'; value: AvailabilityBlock }
  > = [
    ...calendar.appointments.map((value) => ({ kind: 'appointment' as const, value })),
    ...calendar.blocks.map((value) => ({ kind: 'block' as const, value })),
  ];

  for (const entry of entries) {
    const entryStart = new Date(entry.value.startAt).getTime();
    const entryEnd = new Date(entry.value.endAt).getTime();
    week.days.forEach((day, dayIndex) => {
      const dayStart = new Date(day.startAt).getTime();
      const dayEnd = new Date(day.endAt).getTime();
      const segmentStart = Math.max(entryStart, dayStart);
      const segmentEnd = Math.min(entryEnd, dayEnd);
      if (segmentEnd <= segmentStart) {
        return;
      }
      const startMinutes =
        segmentStart === dayStart ? 0 : branchMinutesOfDay(new Date(segmentStart), timezone);
      const endMinutes =
        segmentEnd === dayEnd ? 24 * 60 : branchMinutesOfDay(new Date(segmentEnd), timezone);
      const appointment = entry.kind === 'appointment' ? entry.value : null;
      result[dayIndex].push({
        id: `${entry.kind}-${entry.value.id}-${day.dateKey}`,
        kind: entry.kind,
        title: appointment?.title ?? 'Unavailable',
        detail: appointment
          ? `${appointment.guestDisplayName} · ${appointment.reference}`
          : 'Availability block',
        time: formatTimeRange(entry.value.startAt, entry.value.endAt, timezone),
        top: (startMinutes / 60) * HOUR_HEIGHT,
        height: Math.max(((endMinutes - startMinutes) / 60) * HOUR_HEIGHT, 24),
      });
    });
  }
  return result;
}

function formatTimeRange(startAt: string, endAt: string, timezone: string): string {
  const formatter = new Intl.DateTimeFormat('en-ZA', {
    timeZone: timezone,
    hour: '2-digit',
    minute: '2-digit',
  });
  return `${formatter.format(new Date(startAt))}–${formatter.format(new Date(endAt))}`;
}

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { BranchDateTimePipe } from '../../../../core/time/branch-date-time';
import { AvailabilityBlock } from '../../data-access/staff-calendar.models';

export interface RemoveBlockDialogData {
  block: AvailabilityBlock;
  timezone: string;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BranchDateTimePipe, MatButtonModule, MatDialogModule],
  selector: 'app-remove-block-dialog',
  template: `
    <h2 mat-dialog-title>Remove unavailable time?</h2>
    <mat-dialog-content>
      <p>
        {{ data.block.startAt | branchDateTime: data.timezone }} to
        {{ data.block.endAt | branchDateTime: data.timezone }} will become available again.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">Remove block</button>
    </mat-dialog-actions>
  `,
})
export class RemoveBlockDialog {
  protected readonly data = inject<RemoveBlockDialogData>(MAT_DIALOG_DATA);
}

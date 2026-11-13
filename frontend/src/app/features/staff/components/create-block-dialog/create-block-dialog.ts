import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { BranchDateTimePipe } from '../../../../core/time/branch-date-time';
import { CreateAvailabilityBlockCommand } from '../../data-access/staff-calendar.models';

export interface CreateBlockDialogData {
  timezone: string;
  command: CreateAvailabilityBlockCommand;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BranchDateTimePipe, MatButtonModule, MatDialogModule],
  selector: 'app-create-block-dialog',
  styleUrl: './create-block-dialog.scss',
  templateUrl: './create-block-dialog.html',
})
export class CreateBlockDialog {
  protected readonly data = inject<CreateBlockDialogData>(MAT_DIALOG_DATA);
  protected readonly timezone = this.data.timezone;
}

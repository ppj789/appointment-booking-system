import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  durationLabel,
  InvitationDraftCommand,
  invitationDurations,
  InvitationSummary,
} from '../../data-access/staff-invitation.models';

export interface InvitationDraftDialogData {
  invitation?: InvitationSummary;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  selector: 'app-invitation-draft-dialog',
  styleUrl: './invitation-draft-dialog.scss',
  templateUrl: './invitation-draft-dialog.html',
})
export class InvitationDraftDialog {
  private readonly data = inject<InvitationDraftDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<InvitationDraftDialog, InvitationDraftCommand>);
  protected readonly editing = Boolean(this.data.invitation);
  protected readonly durations = invitationDurations;
  protected readonly durationLabel = durationLabel;
  protected readonly form = new FormGroup({
    title: new FormControl(this.data.invitation?.title ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    description: new FormControl(this.data.invitation?.description ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(2000)],
    }),
    guestDisplayName: new FormControl(this.data.invitation?.guest.displayName ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    guestEmail: new FormControl(this.data.invitation?.guest.email ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    defaultDurationMinutes: new FormControl(this.data.invitation?.defaultDurationMinutes ?? 30, {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    this.dialogRef.close({
      title: values.title,
      description: values.description,
      guest: { displayName: values.guestDisplayName, email: values.guestEmail },
      defaultDurationMinutes: values.defaultDurationMinutes,
    });
  }
}

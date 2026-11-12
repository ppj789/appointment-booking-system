import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { ManagedStaff } from '../../data-access/admin-staff.models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  selector: 'app-deactivate-staff-dialog',
  template: `
    <h2 mat-dialog-title>Deactivate Staff?</h2>
    <mat-dialog-content>
      <p>
        <strong>{{ staff.displayName }}</strong> will no longer be able to sign in or accept new
        bookings. Historical records will be preserved.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">Deactivate</button>
    </mat-dialog-actions>
  `,
})
export class DeactivateStaffDialog {
  protected readonly staff = inject<ManagedStaff>(MAT_DIALOG_DATA);
}

import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { ManagedStaff } from '../../data-access/admin-staff.models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  selector: 'app-replace-setup-dialog',
  template: `
    <h2 mat-dialog-title>Replace setup link?</h2>
    <mat-dialog-content>
      <p>
        Any previous setup link for <strong>{{ staff.displayName }}</strong> will stop working. The
        replacement link will be shown only once.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">Replace link</button>
    </mat-dialog-actions>
  `,
})
export class ReplaceSetupDialog {
  protected readonly staff = inject<ManagedStaff>(MAT_DIALOG_DATA);
}

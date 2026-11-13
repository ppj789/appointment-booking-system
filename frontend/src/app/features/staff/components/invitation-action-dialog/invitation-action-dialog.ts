import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { InvitationSummary } from '../../data-access/staff-invitation.models';

export interface InvitationActionDialogData {
  action: 'activate' | 'revoke';
  invitation: InvitationSummary;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  selector: 'app-invitation-action-dialog',
  template: `
    <h2 mat-dialog-title>{{ data.action === 'activate' ? 'Activate' : 'Revoke' }} Invitation?</h2>
    <mat-dialog-content>
      @if (data.action === 'activate') {
        <p>
          <strong>{{ data.invitation.title }}</strong> will become immutable and its Guest link will
          be shown once.
        </p>
      } @else {
        <p>
          <strong>{{ data.invitation.title }}</strong> will no longer accept a booking. This cannot
          be undone.
        </p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancel</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">
        {{ data.action === 'activate' ? 'Activate and show link' : 'Revoke' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class InvitationActionDialog {
  protected readonly data = inject<InvitationActionDialogData>(MAT_DIALOG_DATA);
}

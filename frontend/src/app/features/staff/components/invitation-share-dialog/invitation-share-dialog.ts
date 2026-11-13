import { Clipboard } from '@angular/cdk/clipboard';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { InvitationActivation } from '../../data-access/staff-invitation.models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  selector: 'app-invitation-share-dialog',
  styleUrl: './invitation-share-dialog.scss',
  templateUrl: './invitation-share-dialog.html',
})
export class InvitationShareDialog {
  private readonly clipboard = inject(Clipboard);
  protected readonly activation = inject<InvitationActivation>(MAT_DIALOG_DATA);
  protected readonly copied = signal(false);
  protected readonly shareUrl = new URL(
    this.activation.shareUrl,
    globalThis.location.origin,
  ).toString();

  protected copy(): void {
    this.copied.set(this.clipboard.copy(this.shareUrl));
  }
}

import { Clipboard } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { StaffSetup } from '../../data-access/admin-staff.models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  selector: 'app-setup-link-dialog',
  styleUrl: './setup-link-dialog.scss',
  templateUrl: './setup-link-dialog.html',
})
export class SetupLinkDialog {
  private readonly clipboard = inject(Clipboard);
  protected readonly setup = inject<StaffSetup>(MAT_DIALOG_DATA);
  protected readonly copied = signal(false);
  protected readonly setupUrl = new URL(this.setup.setupUrl, globalThis.location.origin).toString();

  protected copy(): void {
    this.copied.set(this.clipboard.copy(this.setupUrl));
  }
}

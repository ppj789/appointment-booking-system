import { ChangeDetectionStrategy, Component, inject, input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';
import { BranchDateTimePipe } from '../../../../core/time/branch-date-time';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { LoadingState } from '../../../../shared/ui/loading-state/loading-state';
import {
  InvitationActionDialog,
  InvitationActionDialogData,
} from '../invitation-action-dialog/invitation-action-dialog';
import {
  InvitationDraftDialog,
  InvitationDraftDialogData,
} from '../invitation-draft-dialog/invitation-draft-dialog';
import { InvitationShareDialog } from '../invitation-share-dialog/invitation-share-dialog';
import {
  durationLabel,
  InvitationDraftCommand,
  InvitationSummary,
  invitationStatusLabel,
} from '../../data-access/staff-invitation.models';
import { StaffInvitationStore } from '../../data-access/staff-invitation.store';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BranchDateTimePipe, EmptyState, ErrorState, LoadingState, MatButtonModule],
  providers: [StaffInvitationStore],
  selector: 'app-staff-invitations-panel',
  styleUrl: './staff-invitations-panel.scss',
  templateUrl: './staff-invitations-panel.html',
})
export class StaffInvitationsPanel implements OnInit {
  private readonly dialog = inject(MatDialog);
  protected readonly store = inject(StaffInvitationStore);
  readonly timezone = input.required<string>();
  protected readonly durationLabel = durationLabel;
  protected readonly statusLabel = invitationStatusLabel;

  ngOnInit(): void {
    void this.store.load();
  }

  protected async createDraft(): Promise<void> {
    await this.openDraftDialog();
  }

  protected async editDraft(invitation: InvitationSummary): Promise<void> {
    await this.openDraftDialog(invitation);
  }

  protected async activate(invitation: InvitationSummary): Promise<void> {
    this.store.clearError();
    const confirmed = await this.confirmAction('activate', invitation);
    if (!confirmed) {
      return;
    }
    const activation = await this.store.activate(invitation.id);
    if (activation) {
      this.dialog.open(InvitationShareDialog, { data: activation, width: '40rem' });
    }
  }

  protected async revoke(invitation: InvitationSummary): Promise<void> {
    this.store.clearError();
    const confirmed = await this.confirmAction('revoke', invitation);
    if (confirmed) {
      await this.store.revoke(invitation.id);
    }
  }

  private async openDraftDialog(invitation?: InvitationSummary): Promise<void> {
    this.store.clearError();
    const command = await firstValueFrom(
      this.dialog
        .open<InvitationDraftDialog, InvitationDraftDialogData, InvitationDraftCommand>(
          InvitationDraftDialog,
          { data: { invitation }, width: '40rem' },
        )
        .afterClosed(),
    );
    if (command) {
      await this.store.save(command, invitation?.id);
    }
  }

  private confirmAction(
    action: 'activate' | 'revoke',
    invitation: InvitationSummary,
  ): Promise<boolean | undefined> {
    return firstValueFrom(
      this.dialog
        .open<InvitationActionDialog, InvitationActionDialogData, boolean>(InvitationActionDialog, {
          data: { action, invitation },
          width: '32rem',
        })
        .afterClosed(),
    );
  }
}

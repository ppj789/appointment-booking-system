import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { firstValueFrom } from 'rxjs';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { LoadingState } from '../../../../shared/ui/loading-state/loading-state';
import {
  CreateStaffDialog,
  CreateStaffDialogData,
} from '../../components/create-staff-dialog/create-staff-dialog';
import { DeactivateStaffDialog } from '../../components/deactivate-staff-dialog/deactivate-staff-dialog';
import { ReplaceSetupDialog } from '../../components/replace-setup-dialog/replace-setup-dialog';
import { SetupLinkDialog } from '../../components/setup-link-dialog/setup-link-dialog';
import { AdminStaffStore } from '../../data-access/admin-staff.store';
import {
  accountStatusLabel,
  CreateStaffCommand,
  ManagedStaff,
} from '../../data-access/admin-staff.models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    EmptyState,
    ErrorState,
    LoadingState,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
  ],
  providers: [AdminStaffStore],
  selector: 'app-staff-management-page',
  styleUrl: './staff-management-page.scss',
  templateUrl: './staff-management-page.html',
})
export class StaffManagementPage implements OnInit {
  private readonly dialog = inject(MatDialog);
  protected readonly store = inject(AdminStaffStore);
  protected readonly columns = ['name', 'email', 'status', 'actions'];
  protected readonly statusLabel = accountStatusLabel;

  ngOnInit(): void {
    void this.store.load();
  }

  protected filterByBranch(branchCode: string): void {
    void this.store.selectBranch(branchCode);
  }

  protected async addStaff(): Promise<void> {
    this.store.clearActionError();
    const branch = this.store.selectedBranch();
    if (!branch) {
      return;
    }
    const command = await firstValueFrom(
      this.dialog
        .open<CreateStaffDialog, CreateStaffDialogData, CreateStaffCommand>(CreateStaffDialog, {
          data: { branch },
          width: '36rem',
        })
        .afterClosed(),
    );
    if (!command) {
      return;
    }

    const setup = await this.store.provision(command);
    if (setup) {
      this.dialog.open(SetupLinkDialog, { data: setup, width: '40rem' });
    }
  }

  protected async replaceSetup(staff: ManagedStaff): Promise<void> {
    this.store.clearActionError();
    const confirmed = await firstValueFrom(
      this.dialog.open(ReplaceSetupDialog, { data: staff, width: '32rem' }).afterClosed(),
    );
    if (!confirmed) {
      return;
    }
    const setup = await this.store.replaceSetup(staff.id);
    if (setup) {
      this.dialog.open(SetupLinkDialog, { data: setup, width: '40rem' });
    }
  }

  protected async deactivate(staff: ManagedStaff): Promise<void> {
    this.store.clearActionError();
    const confirmed = await firstValueFrom(
      this.dialog.open(DeactivateStaffDialog, { data: staff, width: '32rem' }).afterClosed(),
    );
    if (confirmed) {
      await this.store.deactivate(staff.id);
    }
  }
}

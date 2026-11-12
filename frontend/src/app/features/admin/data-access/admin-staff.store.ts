import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppError, toAppError } from '../../../core/http/app-error';
import { AdminStaffApi } from './admin-staff.api';
import { AdminBranch, CreateStaffCommand, ManagedStaff, StaffSetup } from './admin-staff.models';

@Injectable()
export class AdminStaffStore {
  private readonly api = inject(AdminStaffApi);
  private readonly branchesState = signal<AdminBranch[]>([]);
  private readonly staffState = signal<ManagedStaff[]>([]);
  private readonly selectedBranchCodeState = signal('');
  private readonly loadingState = signal(false);
  private readonly actionStaffIdState = signal<string | null>(null);
  private readonly loadErrorState = signal<AppError | null>(null);
  private readonly actionErrorState = signal<AppError | null>(null);
  private staffRequestSequence = 0;

  readonly branches = this.branchesState.asReadonly();
  readonly staff = this.staffState.asReadonly();
  readonly selectedBranchCode = this.selectedBranchCodeState.asReadonly();
  readonly loading = this.loadingState.asReadonly();
  readonly actionStaffId = this.actionStaffIdState.asReadonly();
  readonly loadError = this.loadErrorState.asReadonly();
  readonly actionError = this.actionErrorState.asReadonly();
  readonly hasBranches = computed(() => this.branchesState().length > 0);
  readonly selectedBranch = computed(
    () =>
      this.branchesState().find((branch) => branch.publicCode === this.selectedBranchCodeState()) ??
      null,
  );

  async load(): Promise<void> {
    this.loadingState.set(true);
    this.loadErrorState.set(null);
    try {
      const branches = await firstValueFrom(this.api.listBranches());
      this.branchesState.set(branches);
      const branchCode = branches[0]?.publicCode ?? '';
      this.selectedBranchCodeState.set(branchCode);
      const staff = branchCode ? await firstValueFrom(this.api.listStaff(branchCode)) : [];
      this.staffState.set(sortStaff(staff));
    } catch (error: unknown) {
      this.loadErrorState.set(toAppError(error));
    } finally {
      this.loadingState.set(false);
    }
  }

  async selectBranch(branchCode: string): Promise<void> {
    if (!branchCode || branchCode === this.selectedBranchCodeState()) {
      return;
    }
    this.selectedBranchCodeState.set(branchCode);
    await this.reloadStaff(branchCode);
  }

  async provision(command: CreateStaffCommand): Promise<StaffSetup | null> {
    this.actionStaffIdState.set('new');
    this.actionErrorState.set(null);
    try {
      const setup = await firstValueFrom(this.api.provision(command));
      if (this.selectedBranchCodeState() === command.branchCode) {
        this.staffState.update((staff) => sortStaff([...staff, setup.staff]));
      }
      return setup;
    } catch (error: unknown) {
      this.actionErrorState.set(toAppError(error));
      return null;
    } finally {
      this.actionStaffIdState.set(null);
    }
  }

  async replaceSetup(staffId: string): Promise<StaffSetup | null> {
    this.actionStaffIdState.set(staffId);
    this.actionErrorState.set(null);
    try {
      return await firstValueFrom(this.api.replaceSetup(staffId));
    } catch (error: unknown) {
      this.actionErrorState.set(toAppError(error));
      return null;
    } finally {
      this.actionStaffIdState.set(null);
    }
  }

  async deactivate(staffId: string): Promise<boolean> {
    this.actionStaffIdState.set(staffId);
    this.actionErrorState.set(null);
    try {
      const updated = await firstValueFrom(this.api.deactivate(staffId));
      this.staffState.update((staff) =>
        staff.map((item) => (item.id === updated.id ? updated : item)),
      );
      return true;
    } catch (error: unknown) {
      this.actionErrorState.set(toAppError(error));
      return false;
    } finally {
      this.actionStaffIdState.set(null);
    }
  }

  clearActionError(): void {
    this.actionErrorState.set(null);
  }

  private async reloadStaff(branchCode: string): Promise<void> {
    const requestSequence = ++this.staffRequestSequence;
    this.loadingState.set(true);
    this.loadErrorState.set(null);
    try {
      const staff = await firstValueFrom(this.api.listStaff(branchCode));
      if (requestSequence === this.staffRequestSequence) {
        this.staffState.set(sortStaff(staff));
      }
    } catch (error: unknown) {
      if (requestSequence === this.staffRequestSequence) {
        this.loadErrorState.set(toAppError(error));
      }
    } finally {
      if (requestSequence === this.staffRequestSequence) {
        this.loadingState.set(false);
      }
    }
  }
}

function sortStaff(staff: ManagedStaff[]): ManagedStaff[] {
  return [...staff].sort((left, right) =>
    left.displayName.localeCompare(right.displayName, undefined, { sensitivity: 'base' }),
  );
}

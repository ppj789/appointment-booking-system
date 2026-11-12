export type AccountStatus = 'PENDING_SETUP' | 'ACTIVE' | 'DEACTIVATED';

export interface AdminBranch {
  publicCode: string;
  name: string;
  displayAddress: string;
  timezone: string;
}

export interface ManagedStaff {
  id: string;
  displayName: string;
  email: string;
  status: AccountStatus;
  branch: AdminBranch;
}

export interface CreateStaffCommand {
  branchCode: string;
  displayName: string;
  email: string;
}

export interface StaffSetup {
  staff: ManagedStaff;
  setupUrl: string;
  expiresAt: string;
}

export function accountStatusLabel(status: AccountStatus): string {
  switch (status) {
    case 'PENDING_SETUP':
      return 'Pending setup';
    case 'ACTIVE':
      return 'Active';
    case 'DEACTIVATED':
      return 'Deactivated';
  }
}

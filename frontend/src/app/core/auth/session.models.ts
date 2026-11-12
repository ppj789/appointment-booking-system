export type AccountType = 'ADMIN' | 'STAFF';

export interface AccountSummary {
  displayName: string;
  accountType: AccountType;
}

export interface BranchSummary {
  publicCode: string;
  name: string;
  displayAddress: string;
  timezone: string;
}

export interface SessionContext {
  account: AccountSummary;
  branch: BranchSummary | null;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export type SessionStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous' | 'error';

export function workflowRoute(accountType: AccountType): '/admin' | '/staff' {
  return accountType === 'ADMIN' ? '/admin' : '/staff';
}

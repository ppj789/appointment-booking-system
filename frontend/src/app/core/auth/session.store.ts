import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppError, toAppError } from '../http/app-error';
import { SessionApi } from './session.api';
import { LoginCredentials, SessionContext, SessionStatus } from './session.models';

@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly api = inject(SessionApi);
  private readonly contextState = signal<SessionContext | null>(null);
  private readonly statusState = signal<SessionStatus>('idle');
  private readonly errorState = signal<AppError | null>(null);
  private readonly busyState = signal(false);
  private restoreTask?: Promise<void>;

  readonly context = this.contextState.asReadonly();
  readonly status = this.statusState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly busy = this.busyState.asReadonly();
  readonly isAuthenticated = computed(() => this.statusState() === 'authenticated');

  async restore(): Promise<void> {
    if (this.statusState() === 'authenticated' || this.statusState() === 'anonymous') {
      return;
    }
    if (this.restoreTask) {
      return this.restoreTask;
    }

    this.restoreTask = this.restoreSession();
    try {
      await this.restoreTask;
    } finally {
      this.restoreTask = undefined;
    }
  }

  async login(credentials: LoginCredentials): Promise<AppError | null> {
    this.busyState.set(true);
    this.errorState.set(null);
    try {
      const context = await firstValueFrom(this.api.login(credentials));
      this.contextState.set(context);
      this.statusState.set('authenticated');
      return null;
    } catch (error: unknown) {
      const appError = toAppError(error);
      this.contextState.set(null);
      this.statusState.set('anonymous');
      this.errorState.set(appError);
      return appError;
    } finally {
      this.busyState.set(false);
    }
  }

  async logout(): Promise<AppError | null> {
    this.busyState.set(true);
    this.errorState.set(null);
    try {
      await firstValueFrom(this.api.logout());
      this.contextState.set(null);
      this.statusState.set('anonymous');
      return null;
    } catch (error: unknown) {
      const appError = toAppError(error);
      this.errorState.set(appError);
      return appError;
    } finally {
      this.busyState.set(false);
    }
  }

  clearError(): void {
    this.errorState.set(null);
  }

  private async restoreSession(): Promise<void> {
    this.statusState.set('loading');
    this.errorState.set(null);
    try {
      const context = await firstValueFrom(this.api.current());
      this.contextState.set(context);
      this.statusState.set('authenticated');
    } catch (error: unknown) {
      this.contextState.set(null);
      if (isUnauthenticated(error)) {
        this.statusState.set('anonymous');
        return;
      }
      this.statusState.set('error');
      this.errorState.set(toAppError(error));
    }
  }
}

function isUnauthenticated(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === 401;
}

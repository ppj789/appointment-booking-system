import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppError, toAppError } from '../../../core/http/app-error';
import { StaffInvitationApi } from './staff-invitation.api';
import {
  InvitationActivation,
  InvitationDraftCommand,
  InvitationSummary,
} from './staff-invitation.models';

@Injectable()
export class StaffInvitationStore {
  private readonly api = inject(StaffInvitationApi);
  private readonly invitationsState = signal<InvitationSummary[]>([]);
  private readonly loadingState = signal(false);
  private readonly actionIdState = signal<string | null>(null);
  private readonly errorState = signal<AppError | null>(null);

  readonly invitations = this.invitationsState.asReadonly();
  readonly loading = this.loadingState.asReadonly();
  readonly actionId = this.actionIdState.asReadonly();
  readonly error = this.errorState.asReadonly();

  async load(): Promise<void> {
    this.loadingState.set(true);
    this.errorState.set(null);
    try {
      this.invitationsState.set(sortInvitations(await firstValueFrom(this.api.list())));
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
    } finally {
      this.loadingState.set(false);
    }
  }

  async save(
    command: InvitationDraftCommand,
    invitationId?: string,
  ): Promise<InvitationSummary | null> {
    this.actionIdState.set(invitationId ?? 'new');
    this.errorState.set(null);
    try {
      const response = await firstValueFrom(
        invitationId ? this.api.update(invitationId, command) : this.api.create(command),
      );
      const saved: InvitationSummary = {
        ...response,
        activatedAt: null,
        consumedAt: null,
        revokedAt: null,
      };
      this.invitationsState.update((invitations) =>
        sortInvitations([...invitations.filter((invitation) => invitation.id !== saved.id), saved]),
      );
      return saved;
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
      return null;
    } finally {
      this.actionIdState.set(null);
    }
  }

  async activate(invitationId: string): Promise<InvitationActivation | null> {
    this.actionIdState.set(invitationId);
    this.errorState.set(null);
    try {
      const activation = await firstValueFrom(this.api.activate(invitationId));
      await this.load();
      return activation;
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
      return null;
    } finally {
      this.actionIdState.set(null);
    }
  }

  async revoke(invitationId: string): Promise<boolean> {
    this.actionIdState.set(invitationId);
    this.errorState.set(null);
    try {
      await firstValueFrom(this.api.revoke(invitationId));
      await this.load();
      return true;
    } catch (error: unknown) {
      this.errorState.set(toAppError(error));
      return false;
    } finally {
      this.actionIdState.set(null);
    }
  }

  clearError(): void {
    this.errorState.set(null);
  }
}

function sortInvitations(invitations: InvitationSummary[]): InvitationSummary[] {
  return [...invitations].sort(
    (left, right) => new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime(),
  );
}

import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionStore } from '../../../../core/auth/session.store';
import { AppError, toAppError } from '../../../../core/http/app-error';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';
import { AccountSetupApi } from '../../data-access/account-setup.api';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ErrorState,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
  ],
  selector: 'app-account-setup-page',
  styleUrl: './account-setup-page.scss',
  templateUrl: './account-setup-page.html',
})
export class AccountSetupPage implements OnInit {
  private readonly api = inject(AccountSetupApi);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  protected readonly session = inject(SessionStore);
  protected setupToken: string | null = null;

  protected readonly submitting = signal(false);
  protected readonly completed = signal(false);
  protected readonly mismatch = signal(false);
  protected readonly error = signal<AppError | null>(null);
  protected readonly continueError = signal<AppError | null>(null);
  protected readonly form = new FormGroup({
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(12), Validators.maxLength(128)],
    }),
    confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  ngOnInit(): void {
    const fragment = this.route.snapshot.fragment ?? '';
    this.setupToken = new URLSearchParams(fragment).get('token');
    this.location.replaceState('/account-setup');
    if (!this.setupToken) {
      this.error.set({
        message: 'This setup link is unavailable. Ask an administrator for a replacement link.',
      });
    }
  }

  protected async submit(): Promise<void> {
    if (this.form.invalid || !this.setupToken || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const values = this.form.getRawValue();
    if (values.password !== values.confirmPassword) {
      this.mismatch.set(true);
      return;
    }

    this.mismatch.set(false);
    this.error.set(null);
    this.submitting.set(true);
    try {
      await firstValueFrom(this.api.complete(this.setupToken, values.password));
      this.setupToken = null;
      this.completed.set(true);
      this.form.reset();
      await this.session.restore();
    } catch (error: unknown) {
      this.error.set(toAppError(error));
    } finally {
      this.submitting.set(false);
    }
  }

  protected async continueToSignIn(): Promise<void> {
    this.continueError.set(null);
    if (this.session.isAuthenticated()) {
      const error = await this.session.logout();
      if (error) {
        this.continueError.set(error);
        return;
      }
    }
    await this.router.navigateByUrl('/login');
  }
}

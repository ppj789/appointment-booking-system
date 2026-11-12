import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { SessionStore } from '../../../../core/auth/session.store';
import { workflowRoute } from '../../../../core/auth/session.models';
import { ErrorState } from '../../../../shared/ui/error-state/error-state';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
    ErrorState,
  ],
  selector: 'app-login-page',
  styleUrl: './login-page.scss',
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly router = inject(Router);
  protected readonly session = inject(SessionStore);
  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid || this.session.busy()) {
      this.form.markAllAsTouched();
      return;
    }

    const error = await this.session.login(this.form.getRawValue());
    const context = this.session.context();
    if (!error && context) {
      await this.router.navigateByUrl(workflowRoute(context.account.accountType));
    }
  }
}

import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessionStore } from '../../core/auth/session.store';
import { AppError } from '../../core/http/app-error';
import { ErrorState } from '../../shared/ui/error-state/error-state';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ErrorState,
    MatButtonModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  selector: 'app-shell',
  styleUrl: './app-shell.scss',
  templateUrl: './app-shell.html',
})
export class AppShell {
  private readonly router = inject(Router);
  protected readonly session = inject(SessionStore);
  protected readonly logoutError = signal<AppError | null>(null);

  protected async logout(): Promise<void> {
    this.logoutError.set(null);
    const error = await this.session.logout();
    if (error) {
      this.logoutError.set(error);
      return;
    }
    await this.router.navigateByUrl('/login');
  }
}

import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule],
  selector: 'app-error-state',
  template: `
    <section class="state surface-card" role="alert">
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      @if (correlationId()) {
        <p class="reference">Reference: {{ correlationId() }}</p>
      }
      @if (retryable()) {
        <button mat-stroked-button type="button" (click)="retry.emit()">Try again</button>
      }
    </section>
  `,
  styles: `
    .state {
      display: grid;
      justify-items: start;
      gap: var(--app-space-sm);
      border-color: var(--app-color-error);
    }

    h2,
    p {
      margin: 0;
    }

    .reference {
      color: var(--app-color-muted);
      font-size: 0.8rem;
      overflow-wrap: anywhere;
    }
  `,
})
export class ErrorState {
  readonly title = input('Unable to load this page');
  readonly message = input.required<string>();
  readonly correlationId = input<string>();
  readonly retryable = input(false);
  readonly retry = output<void>();
}

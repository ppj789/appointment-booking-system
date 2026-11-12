import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatProgressSpinnerModule],
  selector: 'app-loading-state',
  template: `
    <div class="state" role="status" aria-live="polite">
      <mat-spinner diameter="32" aria-hidden="true" />
      <span>{{ label() }}</span>
    </div>
  `,
  styles: `
    .state {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: var(--app-space-md);
      min-height: 12rem;
      color: var(--app-color-muted);
    }
  `,
})
export class LoadingState {
  readonly label = input('Loading…');
}

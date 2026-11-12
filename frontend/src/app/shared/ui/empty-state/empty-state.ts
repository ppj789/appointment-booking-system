import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-empty-state',
  template: `
    <section class="state surface-card">
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
    </section>
  `,
  styles: `
    .state {
      display: grid;
      justify-items: start;
      gap: var(--app-space-sm);
    }

    h2,
    p {
      margin: 0;
    }

    p {
      color: var(--app-color-muted);
    }
  `,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
}

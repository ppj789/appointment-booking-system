import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { SessionStore } from '../../../../core/auth/session.store';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  selector: 'app-staff-home-page',
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Staff workspace</h1>
        @if (session.context()?.branch; as branch) {
          <p>{{ branch.name }} · {{ branch.displayAddress }} · {{ branch.timezone }}</p>
        }
      </header>

      <app-empty-state
        title="Calendar workspace"
        message="Your calendar and Invitation tools will appear in this workspace."
      />
    </section>
  `,
})
export class StaffHomePage {
  protected readonly session = inject(SessionStore);
}

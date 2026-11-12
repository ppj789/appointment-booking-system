import { ChangeDetectionStrategy, Component } from '@angular/core';
import { EmptyState } from '../../../../shared/ui/empty-state/empty-state';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EmptyState],
  selector: 'app-admin-home-page',
  template: `
    <section class="page">
      <header class="page-header">
        <h1>Admin workspace</h1>
        <p>Manage Staff access across the configured Branches.</p>
      </header>

      <app-empty-state
        title="Staff management"
        message="Branch and Staff management tools will appear in this workspace."
      />
    </section>
  `,
})
export class AdminHomePage {}

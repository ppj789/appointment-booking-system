import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { SessionStore } from '../../../../core/auth/session.store';
import { StaffCalendarPanel } from '../../components/staff-calendar-panel/staff-calendar-panel';
import { StaffInvitationsPanel } from '../../components/staff-invitations-panel/staff-invitations-panel';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatTabsModule, StaffCalendarPanel, StaffInvitationsPanel],
  selector: 'app-staff-home-page',
  styleUrl: './staff-home-page.scss',
  templateUrl: './staff-home-page.html',
})
export class StaffHomePage {
  protected readonly session = inject(SessionStore);
}

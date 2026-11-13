import { Routes } from '@angular/router';
import {
  adminGuard,
  authenticatedGuard,
  loginGuard,
  staffGuard,
  workflowHomeGuard,
} from './core/auth/session.guards';

export const routes: Routes = [
  {
    path: 'book',
    title: 'Book appointment | Appointment Booking',
    loadComponent: () =>
      import('./features/guest/pages/guest-booking-page/guest-booking-page').then(
        ({ GuestBookingPage }) => GuestBookingPage,
      ),
  },
  {
    path: 'account-setup',
    title: 'Set up account | Appointment Booking',
    loadComponent: () =>
      import('./features/auth/pages/account-setup-page/account-setup-page').then(
        ({ AccountSetupPage }) => AccountSetupPage,
      ),
  },
  {
    path: 'login',
    title: 'Sign in | Appointment Booking',
    canActivate: [loginGuard],
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page').then(({ LoginPage }) => LoginPage),
  },
  {
    path: '',
    canActivate: [authenticatedGuard],
    loadComponent: () => import('./layout/app-shell/app-shell').then(({ AppShell }) => AppShell),
    children: [
      {
        path: '',
        pathMatch: 'full',
        canActivate: [workflowHomeGuard],
        loadComponent: () =>
          import('./shared/ui/loading-state/loading-state').then(
            ({ LoadingState }) => LoadingState,
          ),
      },
      {
        path: 'admin',
        title: 'Staff management | Appointment Booking',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/pages/staff-management-page/staff-management-page').then(
            ({ StaffManagementPage }) => StaffManagementPage,
          ),
      },
      {
        path: 'staff',
        title: 'Staff workspace | Appointment Booking',
        canActivate: [staffGuard],
        loadComponent: () =>
          import('./features/staff/pages/staff-home-page/staff-home-page').then(
            ({ StaffHomePage }) => StaffHomePage,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

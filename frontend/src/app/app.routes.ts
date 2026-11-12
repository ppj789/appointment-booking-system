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
        title: 'Admin workspace | Appointment Booking',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/pages/admin-home-page/admin-home-page').then(
            ({ AdminHomePage }) => AdminHomePage,
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

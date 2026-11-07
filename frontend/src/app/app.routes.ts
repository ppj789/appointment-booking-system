import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/app-shell/app-shell').then(({ AppShell }) => AppShell),
    children: [
      {
        path: '',
        title: 'Appointment Booking System',
        loadComponent: () =>
          import('./features/foundation/pages/foundation-page/foundation-page').then(
            ({ FoundationPage }) => FoundationPage,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

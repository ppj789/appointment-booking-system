import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionStore } from './session.store';
import { AccountType, workflowRoute } from './session.models';

export const authenticatedGuard: CanActivateFn = async (_route, state) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.restore();

  if (session.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: state.url === '/' ? undefined : { returnUrl: state.url },
  });
};

export const loginGuard: CanActivateFn = async () => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.restore();
  const context = session.context();
  return context ? router.parseUrl(workflowRoute(context.account.accountType)) : true;
};

export const workflowHomeGuard: CanActivateFn = async () => {
  const session = inject(SessionStore);
  const router = inject(Router);
  await session.restore();
  const context = session.context();
  return context
    ? router.parseUrl(workflowRoute(context.account.accountType))
    : router.parseUrl('/login');
};

export const adminGuard = accountTypeGuard('ADMIN');
export const staffGuard = accountTypeGuard('STAFF');

function accountTypeGuard(requiredType: AccountType): CanActivateFn {
  return async () => {
    const session = inject(SessionStore);
    const router = inject(Router);
    await session.restore();
    const context = session.context();
    if (!context) {
      return router.parseUrl('/login');
    }
    return context.account.accountType === requiredType
      ? true
      : router.parseUrl(workflowRoute(context.account.accountType));
  };
}

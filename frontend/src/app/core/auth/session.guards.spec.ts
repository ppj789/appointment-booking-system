import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { authenticatedGuard, staffGuard } from './session.guards';
import { SessionContext } from './session.models';
import { SessionStore } from './session.store';

describe('session guards', () => {
  let session: {
    restore: ReturnType<typeof vi.fn<() => Promise<void>>>;
    isAuthenticated: ReturnType<typeof vi.fn<() => boolean>>;
    context: ReturnType<typeof vi.fn<() => SessionContext | null>>;
  };
  let router: Router;

  const adminContext: SessionContext = {
    account: { displayName: 'Local Admin', accountType: 'ADMIN' },
    branch: null,
  };

  beforeEach(() => {
    session = {
      restore: vi.fn(async () => undefined),
      isAuthenticated: vi.fn(() => false),
      context: vi.fn(() => null),
    };
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: SessionStore, useValue: session }],
    });
    router = TestBed.inject(Router);
  });

  it('redirects an anonymous request to sign in', async () => {
    const result = await runGuard(authenticatedGuard, '/admin');

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?returnUrl=%2Fadmin');
  });

  it('keeps an Admin out of the Staff workflow without exposing a forbidden page', async () => {
    session.isAuthenticated.mockReturnValue(true);
    session.context.mockReturnValue(adminContext);

    const result = await runGuard(staffGuard, '/staff');

    expect(result).toBeInstanceOf(UrlTree);
    expect(router.serializeUrl(result as UrlTree)).toBe('/admin');
  });
});

function runGuard(guard: typeof authenticatedGuard, url: string): Promise<boolean | UrlTree> {
  return TestBed.runInInjectionContext(
    () =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as Promise<
        boolean | UrlTree
      >,
  );
}

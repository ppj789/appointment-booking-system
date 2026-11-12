import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { SessionApi } from './session.api';
import { SessionContext } from './session.models';
import { SessionStore } from './session.store';

describe('SessionStore', () => {
  const adminContext: SessionContext = {
    account: { displayName: 'Local Admin', accountType: 'ADMIN' },
    branch: null,
  };

  let api: {
    login: ReturnType<typeof vi.fn<(credentials: unknown) => Observable<SessionContext>>>;
    current: ReturnType<typeof vi.fn<() => Observable<SessionContext>>>;
    logout: ReturnType<typeof vi.fn<() => Observable<void>>>;
  };
  let store: SessionStore;

  beforeEach(() => {
    api = {
      login: vi.fn(() => of(adminContext)),
      current: vi.fn(() => of(adminContext)),
      logout: vi.fn(() => of(undefined)),
    };
    TestBed.configureTestingModule({
      providers: [SessionStore, { provide: SessionApi, useValue: api }],
    });
    store = TestBed.inject(SessionStore);
  });

  it('restores an authenticated session once', async () => {
    await store.restore();
    await store.restore();

    expect(store.status()).toBe('authenticated');
    expect(store.context()).toEqual(adminContext);
    expect(api.current).toHaveBeenCalledTimes(1);
  });

  it('treats a missing session as anonymous rather than an application error', async () => {
    api.current.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );

    await store.restore();

    expect(store.status()).toBe('anonymous');
    expect(store.context()).toBeNull();
    expect(store.error()).toBeNull();
  });

  it('maps a failed login without retaining credentials or a session context', async () => {
    api.login.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: {
              detail: 'Authentication failed.',
              code: 'INVALID_CREDENTIALS',
              correlationId: '2bf16fb0-a4ac-4ae8-b71b-860d2846a51b',
            },
            headers: new HttpHeaders(),
          }),
      ),
    );

    const error = await store.login({ email: 'admin@example.test', password: 'wrong' });

    expect(store.status()).toBe('anonymous');
    expect(store.context()).toBeNull();
    expect(error).toEqual({
      message: 'The email or password is incorrect.',
      code: 'INVALID_CREDENTIALS',
      correlationId: '2bf16fb0-a4ac-4ae8-b71b-860d2846a51b',
    });
  });

  it('clears the local context after successful logout', async () => {
    await store.restore();
    const error = await store.logout();

    expect(error).toBeNull();
    expect(store.status()).toBe('anonymous');
    expect(store.context()).toBeNull();
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SessionApi } from './session.api';
import { SessionContext } from './session.models';

describe('SessionApi', () => {
  let api: SessionApi;
  let http: HttpTestingController;

  const adminContext: SessionContext = {
    account: { displayName: 'Local Admin', accountType: 'ADMIN' },
    branch: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SessionApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(SessionApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts credentials and returns the safe session context', () => {
    const credentials = { email: 'admin@example.test', password: 'admin' };
    let response: SessionContext | undefined;

    api.login(credentials).subscribe((context) => (response = context));

    const request = http.expectOne('/api/v1/sessions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(credentials);
    request.flush(adminContext);
    expect(response).toEqual(adminContext);
  });

  it('restores and ends the same-origin session', () => {
    api.current().subscribe();
    const current = http.expectOne('/api/v1/sessions/current');
    expect(current.request.method).toBe('GET');
    current.flush(adminContext);

    api.logout().subscribe();
    const logout = http.expectOne('/api/v1/sessions/current');
    expect(logout.request.method).toBe('DELETE');
    logout.flush(null);
  });
});

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AccountSetupApi } from './account-setup.api';

describe('AccountSetupApi', () => {
  it('keeps the setup capability in its dedicated header', () => {
    TestBed.configureTestingModule({
      providers: [AccountSetupApi, provideHttpClient(), provideHttpClientTesting()],
    });
    const api = TestBed.inject(AccountSetupApi);
    const http = TestBed.inject(HttpTestingController);

    api.complete('one-time-token', 'valid-password').subscribe();

    const request = http.expectOne('/api/v1/account-setup');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-Setup-Token')).toBe('one-time-token');
    expect(request.request.body).toEqual({ password: 'valid-password' });
    request.flush(null);
    http.verify();
  });
});

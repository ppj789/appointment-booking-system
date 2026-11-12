import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { correlationIdInterceptor } from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('adds a canonical UUID correlation ID to an outgoing request', () => {
    client.get('/api/v1/example').subscribe();

    const request = http.expectOne('/api/v1/example');
    expect(request.request.headers.get('X-Correlation-ID')).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
    );
    request.flush({});
  });

  it('preserves a supplied correlation ID', () => {
    const correlationId = '84add27c-f325-4dfc-b729-6540d44136b5';
    client.get('/api/v1/example', { headers: { 'X-Correlation-ID': correlationId } }).subscribe();

    const request = http.expectOne('/api/v1/example');
    expect(request.request.headers.get('X-Correlation-ID')).toBe(correlationId);
    request.flush({});
  });
});

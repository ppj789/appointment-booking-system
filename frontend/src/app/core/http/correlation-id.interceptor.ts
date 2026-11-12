import { HttpInterceptorFn } from '@angular/common/http';

export const correlationIdInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.headers.has('X-Correlation-ID')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { 'X-Correlation-ID': globalThis.crypto.randomUUID() },
    }),
  );
};

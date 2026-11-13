import { HttpErrorResponse } from '@angular/common/http';

export interface AppError {
  message: string;
  code?: string;
  correlationId?: string;
  activeInvitation?: boolean;
  futureAppointment?: boolean;
}

interface ProblemDetails {
  detail?: unknown;
  code?: unknown;
  correlationId?: unknown;
  activeInvitation?: unknown;
  futureAppointment?: unknown;
}

const ERROR_MESSAGES: Readonly<Record<string, string>> = {
  INVALID_CREDENTIALS: 'The email or password is incorrect.',
  LOGIN_RATE_LIMITED: 'Too many sign-in attempts. Please wait and try again.',
  AUTHENTICATION_REQUIRED: 'Your session has ended. Please sign in again.',
  ACCESS_DENIED: 'You do not have access to that action.',
  CALENDAR_CONFLICT: 'That time overlaps an existing appointment or unavailable block.',
  INVITATION_UNAVAILABLE: 'This booking link is unavailable. Ask the Staff member for a new link.',
  SLOT_UNAVAILABLE: 'That time was just taken. Choose another available time.',
  SLOT_OUTSIDE_HORIZON: 'Choose a time within the next 14 days.',
  UNSUPPORTED_DURATION: 'Choose one of the available appointment durations.',
  INVALID_SLOT: 'Choose one of the available appointment times.',
  IDEMPOTENCY_KEY_REUSED: 'The booking could not be retried safely. Choose the time again.',
};

export function toAppError(error: unknown): AppError {
  if (!(error instanceof HttpErrorResponse)) {
    return { message: 'Something went wrong. Please try again.' };
  }

  const problem = isProblemDetails(error.error) ? error.error : undefined;
  const code = typeof problem?.code === 'string' ? problem.code : undefined;
  const correlationId =
    typeof problem?.correlationId === 'string'
      ? problem.correlationId
      : (error.headers.get('X-Correlation-ID') ?? undefined);

  if (error.status === 0) {
    return {
      message: 'The service could not be reached. Check the connection and try again.',
      correlationId,
    };
  }

  const safeDetail = typeof problem?.detail === 'string' ? problem.detail : undefined;
  const appError: AppError = {
    message:
      (code ? ERROR_MESSAGES[code] : undefined) ??
      safeDetail ??
      'The request could not be completed. Please try again.',
    code,
    correlationId,
  };
  if (typeof problem?.activeInvitation === 'boolean') {
    appError.activeInvitation = problem.activeInvitation;
  }
  if (typeof problem?.futureAppointment === 'boolean') {
    appError.futureAppointment = problem.futureAppointment;
  }
  return appError;
}

function isProblemDetails(value: unknown): value is ProblemDetails {
  return typeof value === 'object' && value !== null;
}

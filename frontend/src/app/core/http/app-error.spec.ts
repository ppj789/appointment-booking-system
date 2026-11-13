import { HttpErrorResponse } from '@angular/common/http';
import { toAppError } from './app-error';

describe('application error mapping', () => {
  it('explains a calendar conflict without exposing database details', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: {
        code: 'CALENDAR_CONFLICT',
        detail: 'The requested interval overlaps existing calendar occupancy.',
        correlationId: 'be78557f-142a-4a0e-9004-1cc82c70d314',
      },
    });

    expect(toAppError(error)).toEqual({
      message: 'That time overlaps an existing appointment or unavailable block.',
      code: 'CALENDAR_CONFLICT',
      correlationId: 'be78557f-142a-4a0e-9004-1cc82c70d314',
    });
  });
});

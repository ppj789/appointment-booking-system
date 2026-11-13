import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AvailabilityBlock,
  CreateAvailabilityBlockCommand,
  StaffCalendar,
} from './staff-calendar.models';

@Injectable({ providedIn: 'root' })
export class StaffCalendarApi {
  private readonly http = inject(HttpClient);
  private readonly calendarUrl = '/api/v1/me/calendar';
  private readonly blocksUrl = '/api/v1/me/availability-blocks';

  getCalendar(from: string, to: string): Observable<StaffCalendar> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<StaffCalendar>(this.calendarUrl, { params });
  }

  createBlock(command: CreateAvailabilityBlockCommand): Observable<AvailabilityBlock> {
    return this.http.post<AvailabilityBlock>(this.blocksUrl, command);
  }

  removeBlock(blockId: string): Observable<void> {
    return this.http.delete<void>(`${this.blocksUrl}/${blockId}`);
  }
}

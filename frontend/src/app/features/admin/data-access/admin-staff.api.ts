import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminBranch, CreateStaffCommand, ManagedStaff, StaffSetup } from './admin-staff.models';

@Injectable({ providedIn: 'root' })
export class AdminStaffApi {
  private readonly http = inject(HttpClient);
  private readonly staffUrl = '/api/v1/admin/staff';

  listBranches(): Observable<AdminBranch[]> {
    return this.http.get<AdminBranch[]>('/api/v1/admin/branches');
  }

  listStaff(branchCode?: string): Observable<ManagedStaff[]> {
    const params = branchCode ? new HttpParams().set('branchCode', branchCode) : undefined;
    return this.http.get<ManagedStaff[]>(this.staffUrl, { params });
  }

  provision(command: CreateStaffCommand): Observable<StaffSetup> {
    return this.http.post<StaffSetup>(this.staffUrl, command);
  }

  replaceSetup(staffId: string): Observable<StaffSetup> {
    return this.http.post<StaffSetup>(`${this.staffUrl}/${staffId}/setup-replacement`, null);
  }

  deactivate(staffId: string): Observable<ManagedStaff> {
    return this.http.post<ManagedStaff>(`${this.staffUrl}/${staffId}/deactivate`, null);
  }
}

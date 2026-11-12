import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminStaffApi } from './admin-staff.api';
import { AdminBranch, ManagedStaff, StaffSetup } from './admin-staff.models';

describe('AdminStaffApi', () => {
  let api: AdminStaffApi;
  let http: HttpTestingController;

  const branch: AdminBranch = {
    publicCode: 'CPT',
    name: 'Cape Town Demo Branch',
    displayAddress: '1 Demo Street',
    timezone: 'Africa/Johannesburg',
  };
  const staff: ManagedStaff = {
    id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    displayName: 'Test Staff',
    email: 'staff@example.test',
    status: 'PENDING_SETUP',
    branch,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AdminStaffApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AdminStaffApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists Branches and optionally filters Staff by public Branch code', () => {
    api.listBranches().subscribe();
    const branchesRequest = http.expectOne('/api/v1/admin/branches');
    expect(branchesRequest.request.method).toBe('GET');
    branchesRequest.flush([branch]);

    api.listStaff('CPT').subscribe();
    const staffRequest = http.expectOne(
      (request) =>
        request.url === '/api/v1/admin/staff' && request.params.get('branchCode') === 'CPT',
    );
    expect(staffRequest.request.method).toBe('GET');
    staffRequest.flush([staff]);
  });

  it('provisions Staff using only the authored account fields', () => {
    const command = {
      branchCode: 'CPT',
      displayName: 'Test Staff',
      email: 'staff@example.test',
    };
    const response: StaffSetup = {
      staff,
      setupUrl: '/account-setup#token=secret',
      expiresAt: '2026-11-13T10:00:00Z',
    };

    api.provision(command).subscribe();
    const request = http.expectOne('/api/v1/admin/staff');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(response);
  });

  it('uses the dedicated setup replacement and deactivation commands', () => {
    api.replaceSetup(staff.id).subscribe();
    const replacement = http.expectOne(`/api/v1/admin/staff/${staff.id}/setup-replacement`);
    expect(replacement.request.method).toBe('POST');
    replacement.flush({ staff, setupUrl: '/account-setup#token=new', expiresAt: '' });

    api.deactivate(staff.id).subscribe();
    const deactivation = http.expectOne(`/api/v1/admin/staff/${staff.id}/deactivate`);
    expect(deactivation.request.method).toBe('POST');
    deactivation.flush({ ...staff, status: 'DEACTIVATED' });
  });
});

import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { AdminStaffApi } from './admin-staff.api';
import { AdminBranch, ManagedStaff, StaffSetup } from './admin-staff.models';
import { AdminStaffStore } from './admin-staff.store';

describe('AdminStaffStore', () => {
  const branch: AdminBranch = {
    publicCode: 'CPT',
    name: 'Cape Town Demo Branch',
    displayAddress: '1 Demo Street',
    timezone: 'Africa/Johannesburg',
  };
  const zuluStaff: ManagedStaff = {
    id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    displayName: 'Zulu Staff',
    email: 'zulu@example.test',
    status: 'ACTIVE',
    branch,
  };
  const alphaStaff: ManagedStaff = {
    ...zuluStaff,
    id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
    displayName: 'Alpha Staff',
    email: 'alpha@example.test',
    status: 'PENDING_SETUP',
  };
  const setup: StaffSetup = {
    staff: alphaStaff,
    setupUrl: '/account-setup#token=secret',
    expiresAt: '2026-11-13T10:00:00Z',
  };

  let api: {
    listBranches: ReturnType<typeof vi.fn<() => Observable<AdminBranch[]>>>;
    listStaff: ReturnType<typeof vi.fn<(branchCode?: string) => Observable<ManagedStaff[]>>>;
    provision: ReturnType<typeof vi.fn<() => Observable<StaffSetup>>>;
    replaceSetup: ReturnType<typeof vi.fn<() => Observable<StaffSetup>>>;
    deactivate: ReturnType<typeof vi.fn<() => Observable<ManagedStaff>>>;
  };
  let store: AdminStaffStore;

  beforeEach(() => {
    api = {
      listBranches: vi.fn(() => of([branch])),
      listStaff: vi.fn(() => of([zuluStaff, alphaStaff])),
      provision: vi.fn(() => of(setup)),
      replaceSetup: vi.fn(() => of(setup)),
      deactivate: vi.fn(() => of({ ...zuluStaff, status: 'DEACTIVATED' })),
    };
    TestBed.configureTestingModule({
      providers: [AdminStaffStore, { provide: AdminStaffApi, useValue: api }],
    });
    store = TestBed.inject(AdminStaffStore);
  });

  it('loads Branches and sorts Staff for stable presentation', async () => {
    await store.load();

    expect(store.branches()).toEqual([branch]);
    expect(store.staff().map((staff) => staff.displayName)).toEqual(['Alpha Staff', 'Zulu Staff']);
  });

  it('reloads Staff using the selected Branch filter', async () => {
    await store.selectBranch('CPT');

    expect(store.selectedBranchCode()).toBe('CPT');
    expect(api.listStaff).toHaveBeenCalledWith('CPT');
  });

  it('adds newly provisioned Staff to the matching visible list', async () => {
    api.listStaff.mockReturnValueOnce(of([]));
    await store.load();

    const result = await store.provision({
      branchCode: 'CPT',
      displayName: 'Alpha Staff',
      email: 'alpha@example.test',
    });

    expect(result).toEqual(setup);
    expect(store.staff()).toEqual([alphaStaff]);
  });

  it('preserves safe deactivation blocker categories for the UI', async () => {
    api.deactivate.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              detail: 'Resolve the active customer work before deactivating this Staff account.',
              code: 'STAFF_DEACTIVATION_BLOCKED',
              activeInvitation: true,
              futureAppointment: false,
            },
          }),
      ),
    );

    expect(await store.deactivate(zuluStaff.id)).toBe(false);
    expect(store.actionError()).toMatchObject({
      code: 'STAFF_DEACTIVATION_BLOCKED',
      activeInvitation: true,
      futureAppointment: false,
    });
  });

  it('updates the visible account after successful deactivation', async () => {
    await store.load();
    expect(await store.deactivate(zuluStaff.id)).toBe(true);

    expect(store.staff().find((staff) => staff.id === zuluStaff.id)?.status).toBe('DEACTIVATED');
  });
});

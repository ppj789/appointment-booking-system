import { Location } from '@angular/common';
import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { SessionContext } from '../../../../core/auth/session.models';
import { SessionStore } from '../../../../core/auth/session.store';
import { AccountSetupApi } from '../../data-access/account-setup.api';
import { AccountSetupPage } from './account-setup-page';

describe('AccountSetupPage', () => {
  let api: { complete: ReturnType<typeof vi.fn<() => Observable<void>>> };
  let location: { replaceState: ReturnType<typeof vi.fn> };
  let session: {
    context: WritableSignal<SessionContext | null>;
    isAuthenticated: WritableSignal<boolean>;
    busy: WritableSignal<boolean>;
    restore: ReturnType<typeof vi.fn<() => Promise<void>>>;
    logout: ReturnType<typeof vi.fn<() => Promise<null>>>;
  };

  beforeEach(async () => {
    api = { complete: vi.fn(() => of(undefined)) };
    location = { replaceState: vi.fn() };
    session = {
      context: signal<SessionContext | null>(null),
      isAuthenticated: signal(false),
      busy: signal(false),
      restore: vi.fn(() => Promise.resolve()),
      logout: vi.fn(() => Promise.resolve(null)),
    };
    await TestBed.configureTestingModule({
      imports: [AccountSetupPage],
      providers: [
        provideRouter([]),
        { provide: AccountSetupApi, useValue: api },
        { provide: SessionStore, useValue: session },
        { provide: Location, useValue: location },
        { provide: ActivatedRoute, useValue: { snapshot: { fragment: 'token=one-time-token' } } },
      ],
    }).compileComponents();
  });

  it('removes the raw capability from browser history and completes setup', async () => {
    const fixture = TestBed.createComponent(AccountSetupPage);
    fixture.detectChanges();
    expect(location.replaceState).toHaveBeenCalledWith('/account-setup');

    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    setInput(inputs[0], 'valid-password');
    setInput(inputs[1], 'valid-password');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.complete).toHaveBeenCalledWith('one-time-token', 'valid-password');
    expect(session.restore).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Account ready');
  });

  it('requires an existing account to sign out before Staff sign-in', async () => {
    session.context.set({
      account: { displayName: 'Local Admin', accountType: 'ADMIN' },
      branch: null,
    });
    session.isAuthenticated.set(true);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AccountSetupPage);
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    setInput(inputs[0], 'valid-password');
    setInput(inputs[1], 'valid-password');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'This browser is still signed in as Local Admin',
    );
    const continueButton = fixture.nativeElement.querySelector(
      'mat-card-actions button',
    ) as HTMLButtonElement;
    continueButton.click();
    await fixture.whenStable();

    expect(session.logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});

function setInput(input: HTMLInputElement, value: string): void {
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

import { TestBed } from '@angular/core/testing';
import { FoundationPage } from './foundation-page';

describe('FoundationPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FoundationPage],
    }).compileComponents();
  });

  it('renders the foundation heading and three layer cards', () => {
    const fixture = TestBed.createComponent(FoundationPage);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('reliable base');
    expect(element.querySelectorAll('app-foundation-layer-card')).toHaveLength(3);
  });
});

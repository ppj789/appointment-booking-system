import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { APPLICATION_LINKS } from '../../../../core/config/application-links';
import { FoundationLayerCard } from '../../components/foundation-layer-card/foundation-layer-card';
import { FoundationLayer } from '../../models/foundation-layer';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FoundationLayerCard, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule],
  selector: 'app-foundation-page',
  styleUrl: './foundation-page.scss',
  templateUrl: './foundation-page.html',
})
export class FoundationPage {
  protected readonly layers: readonly FoundationLayer[] = [
    {
      icon: 'calendar_month',
      name: 'Angular + Material',
      description:
        'An accessible, responsive client ready for the Admin, Staff and Guest journeys.',
      detail: 'Frontend',
    },
    {
      icon: 'api',
      name: 'Spring Boot API',
      description: 'A secure modular backend with health, OpenAPI and feature-owned packages.',
      detail: 'Backend',
    },
    {
      icon: 'database',
      name: 'PostgreSQL + Flyway',
      description: 'Durable relational state with one backend-owned migration history.',
      detail: 'Data',
    },
  ];

  protected readonly links = APPLICATION_LINKS;
}

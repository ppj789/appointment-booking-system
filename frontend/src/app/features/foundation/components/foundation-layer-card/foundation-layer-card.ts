import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { FoundationLayer } from '../../models/foundation-layer';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatIconModule],
  selector: 'app-foundation-layer-card',
  styleUrl: './foundation-layer-card.scss',
  templateUrl: './foundation-layer-card.html',
})
export class FoundationLayerCard {
  readonly layer = input.required<FoundationLayer>();
}

import { Component, input } from '@angular/core';
import { DEFAULT_LABEL_COLOR } from '../../models/label-colors';

@Component({
  selector: 'app-label-chip',
  template: `<span class="chip" [style.background-color]="color() ?? defaultColor">{{ name() }}</span>`,
  styles: `
    .chip {
      display: inline-block;
      padding: 2px 12px;
      border-radius: 16px;
      color: #fff;
      font-size: 0.85rem;
      line-height: 24px;
      white-space: nowrap;
    }
  `,
})
export class LabelChip {
  readonly name = input.required<string>();
  readonly color = input<string | null>(null);
  protected readonly defaultColor = DEFAULT_LABEL_COLOR;
}

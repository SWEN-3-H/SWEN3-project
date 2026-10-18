import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LabelChip } from './label-chip';

describe('LabelChip', () => {
  let component: LabelChip;
  let fixture: ComponentFixture<LabelChip>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LabelChip],
    }).compileComponents();

    fixture = TestBed.createComponent(LabelChip);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

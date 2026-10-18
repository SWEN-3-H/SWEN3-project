export interface LabelColor {
  name: string;
  value: string;
}

export const LABEL_COLORS: LabelColor[] = [
  { name: 'Indigo',    value: '#3f51b5' },
  { name: 'Teal',      value: '#00796b' },
  { name: 'Green',     value: '#2e7d32' },
  { name: 'Orange',    value: '#e65100' },
  { name: 'Red',       value: '#c62828' },
  { name: 'Pink',      value: '#ad1457' },
  { name: 'Purple',    value: '#6a1b9a' },
  { name: 'Blue grey', value: '#455a64' },
];

export const DEFAULT_LABEL_COLOR = '#455a64';
